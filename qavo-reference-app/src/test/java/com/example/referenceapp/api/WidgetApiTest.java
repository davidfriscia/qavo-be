/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package com.example.referenceapp.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.qavo.test.ProblemDetailAssertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for the widget API, verifying that the platform's cross-cutting concerns apply to
 * application endpoints: standardized validation errors (RFC 9457) and method-level authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WidgetApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "user:write")
    void rejectsInvalidSlugWithProblemDetail() throws Exception {
        String body = """
                {"code":"Not A Slug","name":"Example","description":"x"}
                """;

        ProblemDetailAssertions.assertProblemDetail(
                mockMvc.perform(post("/api/v1/widgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)),
                400, "validation");
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void forbidsCreateWithoutWritePermission() throws Exception {
        String body = """
                {"code":"valid-slug","name":"Example","description":"x"}
                """;

        mockMvc.perform(post("/api/v1/widgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
