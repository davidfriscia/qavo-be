/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.test;

import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Reusable MockMvc assertions for the platform's RFC 9457 error contract (architecture &sect;5.2),
 * so tests across modules verify the same standard shape without duplicating matchers.
 */
public final class ProblemDetailAssertions {

    private ProblemDetailAssertions() {
    }

    /**
     * Asserts that the response is a Problem Details document with the expected status and error
     * {@code code}, and that it carries the mandatory {@code traceId} and {@code timestamp} members.
     */
    public static ResultActions assertProblemDetail(ResultActions result, int expectedStatus, String expectedCode)
            throws Exception {
        return result
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(expectedStatus))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(expectedCode))
                .andExpect(MockMvcResultMatchers.jsonPath("$.type").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.title").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").exists());
    }
}
