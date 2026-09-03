package com.bms.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Creates domain records through the public API, the way a real client would. */
@Component
public class ScenarioBuilder {

    private final MockMvc mockMvc;

    public ScenarioBuilder(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public String createBuilding(RequestPostProcessor user, String name) throws Exception {
        String body = """
                {"name":"%s","street":"Hauptstrasse 1","city":"Berlin","postalCode":"10115","country":"DE"}
                """.formatted(name);
        return idOf(post("/api/buildings"), user, body);
    }

    public String createApartment(RequestPostProcessor user, String buildingId, String label, String rent)
            throws Exception {
        String body = """
                {"label":"%s","floor":1,"sizeSqm":72.5,"rooms":3,"bedrooms":2,"bathrooms":1,
                 "kitchens":1,"toilets":1,"baseRent":%s,"utilitiesAdvance":150.00,"status":"OCCUPIED"}
                """.formatted(label, rent);
        return idOf(post("/api/buildings/" + buildingId + "/apartments"), user, body);
    }

    public String createTenant(RequestPostProcessor user, String apartmentId, String lastName) throws Exception {
        String body = """
                {"firstName":"Alex","lastName":"%s","email":"alex@example.com","phone":"+49 30 1234",
                 "leaseStart":"2026-01-01","deposit":1800.00,"active":true}
                """.formatted(lastName);
        return idOf(post("/api/apartments/" + apartmentId + "/tenants"), user, body);
    }

    public String createExpense(RequestPostProcessor user, String buildingId, String amount, String incurredOn)
            throws Exception {
        String body = """
                {"buildingId":"%s","category":"MAINTENANCE","amount":%s,"incurredOn":"%s",
                 "description":"Roof repair","vendor":"Dach GmbH"}
                """.formatted(buildingId, amount, incurredOn);
        return idOf(post("/api/expenses"), user, body);
    }

    public String createRentInvoice(RequestPostProcessor user, String tenantId, String issueDate) throws Exception {
        String body = """
                {"tenantId":"%s","type":"RENT","periodStart":"2026-02-01","periodEnd":"2026-02-28",
                 "issueDate":"%s","dueDate":"2026-02-15"}
                """.formatted(tenantId, issueDate);
        return idOf(post("/api/invoices"), user, body);
    }

    private String idOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                        RequestPostProcessor user, String body) throws Exception {
        String json = mockMvc.perform(request.with(user).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
