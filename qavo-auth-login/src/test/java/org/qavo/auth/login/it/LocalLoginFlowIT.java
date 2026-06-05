/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.qavo.security.local.domain.QavoRole;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoRoleRepository;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.qavo.test.AbstractPostgresIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end exercise of the local JWT login flow: credential authentication issues a signed
 * access token and a rotating opaque refresh token; the refresh endpoint mints a new pair and
 * invalidates the presented one; logout revokes all of a user's refresh tokens; and refresh
 * tokens stop working once they outlive {@code qavo.security.local.jwt.refresh-token-duration}.
 *
 * <p>Time is controlled through an injected {@link MutableClock}, so expiry is verified
 * deterministically rather than by sleeping.
 */
@SpringBootTest(classes = TokenIssuanceTestApplication.class)
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(LocalLoginFlowIT.TestClockConfig.class)
class LocalLoginFlowIT extends AbstractPostgresIntegrationTest {

    private static final String USERNAME = "alice";
    private static final String PASSWORD = "correct horse battery staple";

    @DynamicPropertySource
    static void registerJwtSecret(DynamicPropertyRegistry registry) {
        // 32-byte secret (zeroes), Base64 encoded — sufficient for HS256 in tests only.
        registry.add("qavo.security.local.jwt.secret",
                () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        // Tighten the refresh-token lifetime so the expiry test advances the clock by a small
        // amount; the issuance window stays comfortably above the access-token lifetime so the
        // refresh succeeds before expiry.
        registry.add("qavo.security.local.jwt.refresh-token-duration", () -> "PT5M");
        registry.add("qavo.security.local.jwt.access-token-duration", () -> "PT1M");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private QavoUserRepository users;
    @Autowired private QavoRoleRepository roles;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MutableClock clock;

    private void seedUser() {
        if (users.findByUsername(USERNAME).isPresent()) {
            return;
        }
        QavoUser user = new QavoUser(UUID.randomUUID(), USERNAME, USERNAME + "@example.com",
                passwordEncoder.encode(PASSWORD));
        QavoRole userRole = roles.findById("USER")
                .orElseGet(() -> roles.save(new QavoRole("USER", Set.of("user:read"))));
        user.getRoles().add(userRole);
        users.save(user);
    }

    @Test
    void loginIssuesNonEmptyAccessAndRefreshTokens() throws Exception {
        seedUser();

        JsonNode body = login();

        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("refreshToken").asText()).isNotBlank();
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("expiresInSeconds").asLong()).isPositive();
        assertThat(body.get("user").get("username").asText()).isEqualTo(USERNAME);
    }

    @Test
    void refreshRotatesTokensAndOldRefreshNoLongerWorks() throws Exception {
        seedUser();
        JsonNode initial = login();
        String oldRefresh = initial.get("refreshToken").asText();

        JsonNode rotated = refresh(oldRefresh).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().transform(this::parse);

        assertThat(rotated.get("refreshToken").asText())
                .isNotBlank()
                .isNotEqualTo(oldRefresh);
        assertThat(rotated.get("accessToken").asText())
                .isNotBlank()
                .isNotEqualTo(initial.get("accessToken").asText());

        // The original refresh token is now revoked; presenting it again must be rejected.
        refresh(oldRefresh).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        seedUser();
        String refreshToken = login().get("refreshToken").asText();

        // Move past the configured 5-minute refresh window.
        clock.advance(Duration.ofMinutes(6));

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesAllRefreshTokensForTheUser() throws Exception {
        seedUser();
        JsonNode session = login();
        String accessToken = session.get("accessToken").asText();
        String refreshToken = session.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsIssuedBearerToken() throws Exception {
        seedUser();
        String accessToken = login().get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    // --- helpers -----------------------------------------------------------------------------

    private JsonNode login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        return parse(result.getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)));
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON: " + json, e);
        }
    }

    /**
     * Replaces the default UTC system clock with a {@link MutableClock} so tests can fast-forward
     * past refresh-token expiry without sleeping. {@code @Primary} ensures it wins over the
     * plugin's default-only clock bean.
     */
    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        }

        @Bean
        @Primary
        Clock clockAlias(MutableClock mutable) {
            return mutable;
        }
    }
}
