package com.bms.admin;

import java.time.LocalDate;
import java.util.Set;

import com.bms.access.Permission;
import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import com.bms.user.AppUser;
import com.bms.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asAdmin;
import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAccountIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor ADMIN = asAdmin("admin-a", "admin@example.com");
    private static final RequestPostProcessor OWNER = asUser("owner-a", "owner-a@example.com");
    private static final RequestPostProcessor ASSISTANT = asAssistant("assistant-a", "assistant-a@example.com");

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private ScenarioBuilder scenario;

    @Autowired
    private AppUserRepository users;

    private AppUser owner;

    @BeforeEach
    void anOwnerWithABuildingAndAnAssistant() throws Exception {
        scenario.createBuilding(OWNER, "Hauptstrasse 1");
        mockMvc.perform(get("/api/me").with(ASSISTANT)).andExpect(status().isOk());
        mockMvc.perform(get("/api/me").with(ADMIN)).andExpect(status().isOk());
        owner = users.findByKeycloakId("owner-a").orElseThrow();
        testData.assign(owner, users.findByKeycloakId("assistant-a").orElseThrow(), Set.of(Permission.BUILDING_READ));
    }

    @Test
    void onlyAnAdministratorReachesTheAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/accounts").with(OWNER)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/accounts").with(ASSISTANT)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/accounts").with(ADMIN)).andExpect(status().isOk());
    }

    /** Assistants and administrators are not customers; they never had a subscription. */
    @Test
    void theListHoldsOwnersOnlyWithWhatTheyHaveAndWhereTheyStand() throws Exception {
        mockMvc.perform(get("/api/admin/accounts").with(ADMIN))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("owner-a@example.com"))
                .andExpect(jsonPath("$[0].buildings").value(1))
                .andExpect(jsonPath("$[0].assistants").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].endsOn").value(TODAY.plusDays(29).toString()))
                .andExpect(jsonPath("$[0].temporaryPassword").doesNotExist());
    }

    @Test
    void theListFiltersByStanding() throws Exception {
        testData.expire("owner-a");

        mockMvc.perform(get("/api/admin/accounts").param("status", "ACTIVE").with(ADMIN))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/admin/accounts").param("status", "EXPIRED").with(ADMIN))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void anAdministratorSignsAnOwnerUpWithAPasswordToHandOver() throws Exception {
        given(keycloak.createUser(eq("new@example.com"), eq("Nina"), eq("Neu"), anyString(), eq("owner")))
                .willReturn("kc-nina");

        mockMvc.perform(post("/api/admin/accounts").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","firstName":"Nina","lastName":"Neu"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());

        assertThat(users.findByKeycloakId("kc-nina").orElseThrow().isMustChangePassword()).isTrue();
    }

    @Test
    void addingAPeriodThatCoversTodayRenewsAnExpiredOwner() throws Exception {
        testData.expire("owner-a");

        mockMvc.perform(post("/api/admin/accounts/" + owner.getId() + "/periods").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsOn":"%s","endsOn":"%s","note":"Paid for a year"}
                                """.formatted(TODAY, TODAY.plusYears(1))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/accounts/" + owner.getId() + "/periods").with(ADMIN))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].note").value("Paid for a year"));
        mockMvc.perform(post("/api/buildings").with(OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nebenstrasse 2\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void aPeriodCannotEndBeforeItStarts() throws Exception {
        mockMvc.perform(post("/api/admin/accounts/" + owner.getId() + "/periods").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsOn":"2026-05-01","endsOn":"2026-04-30"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A period cannot end before it starts"));
    }

    @Test
    void endingTheSubscriptionMakesYesterdayItsLastDay() throws Exception {
        testData.setPeriod("owner-a", TODAY.minusDays(10), TODAY.plusDays(10));

        mockMvc.perform(post("/api/admin/accounts/" + owner.getId() + "/periods/end").with(ADMIN))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/accounts").with(ADMIN))
                .andExpect(jsonPath("$[0].status").value("EXPIRED"))
                .andExpect(jsonPath("$[0].endsOn").value(TODAY.minusDays(1).toString()));
    }

    /** A trial that began today is cut short, not deleted: the owner stays a customer who can be renewed. */
    @Test
    void endingASubscriptionThatBeganTodayKeepsTheOwnerOnTheList() throws Exception {
        testData.setPeriod("owner-a", TODAY, TODAY.plusDays(29));

        mockMvc.perform(post("/api/admin/accounts/" + owner.getId() + "/periods/end").with(ADMIN))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/accounts").with(ADMIN))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("EXPIRED"));
        mockMvc.perform(get("/api/admin/accounts/" + owner.getId() + "/periods").with(ADMIN))
                .andExpect(jsonPath("$[0].startsOn").value(TODAY.toString()))
                .andExpect(jsonPath("$[0].endsOn").value(TODAY.minusDays(1).toString()));
    }

    @Test
    void suspendingDisablesTheAccountInKeycloakAndReactivatingEnablesIt() throws Exception {
        mockMvc.perform(post("/api/admin/accounts/" + owner.getId() + "/suspension").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        verify(keycloak).setEnabled("owner-a", false);
        mockMvc.perform(get("/api/buildings").with(OWNER)).andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/accounts/" + owner.getId() + "/suspension").with(ADMIN))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        verify(keycloak).setEnabled("owner-a", true);
        mockMvc.perform(get("/api/buildings").with(OWNER)).andExpect(status().isOk());
    }

    @Test
    void anAssistantIsNotAnAccountTheAdministratorManages() throws Exception {
        AppUser assistant = users.findByKeycloakId("assistant-a").orElseThrow();

        mockMvc.perform(post("/api/admin/accounts/" + assistant.getId() + "/suspension").with(ADMIN))
                .andExpect(status().isNotFound());
    }
}
