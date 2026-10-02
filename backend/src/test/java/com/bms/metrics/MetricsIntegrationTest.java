package com.bms.metrics;

import java.time.LocalDate;
import java.time.ZoneOffset;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import com.bms.user.AppUser;
import com.bms.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asAdmin;
import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetricsIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor ADMIN = asAdmin("admin-m", "admin-m@example.com");
    private static final RequestPostProcessor OWNER = asUser("owner-m", "owner-m@example.com");
    private static final RequestPostProcessor ASSISTANT = asAssistant("assistant-m", "assistant-m@example.com");

    /** The last entry of every per day list. */
    private static final String TODAY = "[-1]";

    @Autowired
    private ScenarioBuilder scenario;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private RequestStats requestStats;

    @Autowired
    private ActivityRecorder activity;

    @Autowired
    private MetricsRetention retention;

    /** Requests other tests made are still in memory; start from none. */
    @BeforeEach
    void noRequestsCountedYet() {
        requestStats.flush();
        jdbc.sql("delete from api_stat_daily").update();
    }

    private ResultActions metrics(String days) throws Exception {
        return mockMvc.perform(get("/api/admin/metrics").param("days", days).with(ADMIN));
    }

    private AppUser user(String keycloakId) {
        return users.findByKeycloakId(keycloakId).orElseThrow();
    }

    @Test
    void onlyAnAdministratorSeesTheMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/metrics").with(OWNER)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/metrics").with(ASSISTANT)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/metrics").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(30));
    }

    @Test
    void theRangeIsOneOfFour() throws Exception {
        metrics("5").andExpect(status().isUnprocessableContent());
        metrics("7")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features.buildings.length()").value(7))
                .andExpect(jsonPath("$.traffic.perDay.length()").value(7));
    }

    @Test
    void whatOwnersCreateIsCountedOnTheDayTheyCreatedIt() throws Exception {
        String building = scenario.createBuilding(OWNER, "Lindenweg 3");
        String apartment = scenario.createApartment(OWNER, building, "3C", "900.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Schulz");
        scenario.createExpense(OWNER, building, "120.00", LocalDate.now().toString());
        scenario.createRentInvoice(OWNER, tenant, "2026-02-01");
        // An older building, counted three days ago and not today.
        String old = scenario.createBuilding(OWNER, "Old");
        jdbc.sql("update building set created_at = now() - interval '3 days' where id = :id::uuid")
                .param("id", old).update();

        metrics("7")
                .andExpect(jsonPath("$.features.buildings" + TODAY + ".count").value(1))
                .andExpect(jsonPath("$.features.buildings[-4].count").value(1))
                .andExpect(jsonPath("$.features.apartments" + TODAY + ".count").value(1))
                .andExpect(jsonPath("$.features.tenants" + TODAY + ".count").value(1))
                .andExpect(jsonPath("$.features.expenses" + TODAY + ".count").value(1))
                .andExpect(jsonPath("$.features.rentInvoices" + TODAY + ".count").value(1))
                .andExpect(jsonPath("$.features.coldWaterInvoices" + TODAY + ".count").value(0));
    }

    /**
     * Every owner starts on a trial. One runs out, one is suspended, and one is
     * given a further period by an administrator, which makes a conversion and
     * counts as subscribed even though the trial outlasts it.
     */
    @Test
    void ownersAreCountedByWhereTheyStand() throws Exception {
        for (String owner : new String[] {"owner-trial", "owner-expired", "owner-suspended", "owner-paid"}) {
            mockMvc.perform(get("/api/me").with(asUser(owner, owner + "@example.com"))).andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/me").with(ASSISTANT)).andExpect(status().isOk());
        testData.expire("owner-expired");
        testData.suspend("owner-suspended");
        mockMvc.perform(post("/api/admin/accounts/" + user("owner-paid").getId() + "/periods").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsOn":"%s","endsOn":"%s","note":"Paid for a week"}
                                """.formatted(LocalDate.now(), LocalDate.now().plusDays(6))))
                .andExpect(status().isCreated());

        metrics("30")
                .andExpect(jsonPath("$.customers.trial").value(1))
                .andExpect(jsonPath("$.customers.active").value(1))
                .andExpect(jsonPath("$.customers.expired").value(1))
                .andExpect(jsonPath("$.customers.suspended").value(1))
                .andExpect(jsonPath("$.customers.newOwners" + TODAY + ".count").value(4))
                .andExpect(jsonPath("$.customers.trialsConverted").value(1));
    }

    @Test
    void anyoneWhoUsedTheServiceTodayIsActiveOnce() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/me").with(ASSISTANT)).andExpect(status().isOk());
        // An assistant last seen ten days ago: in the month, not in the week.
        mockMvc.perform(get("/api/me").with(asAssistant("assistant-old", "old@example.com")));
        jdbc.sql("update user_activity set day = day - 10 where user_id = :id")
                .param("id", user("assistant-old").getId()).update();

        metrics("30")
                .andExpect(jsonPath("$.activity.dau.owners").value(1))
                .andExpect(jsonPath("$.activity.dau.assistants").value(1))
                .andExpect(jsonPath("$.activity.wau.assistants").value(1))
                .andExpect(jsonPath("$.activity.mau.assistants").value(2))
                .andExpect(jsonPath("$.activity.activeUsers" + TODAY + ".owners").value(1))
                .andExpect(jsonPath("$.activity.activeUsers[-11].assistants").value(1));
    }

    @Test
    void eachSignInIsCounted() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        activity.signedIn(user("owner-m"));
        activity.signedIn(user("owner-m"));

        metrics("7").andExpect(jsonPath("$.activity.signIns" + TODAY + ".count").value(2));
    }

    /** Counted by route pattern rather than address, refusals and failures included. */
    @Test
    void requestsAreCountedByRouteAndStatus() throws Exception {
        String building = scenario.createBuilding(OWNER, "Lindenweg 3");
        mockMvc.perform(get("/api/buildings/" + building).with(OWNER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/buildings/00000000-0000-0000-0000-000000000000").with(OWNER))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/buildings")).andExpect(status().isUnauthorized());
        requestStats.flush();
        mockMvc.perform(get("/api/buildings/" + building).with(OWNER)).andExpect(status().isOk());
        requestStats.flush();

        assertThat(requests("GET", "/api/buildings/{id}", 200)).isEqualTo(2);
        assertThat(requests("GET", "/api/buildings/{id}", 404)).isEqualTo(1);
        assertThat(requests("GET", "UNMATCHED", 401)).isEqualTo(1);
        metrics("7").andExpect(jsonPath("$.traffic.clientErrors").value(2));
    }

    /**
     * A method is whatever the caller writes, and this runs before anybody is
     * signed in: an invented one must neither break the count nor add a row.
     */
    @Test
    void anInventedMethodIsCountedAsOther() throws Exception {
        for (String method : new String[] {"VERSION-CONTROL", "BREW", "WHATEVER-ELSE"}) {
            mockMvc.perform(request(HttpMethod.valueOf(method), "/api/buildings"));
        }
        mockMvc.perform(get("/api/buildings").with(OWNER)).andExpect(status().isOk());
        requestStats.flush();

        assertThat(jdbc.sql("select distinct method from api_stat_daily").query(String.class).list())
                .containsExactlyInAnyOrder("OTHER", "GET");
        assertThat(requests("GET", "/api/buildings", 200)).isEqualTo(1);
    }

    @Test
    void aDownloadedInvoiceIsCountedAsAPdf() throws Exception {
        String building = scenario.createBuilding(OWNER, "Lindenweg 3");
        String apartment = scenario.createApartment(OWNER, building, "3C", "900.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Schulz");
        String invoice = scenario.createRentInvoice(OWNER, tenant, "2026-02-01");
        mockMvc.perform(get("/api/invoices/" + invoice + "/pdf").with(OWNER)).andExpect(status().isOk());
        requestStats.flush();

        metrics("7").andExpect(jsonPath("$.features.pdfDownloads" + TODAY + ".count").value(1));
    }

    @Test
    void whoWasActiveIsForgottenAfterThirteenMonths() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        insertActivity(user("owner-m"), today.minusMonths(14));
        insertActivity(user("owner-m"), today.minusMonths(12));

        retention.purge();

        assertThat(jdbc.sql("select day from user_activity order by day").query(LocalDate.class).list())
                .containsExactly(today.minusMonths(12), today);
    }

    private void insertActivity(AppUser user, LocalDate day) {
        jdbc.sql("insert into user_activity (user_id, day, role) values (:id, :day, 'OWNER')")
                .param("id", user.getId()).param("day", day).update();
    }

    private long requests(String method, String route, int status) {
        return jdbc.sql("select coalesce(sum(requests), 0) from api_stat_daily"
                        + " where method = :method and route = :route and status = :status")
                .param("method", method).param("route", route).param("status", status)
                .query(Long.class).single();
    }
}
