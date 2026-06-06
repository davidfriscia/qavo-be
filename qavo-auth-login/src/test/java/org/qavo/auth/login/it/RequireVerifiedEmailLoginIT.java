/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.qavo.security.local.domain.QavoRole;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoRoleRepository;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.qavo.test.AbstractPostgresIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the email-verified login guard introduced in 0.0.2-SNAPSHOT: when
 * {@code qavo.auth.registration.email-verification.require-verified-email-to-login=true}, the
 * login endpoint must reject credential exchange for a user whose {@code emailVerified=false}
 * with a 403 RFC 9457 problem of type {@code email-not-verified}. Once the user is flipped to
 * verified, the same credentials authenticate successfully and a token pair is issued.
 */
@SpringBootTest(classes = TokenIssuanceTestApplication.class)
@AutoConfigureMockMvc
class RequireVerifiedEmailLoginIT extends AbstractPostgresIntegrationTest {

    private static final String USERNAME = "diana";
    private static final String PASSWORD = "correct horse battery staple";

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("qavo.security.local.jwt.secret",
                () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("qavo.auth.registration.email-verification.require-verified-email-to-login",
                () -> "true");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private QavoUserRepository users;
    @Autowired private QavoRoleRepository roles;
    @Autowired private PasswordEncoder passwordEncoder;

    private QavoUser seedUnverified() {
        // Use a deterministic username; rebuild fresh each test so prior runs don't leak state.
        users.findByUsername(USERNAME).ifPresent(users::delete);
        QavoUser user = new QavoUser(UUID.randomUUID(), USERNAME, USERNAME + "@example.com",
                passwordEncoder.encode(PASSWORD));
        QavoRole role = roles.findById("USER")
                .orElseGet(() -> roles.save(new QavoRole("USER", Set.of("user:read"))));
        user.getRoles().add(role);
        user.setEmailVerified(false);
        return users.save(user);
    }

    @Test
    void unverifiedEmailIsRejectedWithEmailNotVerifiedProblem() throws Exception {
        seedUnverified();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("email-not-verified"));
    }

    @Test
    void verifiedEmailAuthenticatesNormally() throws Exception {
        QavoUser user = seedUnverified();
        user.setEmailVerified(true);
        users.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void wrongPasswordStillReturnsUnauthorizedRegardlessOfVerificationFlag() throws Exception {
        seedUnverified();

        // The verification guard MUST run only AFTER credential validation passes — a wrong
        // password must not leak whether the account exists or whether it is verified.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(USERNAME, "definitely-wrong")))
                .andExpect(status().isUnauthorized());
    }
}
