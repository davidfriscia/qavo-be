/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.qavo.test.AbstractPostgresIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the {@code GET /api/v1/auth/registration-status} endpoint when the cap feature is
 * disabled (the default): it must return {@code 200 OK} with {@code open=true}, set
 * {@code Cache-Control: no-store}, and require no authentication. This complements the
 * cap-enabled assertions in {@link RegistrationCapIT}.
 */
@SpringBootTest(classes = RegistrationTestApplication.class)
@AutoConfigureMockMvc
class RegistrationStatusEndpointIT extends AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("qavo.security.local.jwt.secret",
                () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        // Cap feature deliberately left unset to exercise the matchIfMissing=true default.
        registry.add("qavo.auth.registration.email-verification.enabled", () -> "false");
    }

    @Autowired private MockMvc mockMvc;

    @Test
    void statusEndpointReportsOpenWhenCapDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/auth/registration-status"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.checkedAt").exists());
    }

    @Test
    void statusEndpointIsReachableWithoutAuthentication() throws Exception {
        // No auth header — the endpoint must still respond 200.
        mockMvc.perform(get("/api/v1/auth/registration-status"))
                .andExpect(status().isOk());
    }
}
