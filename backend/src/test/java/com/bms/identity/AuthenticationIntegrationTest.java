package com.bms.identity;

import java.util.List;
import java.util.Map;

import com.bms.support.AbstractIntegrationTest;
import com.bms.user.AppUser;
import com.bms.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The two ways into this API, and the fact that they arrive at the same place.
 *
 * <p>A browser signs in through this service and is given a session it cannot
 * read; anything else, a mobile application above all, presents its own access
 * token. Neither is privileged over the other, and a caller's identity, roles
 * and local record come out the same either way. The rest of the suite covers
 * the token half, since that is what {@code Jwts} builds.
 */
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    private static final String KEYCLOAK_ID = "kc-session-owner";
    private static final String EMAIL = "session-owner@example.com";
    private static final String AUTHORIZE = "http://localhost:8081/realms/bms/protocol/openid-connect/auth";

    @Autowired
    private AppUserRepository users;

    /** A browser holding a session this service established, as Keycloak describes it. */
    private static RequestPostProcessor asBrowser(String keycloakId, String email, String realmRole) {
        return oidcLogin()
                .idToken(token -> token
                        .subject(keycloakId)
                        .claim("email", email)
                        .claim("given_name", "Sam")
                        .claim("family_name", "Session")
                        .claim("realm_access", Map.of("roles", List.of(realmRole))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + realmRole.toUpperCase()));
    }

    @Test
    void anUnauthenticatedApiCallIsRefusedRatherThanRedirected() throws Exception {
        // A redirect to another host is unreadable to a background request: the
        // browser follows it and hands back an opaque failure, so the
        // application could not tell "signed out" from "broken".
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signingInSendsTheBrowserToKeycloak() throws Exception {
        mockMvc.perform(get("/api/auth/login/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith(AUTHORIZE)));
    }

    @Test
    void theSignInPageIsAskedForTheReadersLanguage() throws Exception {
        mockMvc.perform(get("/api/auth/login/keycloak").param("ui_locales", "fr"))
                .andExpect(header().string("Location", containsString("ui_locales=fr")));
    }

    @Test
    void aLanguageWeDoNotHaveIsNotPassedOn() throws Exception {
        mockMvc.perform(get("/api/auth/login/keycloak").param("ui_locales", "evil param"))
                .andExpect(header().string("Location", not(containsString("ui_locales"))));
    }

    @Test
    void aBrowserSessionIdentifiesTheSameUserAnAccessTokenWould() throws Exception {
        mockMvc.perform(get("/api/me").with(asBrowser(KEYCLOAK_ID, EMAIL, "owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.owner").value(true))
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        // The provisioning filter reads a session exactly as it reads a token.
        assertThat(users.findByKeycloakId(KEYCLOAK_ID)).isPresent();
    }

    @Test
    void anAssistantIsRecognisedByTheirSessionsRoleToo() throws Exception {
        mockMvc.perform(get("/api/me").with(asBrowser("kc-helper", "helper@example.com", "assistant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(false))
                .andExpect(jsonPath("$.permissions.length()").value(0));
    }

    @Test
    void aBrowserWritingWithoutTheForgeryTokenIsRefused() throws Exception {
        // The session cookie alone is not proof of intent: another site could
        // cause the browser to send it. The header a script has to add is.
        mockMvc.perform(post("/api/auth/password")
                        .with(asBrowser(KEYCLOAK_ID, EMAIL, "owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"a-good-secret\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void choosingAPasswordClearsTheObligationToChooseOne() throws Exception {
        AppUser handedOver = users.save(new AppUser("kc-handed", "handed@example.com", "Hana", "Over", true));

        mockMvc.perform(get("/api/me").with(asBrowser("kc-handed", "handed@example.com", "assistant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        mockMvc.perform(post("/api/auth/password")
                        .with(asBrowser("kc-handed", "handed@example.com", "assistant"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"a-secret-only-i-know\"}"))
                .andExpect(status().isNoContent());

        verify(keycloak).resetPassword(eq("kc-handed"), eq("a-secret-only-i-know"));
        assertThat(users.findById(handedOver.getId()).orElseThrow().isMustChangePassword()).isFalse();
    }

    @Test
    void aShortPasswordIsRefusedBeforeKeycloakIsCalled() throws Exception {
        mockMvc.perform(post("/api/auth/password")
                        .with(asBrowser(KEYCLOAK_ID, EMAIL, "owner"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void anAssistantIsCreatedOwingUsAPasswordOfTheirOwn() throws Exception {
        given(keycloak.createUser(eq("nora@example.com"), eq("Nora"), eq("New"), anyString(), eq("assistant")))
                .willReturn("kc-nora");
        mockMvc.perform(get("/api/me").with(asBrowser(KEYCLOAK_ID, EMAIL, "owner"))).andExpect(status().isOk());

        mockMvc.perform(post("/api/assistants")
                        .with(asBrowser(KEYCLOAK_ID, EMAIL, "owner"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nora@example.com","firstName":"Nora","lastName":"New",
                                 "permissions":["EXPENSE_READ"]}
                                """))
                .andExpect(status().isCreated());

        assertThat(users.findByEmailIgnoreCase("nora@example.com").orElseThrow().isMustChangePassword())
                .isTrue();
    }
}
