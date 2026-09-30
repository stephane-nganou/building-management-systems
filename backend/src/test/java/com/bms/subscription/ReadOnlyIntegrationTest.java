package com.bms.subscription;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Once an owner's subscription has lapsed, their data can still be read by
 * anyone who could read it before, but nobody can change it.
 */
class ReadOnlyIntegrationTest extends AbstractIntegrationTest {

    private static final String EXPIRED = "The subscription has expired. The data can be read but no longer changed.";
    private static final String SUSPENDED = "This account is suspended.";

    private static final RequestPostProcessor OWNER = asUser("owner-a", "owner-a@example.com");
    private static final RequestPostProcessor OTHER_OWNER = asUser("owner-b", "owner-b@example.com");
    private static final RequestPostProcessor ASSISTANT = asAssistant("assistant-a", "assistant-a@example.com");

    /** Writes that belong to no owner's data, and so are never refused for a subscription. */
    private static final Set<String> NOT_OWNER_DATA = Set.of(
            "POST /api/auth/register",
            "POST /api/auth/password");

    @Autowired
    private ScenarioBuilder scenario;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    private String building;
    private String apartment;
    private String tenant;
    private String expense;
    private String invoice;
    private String assignment;

    @BeforeEach
    void anOwnerWithOneOfEverything() throws Exception {
        building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        apartment = scenario.createApartment(OWNER, building, "1A", "900.00");
        tenant = scenario.createTenant(OWNER, apartment, "Weber");
        expense = scenario.createExpense(OWNER, building, "250.00", "2026-02-10");
        invoice = scenario.createRentInvoice(OWNER, tenant, "2026-02-01");
        mockMvc.perform(get("/api/me").with(ASSISTANT)).andExpect(status().isOk());
        assignment = grantAssistant(OWNER, "\"BUILDING_READ\",\"BUILDING_WRITE\",\"INVOICE_READ\"");
    }

