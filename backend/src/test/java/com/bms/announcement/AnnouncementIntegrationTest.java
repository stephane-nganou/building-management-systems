package com.bms.announcement;

import java.time.Duration;
import java.time.Instant;

import com.bms.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asAdmin;
import static com.bms.support.Jwts.asAssistant;
import static com.bms.support.Jwts.asUser;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The administrator tells everybody something for a while; everybody signed in reads it while it lasts. */
class AnnouncementIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor ADMIN = asAdmin("admin-ann", "admin-ann@example.com");
    private static final RequestPostProcessor OWNER = asUser("owner-ann", "owner-ann@example.com");
    private static final RequestPostProcessor ASSISTANT = asAssistant("assistant-ann", "assistant-ann@example.com");

    private static final Instant NOW = Instant.now();

    @Test
    void onlyAnAdministratorManagesAnnouncements() throws Exception {
        String body = request("INFO", "Hello", NOW, NOW.plus(Duration.ofHours(1)));

        mockMvc.perform(get("/api/admin/announcements").with(OWNER)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/announcements").with(OWNER)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/announcements").with(ASSISTANT)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void everyoneSignedInReadsWhatIsShowingNowAndNothingElse() throws Exception {
        create("WARNING", "Down for maintenance tonight", NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(1)));
        create("INFO", "Over", NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(1)));
        create("INFO", "Not yet", NOW.plus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(2)));

        for (RequestPostProcessor user : new RequestPostProcessor[] {OWNER, ASSISTANT, ADMIN}) {
            mockMvc.perform(get("/api/announcements").with(user))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].kind").value("WARNING"))
                    .andExpect(jsonPath("$[0].messageEn").value("Down for maintenance tonight"));
        }

        mockMvc.perform(get("/api/admin/announcements").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].messageEn").value("Not yet"));
    }

    @Test
    void nobodySignedOutReadsThem() throws Exception {
        mockMvc.perform(get("/api/announcements")).andExpect(status().isUnauthorized());
    }

    @Test
    void aBlankTranslationIsNoTranslation() throws Exception {
        String body = """
                {"kind":"INFO","messageEn":"New reports","messageFr":"  ","messageDe":" Neue Berichte ",
                 "startsAt":"%s","endsAt":"%s"}
                """.formatted(NOW, NOW.plus(Duration.ofHours(1)));

        mockMvc.perform(post("/api/admin/announcements").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageFr").doesNotExist())
                .andExpect(jsonPath("$.messageDe").value("Neue Berichte"));
    }

    @Test
    void anEditChangesWhenItWasLastUpdated() throws Exception {
        String created = create("INFO", "Update on Friday", NOW, NOW.plus(Duration.ofHours(1)));
        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");
        String firstUpdate = com.jayway.jsonpath.JsonPath.read(created, "$.updatedAt");

        mockMvc.perform(put("/api/admin/announcements/" + id).with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("WARNING", "Update moved to Saturday", NOW, NOW.plus(Duration.ofHours(2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("WARNING"))
                .andExpect(jsonPath("$.messageEn").value("Update moved to Saturday"))
                .andExpect(jsonPath("$.updatedAt").value(not(equalTo(firstUpdate))));
    }

    @Test
    void aDeletedAnnouncementIsGone() throws Exception {
        String id = com.jayway.jsonpath.JsonPath.read(
                create("INFO", "Short lived", NOW.minus(Duration.ofMinutes(1)), NOW.plus(Duration.ofHours(1))), "$.id");

        mockMvc.perform(delete("/api/admin/announcements/" + id).with(ADMIN)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/announcements").with(OWNER)).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/api/admin/announcements/" + id).with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void anAnnouncementHasToEndAfterItStarts() throws Exception {
        mockMvc.perform(post("/api/admin/announcements").with(ADMIN)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "de")
                        .contentType(MediaType.APPLICATION_JSON).content(request("INFO", "Backwards", NOW, NOW)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Eine Ankündigung muss nach ihrem Beginn enden"));
    }

    @Test
    void englishIsRequired() throws Exception {
        mockMvc.perform(post("/api/admin/announcements").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("INFO", " ", NOW, NOW.plus(Duration.ofHours(1)))))
                .andExpect(status().isBadRequest());
    }

    private String create(String kind, String message, Instant startsAt, Instant endsAt) throws Exception {
        return mockMvc.perform(post("/api/admin/announcements").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(request(kind, message, startsAt, endsAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static String request(String kind, String message, Instant startsAt, Instant endsAt) {
        return """
                {"kind":"%s","messageEn":"%s","startsAt":"%s","endsAt":"%s"}
                """.formatted(kind, message, startsAt, endsAt);
    }
}
