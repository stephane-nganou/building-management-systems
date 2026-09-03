package com.bms.building;

import java.util.List;

import com.bms.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuildingApiIntegrationTest extends AbstractIntegrationTest {

    private static final String BODY = """
            {"name":"Rosenweg 12","street":"Rosenweg 12","city":"Koln",
             "postalCode":"50667","country":"DE","notes":"Corner building"}
            """;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/buildings")).andExpect(status().isUnauthorized());
    }

    @Test
    void createsReadsUpdatesAndDeletesABuilding() throws Exception {
        String created = mockMvc.perform(post("/api/buildings")
                        .with(asUser("owner-1", "owner1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rosenweg 12"))
                .andExpect(jsonPath("$.apartmentCount").value(0))
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/buildings/" + id).with(asUser("owner-1", "owner1@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Koln"));

        mockMvc.perform(put("/api/buildings/" + id)
                        .with(asUser("owner-1", "owner1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"name\":\"Rosenweg 12\"", "\"name\":\"Rosenweg 14\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rosenweg 14"));

        mockMvc.perform(delete("/api/buildings/" + id).with(asUser("owner-1", "owner1@example.com")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/buildings/" + id).with(asUser("owner-1", "owner1@example.com")))
                .andExpect(status().isNotFound());
    }

    /**
     * A first request that only reads runs in a read only transaction, where a
     * lazily created user record would never be flushed. The caller must still
     * end up with a local record.
     */
    @Test
    void aNewUserIsGivenALocalRecordOnAReadOnlyFirstRequest() throws Exception {
        mockMvc.perform(get("/api/buildings").with(asUser("fresh", "fresh@example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me").with(asUser("fresh", "fresh@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("fresh@example.com"));

        // Creating works straight away, which needs that record to exist.
        mockMvc.perform(post("/api/buildings")
                        .with(asUser("fresh", "fresh@example.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsABuildingWithoutAName() throws Exception {
        mockMvc.perform(post("/api/buildings")
                        .with(asUser("owner-1", "owner1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void oneOwnerCannotSeeAnotherOwnersBuildings() throws Exception {
        String created = mockMvc.perform(post("/api/buildings")
                        .with(asUser("owner-1", "owner1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/buildings/" + id).with(asUser("owner-2", "owner2@example.com")))
                .andExpect(status().isNotFound());

        String list = mockMvc.perform(get("/api/buildings").with(asUser("owner-2", "owner2@example.com")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<?>>read(list, "$")).isEmpty();
    }
}