    @Test
    void everyWriteToAnExpiredOwnersDataIsRefused() throws Exception {
        testData.expire("owner-a");

        for (Write write : writes()) {
            mockMvc.perform(write.toRequest().with(OWNER))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value(EXPIRED));
        }
    }

    /** New write endpoints have to be added to {@link #writes()}, which keeps them covered above. */
    @Test
    void theWritesAboveAreEveryWriteTheApiHas() {
        Set<String> routed = new TreeSet<>();
        mappings.getHandlerMethods().keySet().forEach(info -> info.getMethodsCondition().getMethods().stream()
                .filter(method -> !method.name().equals("GET"))
                .forEach(method -> info.getPathPatternsCondition().getPatternValues().stream()
                        .filter(pattern -> pattern.startsWith("/api/"))
                        .forEach(pattern -> routed.add(method.name() + " " + pattern))));
        routed.removeAll(NOT_OWNER_DATA);

        assertThat(routed).isEqualTo(new TreeSet<>(writes().stream().map(Write::route).toList()));
    }

    @Test
    void anExpiredOwnerCanStillReadEverything() throws Exception {
        testData.expire("owner-a");

        mockMvc.perform(get("/api/buildings/" + building).with(OWNER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/expenses").with(OWNER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/invoices/" + invoice + "/pdf").with(OWNER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/profit-loss").param("from", "2026-01-01").param("to", "2026-12-31")
                .with(OWNER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/assistants").with(OWNER)).andExpect(status().isOk());
    }

    @Test
    void anAssistantKeepsReadingForAnExpiredOwnerButCannotWrite() throws Exception {
        testData.expire("owner-a");

        mockMvc.perform(get("/api/invoices/" + invoice + "/pdf").with(ASSISTANT)).andExpect(status().isOk());
        mockMvc.perform(buildingUpdate(building).with(ASSISTANT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(EXPIRED));
    }

    /** The lapse is per owner: the same assistant still works for someone who is paid up. */
    @Test
    void anAssistantStillWritesForAnotherOwnerWhoseSubscriptionIsActive() throws Exception {
        String otherBuilding = scenario.createBuilding(OTHER_OWNER, "Nebenstrasse 2");
        grantAssistant(OTHER_OWNER, "\"BUILDING_READ\",\"BUILDING_WRITE\"");
        testData.expire("owner-a");

        mockMvc.perform(buildingUpdate(otherBuilding).with(ASSISTANT)).andExpect(status().isOk());
        mockMvc.perform(buildingUpdate(building).with(ASSISTANT)).andExpect(status().isForbidden());
    }

    @Test
    void aSuspendedOwnersDataIsReadOnlyForTheirAssistant() throws Exception {
        testData.suspend("owner-a");

        mockMvc.perform(get("/api/buildings/" + building).with(ASSISTANT)).andExpect(status().isOk());
        mockMvc.perform(buildingUpdate(building).with(ASSISTANT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(SUSPENDED));
    }

    private List<Write> writes() {
        return List.of(
                new Write(HttpMethod.POST, "/api/buildings", "/api/buildings", BUILDING),
                new Write(HttpMethod.PUT, "/api/buildings/{id}", "/api/buildings/" + building, BUILDING),
                new Write(HttpMethod.DELETE, "/api/buildings/{id}", "/api/buildings/" + building, null),
                new Write(HttpMethod.POST, "/api/buildings/{buildingId}/apartments",
                        "/api/buildings/" + building + "/apartments", APARTMENT),
                new Write(HttpMethod.PUT, "/api/apartments/{id}", "/api/apartments/" + apartment, APARTMENT),
                new Write(HttpMethod.DELETE, "/api/apartments/{id}", "/api/apartments/" + apartment, null),
                new Write(HttpMethod.POST, "/api/apartments/{apartmentId}/tenants",
                        "/api/apartments/" + apartment + "/tenants", TENANT),
                new Write(HttpMethod.PUT, "/api/tenants/{id}", "/api/tenants/" + tenant, TENANT),
                new Write(HttpMethod.DELETE, "/api/tenants/{id}", "/api/tenants/" + tenant, null),
                new Write(HttpMethod.POST, "/api/expenses", "/api/expenses", expenseBody()),
                new Write(HttpMethod.PUT, "/api/expenses/{id}", "/api/expenses/" + expense, expenseBody()),
                new Write(HttpMethod.DELETE, "/api/expenses/{id}", "/api/expenses/" + expense, null),
                new Write(HttpMethod.POST, "/api/invoices", "/api/invoices", invoiceBody()),
                new Write(HttpMethod.POST, "/api/invoices/{id}/status",
                        "/api/invoices/" + invoice + "/status?status=SENT", null),
                new Write(HttpMethod.DELETE, "/api/invoices/{id}", "/api/invoices/" + invoice, null),
                new Write(HttpMethod.POST, "/api/assistants", "/api/assistants", ASSISTANT_GRANT),
                new Write(HttpMethod.PUT, "/api/assistants/{id}", "/api/assistants/" + assignment,
                        "{\"permissions\":[\"BUILDING_READ\"]}"),
                new Write(HttpMethod.POST, "/api/assistants/{id}/password",
                        "/api/assistants/" + assignment + "/password", null),
                new Write(HttpMethod.DELETE, "/api/assistants/{id}", "/api/assistants/" + assignment, null));
    }

    private String grantAssistant(RequestPostProcessor owner, String permissions) throws Exception {
        String json = mockMvc.perform(post("/api/assistants").with(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"assistant-a@example.com","firstName":"Adam","lastName":"Assistant",
                                 "permissions":[%s]}
                                """.formatted(permissions)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private MockHttpServletRequestBuilder buildingUpdate(String id) {
        return request(HttpMethod.PUT, "/api/buildings/" + id).contentType(MediaType.APPLICATION_JSON).content(BUILDING);
    }

    private String expenseBody() {
        return """
                {"buildingId":"%s","category":"MAINTENANCE","amount":80.00,"incurredOn":"2026-02-11",
                 "description":"Lock","vendor":"Schloss AG"}
                """.formatted(building);
    }

    private String invoiceBody() {
        return """
                {"tenantId":"%s","type":"RENT","periodStart":"2026-03-01","periodEnd":"2026-03-31",
                 "issueDate":"2026-03-01","dueDate":"2026-03-15"}
                """.formatted(tenant);
    }

    private static final String BUILDING = """
            {"name":"Renamed","street":"Hauptstrasse 1","city":"Berlin","postalCode":"10115","country":"DE"}
            """;

    private static final String APARTMENT = """
            {"label":"2B","rooms":2,"bedrooms":1,"bathrooms":1,"kitchens":1,"toilets":1,
             "baseRent":700.00,"utilitiesAdvance":100.00,"status":"VACANT"}
            """;

    private static final String TENANT = """
            {"firstName":"Kim","lastName":"Lee","leaseStart":"2026-03-01","active":false}
            """;

    private static final String ASSISTANT_GRANT = """
            {"email":"someone@example.com","firstName":"Some","lastName":"One","permissions":["BUILDING_READ"]}
            """;

    private record Write(HttpMethod method, String pattern, String url, String body) {

        String route() {
            return method.name() + " " + pattern;
        }

        MockHttpServletRequestBuilder toRequest() {
            MockHttpServletRequestBuilder request = request(method, url);
            return body == null ? request : request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
    }
}
