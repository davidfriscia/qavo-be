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
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Exercises the brute-force lockout policy end-to-end: repeated bad-password attempts up to the
 * configured threshold cause the account to lock and respond with HTTP 423 carrying an
 * {@code unlocksAt} extension property; once the lock window expires the same credentials
 * succeed again without administrator intervention.
 */
@SpringBootTest(classes = TokenIssuanceTestApplication.class)
@AutoConfigureMockMvc
@Import(LockoutFlowIT.TestClockConfig.class)
class LockoutFlowIT extends AbstractPostgresIntegrationTest {

    private static final String USERNAME = "carol";
    private static final String PASSWORD = "correct horse battery staple";

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("qavo.security.local.jwt.secret",
                () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("qavo.security.local.lockout.enabled", () -> "true");
        registry.add("qavo.security.local.lockout.max-attempts", () -> "3");
        registry.add("qavo.security.local.lockout.duration", () -> "PT5M");
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
    void repeatedFailuresLockTheAccountAndSucceedAfterTheWindowExpires() throws Exception {
        seedUser();

        // 1) Two failed attempts — still unauthorized, not locked.
        attemptLogin("wrong-1").andExpect(status().isUnauthorized());
        attemptLogin("wrong-2").andExpect(status().isUnauthorized());

        // 2) Third failure crosses the threshold (max-attempts=3) and the account is now locked.
        attemptLogin("wrong-3").andExpect(status().isUnauthorized());

        // 3) The next attempt — even with the correct password — must be rejected with 423 and
        //    expose unlocksAt so the client can render a meaningful retry-after countdown.
        String body = attemptLogin(PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("account-locked"))
                .andExpect(jsonPath("$.unlocksAt").exists())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).get("unlocksAt").asText())
                .as("unlocksAt must be a parseable ISO-8601 instant in the future")
                .satisfies(value -> assertThat(Instant.parse(value)).isAfter(clock.instant()));

        // 4) Advance past the lock window — the same credentials now authenticate, proving the
        //    lock is self-clearing and the counter was reset on success.
        clock.advance(Duration.ofMinutes(6));
        attemptLogin(PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    private ResultActions attemptLogin(String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(USERNAME, password)));
    }

    /** Replaces the platform clocks so lock expiry can be triggered by advancing in-memory time. */
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
