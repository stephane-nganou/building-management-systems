package com.bms.access;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asAdmin;
import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static com.bms.support.Jwts.withoutRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What happens to an owner's assistants when the owner is suspended or lets
 * their subscription lapse, and what an account with no role may do.
 */
class AssistantLifecycleIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-a", "owner-a@example.com");
    private static final RequestPostProcessor ASSISTANT = asAssistant("assistant-a", "assistant-a@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    private String building;
    private String assignment;

    @BeforeEach
    void anAssistantWhoMayReadBuildings() throws Exception {
        building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        mockMvc.perform(get("/api/me").with(ASSISTANT)).andExpect(status().isOk());
        String granted = mockMvc.perform(post("/api/assistants").with(OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"assistant-a@example.com","firstName":"Adam","lastName":"Assistant",
                                 "permissions":["BUILDING_READ"]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assignment = JsonPath.read(granted, "$.id");
    }

    private void permissions(String list, int expectedStatus) throws Exception {
        mockMvc.perform(put("/api/assistants/" + assignment).with(OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[%s]}".formatted(list)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void aSuspendedOwnersAssistantLosesAccessToo() throws Exception {
        mockMvc.perform(get("/api/buildings/" + building).with(ASSISTANT)).andExpect(status().isOk());

        testData.suspend("owner-a");

        mockMvc.perform(get("/api/buildings/" + building).with(ASSISTANT)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/buildings").with(ASSISTANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        // Still listed, so the assistant sees why, but granting nothing.
        mockMvc.perform(get("/api/me").with(ASSISTANT))
                .andExpect(jsonPath("$.assistingFor[0].ownerStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.permissions.length()").value(0));
    }

    @Test
    void anOwnerWhoseSubscriptionLapsedCanStillRevokeAnAssistant() throws Exception {
        testData.expire("owner-a");

        mockMvc.perform(delete("/api/assistants/" + assignment).with(OWNER)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/buildings/" + building).with(ASSISTANT)).andExpect(status().isNotFound());
    }

    @Test
    void anOwnerWhoseSubscriptionLapsedCanNarrowButNotWidenPermissions() throws Exception {
        testData.expire("owner-a");

        permissions("\"BUILDING_READ\",\"BUILDING_WRITE\"", 403);
        permissions("", 200);
    }

    @Test
    void anAccountWithNoRoleOwnsNothing() throws Exception {
        RequestPostProcessor nobody = withoutRole("kc-nobody", "nobody@example.com");

        mockMvc.perform(get("/api/me").with(nobody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(false))
                .andExpect(jsonPath("$.subscription").doesNotExist())
                .andExpect(jsonPath("$.permissions.length()").value(0));
        mockMvc.perform(post("/api/buildings").with(nobody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mine\",\"currency\":\"EUR\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/assistants").with(nobody)).andExpect(status().isForbidden());
    }

    /** It is not an assistant either, so no owner can link it and manage it. */
    @Test
    void anAccountWithNoRoleCannotBeAddedAsAnAssistant() throws Exception {
        mockMvc.perform(get("/api/me").with(withoutRole("kc-nobody", "nobody@example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assistants").with(OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","firstName":"No","lastName":"Body",
                                 "permissions":["BUILDING_READ"]}
                                """))
                .andExpect(status().is(422));
    }

    @Test
    void neitherAnAssistantNorTheAdministratorStartsABuilding() throws Exception {
        for (RequestPostProcessor caller : new RequestPostProcessor[] {
                ASSISTANT, asAdmin("admin-a", "admin-a@example.com")}) {
            mockMvc.perform(post("/api/buildings").with(caller)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Mine\",\"currency\":\"EUR\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/assistants").with(ASSISTANT)).andExpect(status().isForbidden());
    }
}
