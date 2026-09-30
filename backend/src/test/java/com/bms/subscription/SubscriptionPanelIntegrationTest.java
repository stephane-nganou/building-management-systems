package com.bms.subscription;

import java.time.LocalDate;

import com.bms.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What an owner sees on their Manage subscription screen, and who else may see it. */
class SubscriptionPanelIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-a", "owner-a@example.com");

    @Test
    void anOwnerSeesTheirStandingEveryPeriodAndWhomToContact() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        LocalDate lastDay = LocalDate.now().plusDays(5);
        testData.setPeriod("owner-a", LocalDate.now().minusDays(20), lastDay);
        testData.addPeriod("owner-a", lastDay.plusDays(30), lastDay.plusDays(60));

        mockMvc.perform(get("/api/subscription").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standing.status").value("ACTIVE"))
                .andExpect(jsonPath("$.standing.endsOn").value(lastDay.toString()))
                .andExpect(jsonPath("$.standing.daysLeft").value(5))
                .andExpect(jsonPath("$.standing.endingSoon").value(true))
                .andExpect(jsonPath("$.periods.length()").value(2))
                .andExpect(jsonPath("$.periods[0].startsOn").value(lastDay.plusDays(30).toString()))
                .andExpect(jsonPath("$.support.email").value("support@hausbuch.example"))
                .andExpect(jsonPath("$.support.phone").isNotEmpty())
                .andExpect(jsonPath("$.support.hours").isNotEmpty());
    }

    @Test
    void anAssistantHasNoSubscriptionToManage() throws Exception {
        mockMvc.perform(get("/api/subscription").with(asAssistant("assistant-a", "assistant-a@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only an owner has a subscription to manage."));
    }

    /** Refused everything else, a suspended owner can still read why and whom to call. */
    @Test
    void aSuspendedOwnerCanStillReadTheirSubscriptionAndTheContacts() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        testData.suspend("owner-a");

        mockMvc.perform(get("/api/subscription").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standing.status").value("SUSPENDED"));
        mockMvc.perform(get("/api/support").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("support@hausbuch.example"));
        mockMvc.perform(get("/api/buildings").with(OWNER)).andExpect(status().isForbidden());
    }
}
