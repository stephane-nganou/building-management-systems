package com.bms.access;

import java.util.List;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantAccessIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-a", "owner-a@example.com");
    private static final RequestPostProcessor ASSISTANT = asUser("assistant-a", "assistant-a@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    /** The assistant must exist locally before they can be granted access. */
    private void assistantSignsInOnce() throws Exception {
        mockMvc.perform(get("/api/me").with(ASSISTANT)).andExpect(status().isOk());
    }

    private void grant(String permissions) throws Exception {
        String body = """
                {"email":"assistant-a@example.com","permissions":[%s]}
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
                .andExpect(jsonPath("$.assistingFor.length()").value(1))
                .andExpect(jsonPath("$.assistingFor[0].permissions.length()").value(2));
    }

    @Test
    void ownerCannotAddThemselfAsAssistant() throws Exception {
        mockMvc.perform(get("/api/me").with(OWNER)).andExpect(status().isOk());

        mockMvc.perform(post("/api/assistants").with(OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner-a@example.com\",\"permissions\":[\"BUILDING_READ\"]}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
