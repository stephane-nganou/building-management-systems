package com.bms.identity;

import com.bms.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registration needs no account, so one client may only sign up so often. */
@TestPropertySource(properties = "bms.registration.max-per-hour=2")
class RegistrationRateLimitIntegrationTest extends AbstractIntegrationTest {

    private void signUp(String email, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","firstName":"Nina","lastName":"Neu","password":"a-good-secret"}
                                """.formatted(email)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void theThirdSignUpWithinTheHourIsRefused() throws Exception {
        given(keycloak.createUser(anyString(), anyString(), anyString(), anyString(), eq("owner"), eq(false)))
                .willReturn("kc-one", "kc-two");

        signUp("one@example.com", 201);
        signUp("two@example.com", 201);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"three@example.com","firstName":"Nina","lastName":"Neu","password":"a-good-secret"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail")
                        .value("Too many sign ups from your connection. Please try again in an hour."));
    }
}
