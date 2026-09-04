package com.bms.access;

import java.util.List;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantAccessIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-a", "owner-a@example.com");
    private static final RequestPostProcessor ASSISTANT = asAssistant("assistant-a", "assistant-a@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    /** The assistant must exist locally before they can be granted access. */
    private void assistantSignsInOnce() throws Exception {
        mockMvc.perform(get("/api/me").with(ASSISTANT)).andExpect(status().isOk());
    }

    private void grant(String permissions) throws Exception {
        String body = """
                {"email":"assistant-a@example.com","firstName":"Adam","lastName":"Assistant",
                 "permissions":[%s]}
                """.formatted(permissions);
        mockMvc.perform(post("/api/assistants").with(OWNER)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void assistantWithoutAGrantSeesNothing() throws Exception {
        scenario.createBuilding(OWNER, "Hauptstrasse 1");
        assistantSignsInOnce();

        String list = mockMvc.perform(get("/api/buildings").with(ASSISTANT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<?>>read(list, "$")).isEmpty();
    }

    @Test
    void readGrantLetsTheAssistantReadButNotWrite() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        assistantSignsInOnce();
        grant("\"BUILDING_READ\"");

        mockMvc.perform(get("/api/buildings/" + building).with(ASSISTANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hauptstrasse 1"));

        // Writing needs BUILDING_WRITE, which was not granted.
        mockMvc.perform(post("/api/buildings/" + building + "/apartments").with(ASSISTANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"2B","rooms":2,"bedrooms":1,"bathrooms":1,"kitchens":1,"toilets":1,
                                 "baseRent":700.00,"utilitiesAdvance":100.00,"status":"VACANT"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void writeGrantLetsTheAssistantCreateApartments() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        assistantSignsInOnce();
        grant("\"BUILDING_READ\",\"APARTMENT_READ\",\"APARTMENT_WRITE\"");

        String apartment = scenario.createApartment(ASSISTANT, building, "2B", "700.00");
        assertThat(apartment).isNotBlank();

        mockMvc.perform(get("/api/apartments").param("buildingId", building).with(ASSISTANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void revokingTheGrantRemovesAccessAgain() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        assistantSignsInOnce();
        grant("\"BUILDING_READ\"");

        String assistants = mockMvc.perform(get("/api/assistants").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String assignmentId = JsonPath.read(assistants, "$[0].id");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/assistants/" + assignmentId).with(OWNER))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/buildings/" + building).with(ASSISTANT))
                .andExpect(status().isNotFound());
    }

    @Test
    void meReportsTheDelegationsTheAssistantHolds() throws Exception {
        scenario.createBuilding(OWNER, "Hauptstrasse 1");
        assistantSignsInOnce();
        grant("\"BUILDING_READ\",\"EXPENSE_READ\"");

        mockMvc.perform(get("/api/me").with(ASSISTANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("assistant-a@example.com"))
                .andExpect(jsonPath("$.owner").value(false))
                .andExpect(jsonPath("$.permissions.length()").value(2))
                .andExpect(jsonPath("$.assistingFor.length()").value(1))
                .andExpect(jsonPath("$.assistingFor[0].permissions.length()").value(2));
    }

    /** An owner holds every permission implicitly, which is what hides nothing from them. */
    @Test
    void meReportsAnOwnerAsHoldingEveryPermission() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(true))
                .andExpect(jsonPath("$.permissions.length()").value(Permission.values().length))
                .andExpect(jsonPath("$.assistingFor.length()").value(0));
    }

    @Test
    void addingAnUnknownEmailCreatesTheAccountAndReturnsItsPasswordOnce() throws Exception {
        given(keycloak.createUser(eq("new-assistant@example.com"), eq("Nora"), eq("New"),
                anyString(), eq(true), eq("assistant"))).willReturn("kc-new-assistant");
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());

        String created = mockMvc.perform(post("/api/assistants").with(OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new-assistant@example.com","firstName":"Nora","lastName":"New",
                                 "permissions":["BUILDING_READ"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new-assistant@example.com"))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Listing them again never repeats the password.
        mockMvc.perform(get("/api/assistants").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].temporaryPassword").doesNotExist());

        String assignmentId = JsonPath.read(created, "$.id");
        mockMvc.perform(post("/api/assistants/" + assignmentId + "/password").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());
        verify(keycloak).resetPassword(eq("kc-new-assistant"), anyString(), eq(true));
    }

    @Test
    void ownerCannotAddThemselfAsAssistant() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());

        mockMvc.perform(post("/api/assistants").with(OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner-a@example.com","firstName":"Olivia","lastName":"Owner",
                                 "permissions":["BUILDING_READ"]}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
