package com.bms.identity;

import com.bms.common.exception.IdentityProviderException;
import com.bms.support.AbstractIntegrationTest;
import com.bms.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationIntegrationTest extends AbstractIntegrationTest {

    private static final String SIGN_UP = """
            {"email":"nina@example.com","firstName":"Nina","lastName":"Neu","password":"a-good-secret"}
            """;

    @Autowired
    private AppUserRepository users;

    @Test
    void anyoneCanSignUpAsAnOwnerWithoutAToken() throws Exception {
        given(keycloak.createUser(eq("nina@example.com"), eq("Nina"), eq("Neu"),
                eq("a-good-secret"), eq(false), eq("owner"))).willReturn("kc-nina");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(SIGN_UP))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nina@example.com"))
                .andExpect(jsonPath("$.name").value("Nina Neu"));

        // The local record exists straight away, so an owner can be given work
        // before they have signed in for the first time.
        assertThat(users.findByEmailIgnoreCase("nina@example.com")).isPresent();
        verify(keycloak).createUser(anyString(), anyString(), anyString(), anyString(), eq(false), eq("owner"));
    }

    @Test
    void signingUpTwiceWithTheSameEmailIsRejected() throws Exception {
        given(keycloak.createUser(anyString(), anyString(), anyString(), anyString(), eq(false), eq("owner")))
                .willReturn("kc-nina");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(SIGN_UP))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(SIGN_UP))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("An account already exists for nina@example.com"));
    }

    @Test
    void aShortPasswordIsRejectedBeforeKeycloakIsCalled() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nina@example.com","firstName":"Nina","lastName":"Neu","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    /**
     * A Keycloak that will not talk to us is a server side fault. Reporting it
     * as a 401 made a configuration problem look like an authentication one.
     */
    @Test
    void aKeycloakFailureIsReportedAsABadGatewayRatherThanUnauthorized() throws Exception {
        given(keycloak.createUser(anyString(), anyString(), anyString(), anyString(), eq(false), eq("owner")))
                .willThrow(new IdentityProviderException("the bms-backend client is missing"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(SIGN_UP))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Accounts cannot be managed right now. Please try again later."));

        assertThat(users.findByEmailIgnoreCase("nina@example.com")).isEmpty();
    }

    /** Everything else still needs a token; registration is the single exception. */
    @Test
    void theRestOfTheApiStaysClosedToAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
}
