package com.bms.subscription;

import java.time.LocalDate;
import java.util.Set;

import com.bms.access.Permission;
import com.bms.support.AbstractIntegrationTest;
import com.bms.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asAdmin;
import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What the profile says about a caller's standing, and what a suspension does to their session. */
class StandingIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-a", "owner-a@example.com");
    private static final RequestPostProcessor ASSISTANT = asAssistant("assistant-a", "assistant-a@example.com");

    @Autowired
    private AppUserRepository users;

    @Test
    void anOwnerSeesTheirSubscriptionAndWhenItRunsOut() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.suspended").value(false))
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subscription.endsOn").value(LocalDate.now().plusDays(29).toString()));

        testData.expire("owner-a");

        mockMvc.perform(get("/api/me").with(OWNER))
                .andExpect(jsonPath("$.subscription.status").value("EXPIRED"))
                .andExpect(jsonPath("$.subscription.endsOn").value(LocalDate.now().minusDays(1).toString()));
    }

    /** "At least a week before": the warning starts seven days ahead of the last day. */
    @Test
    void anOwnerIsWarnedFromAWeekBeforeTheLastDay() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());

        testData.setPeriod("owner-a", LocalDate.now().minusDays(20), LocalDate.now().plusDays(8));
        mockMvc.perform(get("/api/me").with(OWNER))
                .andExpect(jsonPath("$.subscription.daysLeft").value(8))
                .andExpect(jsonPath("$.subscription.endingSoon").value(false));

        testData.setPeriod("owner-a", LocalDate.now().minusDays(20), LocalDate.now().plusDays(7));
        mockMvc.perform(get("/api/me").with(OWNER))
                .andExpect(jsonPath("$.subscription.daysLeft").value(7))
                .andExpect(jsonPath("$.subscription.endingSoon").value(true));
    }

    @Test
    void aRenewalAlreadyBookedMeansNothingIsEnding() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        LocalDate lastDay = LocalDate.now().plusDays(3);
        testData.setPeriod("owner-a", LocalDate.now().minusDays(20), lastDay);
        testData.addPeriod("owner-a", lastDay.plusDays(1), lastDay.plusYears(1));

        mockMvc.perform(get("/api/me").with(OWNER))
                .andExpect(jsonPath("$.subscription.endsOn").value(lastDay.plusYears(1).toString()))
                .andExpect(jsonPath("$.subscription.endingSoon").value(false));
    }

    @Test
    void anAssistantSeesTheStandingOfEachOwnerTheyWorkFor() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/me").with(ASSISTANT)).andExpect(status().isOk());
        testData.assign(users.findByKeycloakId("owner-a").orElseThrow(),
                users.findByKeycloakId("assistant-a").orElseThrow(), Set.of(Permission.BUILDING_READ));
        testData.expire("owner-a");

        mockMvc.perform(get("/api/me").with(ASSISTANT))
                .andExpect(jsonPath("$.subscription").doesNotExist())
                .andExpect(jsonPath("$.assistingFor[0].ownerStatus").value("EXPIRED"));
    }

    @Test
    void anAdministratorIsNeitherOwnerNorSubscriber() throws Exception {
        mockMvc.perform(get("/api/me").with(asAdmin("admin-a", "admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.owner").value(false))
                .andExpect(jsonPath("$.permissions.length()").value(0))
                .andExpect(jsonPath("$.subscription").doesNotExist());
    }

    /** Disabling the account in Keycloak stops new sign ins; this stops the session already open. */
    @Test
    void aSuspendedOwnerIsRefusedEverythingButTheirProfile() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        testData.suspend("owner-a");

        mockMvc.perform(get("/api/buildings").with(OWNER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("This account is suspended."));
        mockMvc.perform(get("/api/buildings").with(OWNER).header("Accept-Language", "fr"))
                .andExpect(jsonPath("$.detail").value("Ce compte est suspendu."));
        mockMvc.perform(get("/api/me").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(true))
                .andExpect(jsonPath("$.subscription.status").value("SUSPENDED"));
    }
}
