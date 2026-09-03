package com.bms.support;

import com.bms.identity.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Boots the full application against a real Postgres, so the Flyway migrations
 * and the JPA mappings are validated on every run.
 *
 * <p>The container is a singleton started once per JVM rather than a JUnit
 * managed {@code @Container}: Spring caches the application context across test
 * classes, so a container torn down after the first class would leave later
 * classes pointing at a dead database. Ryuk removes it when the JVM exits.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    static {
        POSTGRES.start();
    }

    /** No Keycloak in tests: authentication is supplied directly by the jwt() post processor. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    /** Account creation is stubbed; the tests care about what we do with the result. */
    @MockitoBean
    protected KeycloakAdminClient keycloak;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestData testData;

    @BeforeEach
    void resetDatabase() {
        testData.deleteAll();
    }
}
