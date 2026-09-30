package com.bms.subscription;

import java.time.LocalDate;
import java.util.List;

import com.bms.support.AbstractIntegrationTest;
import com.bms.user.AppUser;
import com.bms.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every owner starts on a thirty day trial, however their account came to exist. */
class TrialIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AppUserRepository users;

    @Autowired
    private SubscriptionPeriodRepository periods;

    @Test
    void signingUpStartsATrialOfThirtyDaysFromToday() throws Exception {
        given(keycloak.createUser(anyString(), anyString(), anyString(), anyString(), eq("owner")))
                .willReturn("kc-nina");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"nina@example.com","firstName":"Nina","lastName":"Neu","password":"a-good-secret"}
                        """))
                .andExpect(status().isCreated());

        SubscriptionPeriod trial = onlyPeriodOf(users.findByEmailIgnoreCase("nina@example.com").orElseThrow());
        assertThat(trial.getStartsOn()).isEqualTo(LocalDate.now());
        assertThat(trial.getEndsOn()).isEqualTo(LocalDate.now().plusDays(29));
    }

    @Test
    void anOwnerMadeInKeycloakGetsATrialOnTheirFirstRequest() throws Exception {
        mockMvc.perform(get("/api/me").with(asUser("kc-demo", "demo@example.com"))).andExpect(status().isOk());

        assertThat(onlyPeriodOf(users.findByKeycloakId("kc-demo").orElseThrow()).getNote()).isEqualTo("Trial");
    }

    @Test
    void anAssistantHasNoSubscriptionOfTheirOwn() throws Exception {
        mockMvc.perform(get("/api/me").with(asAssistant("kc-sam", "sam@example.com"))).andExpect(status().isOk());

        assertThat(periods.findByOwnerIdOrderByStartsOnDesc(users.findByKeycloakId("kc-sam").orElseThrow().getId()))
                .isEmpty();
    }

    /**
     * The V3 migration could only guess who owned data, since roles live in
     * Keycloak, and an assistant nobody had assigned yet looked like an owner.
     * Their token settles it the next time they sign in.
     */
    @Test
    void anAssistantTheMigrationTookForAnOwnerLosesTheTrialOnTheirNextRequest() throws Exception {
        mockMvc.perform(get("/api/me").with(asAssistant("kc-sam", "sam@example.com"))).andExpect(status().isOk());
        testData.setPeriod("kc-sam", LocalDate.now(), LocalDate.now().plusDays(29));

        mockMvc.perform(get("/api/me").with(asAssistant("kc-sam", "sam@example.com"))).andExpect(status().isOk());

        assertThat(periods.findByOwnerIdOrderByStartsOnDesc(users.findByKeycloakId("kc-sam").orElseThrow().getId()))
                .isEmpty();
    }

    private SubscriptionPeriod onlyPeriodOf(AppUser owner) {
        List<SubscriptionPeriod> found = periods.findByOwnerIdOrderByStartsOnDesc(owner.getId());
        assertThat(found).hasSize(1);
        return found.getFirst();
    }
}
