/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.qavo.core.observability.TraceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ProblemDetailFactoryTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-05-26T10:15:30Z"), ZoneOffset.UTC);

    private final ProblemDetailFactory factory = new ProblemDetailFactory(
            URI.create("https://errors.example.org"),
            new StubTraceContext("trace-123"),
            FIXED);

    @Test
    void buildsTypeUriFromBaseAndCode() {
        ProblemDetail problem = factory.create(CoreProblemType.RESOURCE_NOT_FOUND, "missing");

        assertThat(problem.getType()).isEqualTo(URI.create("https://errors.example.org/resource-not-found"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource not found");
    }

    @Test
    void populatesStandardExtensions() {
        ProblemDetail problem = factory.create(
                CoreProblemType.VALIDATION,
                "Validation error",
                "One or more fields are invalid",
                List.of(FieldErrorDetail.of("email", "Invalid email format", "Email")));

        assertThat(problem.getProperties()).containsKeys(
                ProblemDetailFactory.TIMESTAMP,
                ProblemDetailFactory.TRACE_ID,
                ProblemDetailFactory.CODE,
                ProblemDetailFactory.ERRORS);
        assertThat(problem.getProperties().get(ProblemDetailFactory.TRACE_ID)).isEqualTo("trace-123");
        assertThat(problem.getProperties().get(ProblemDetailFactory.CODE)).isEqualTo("validation");
        assertThat(problem.getProperties().get(ProblemDetailFactory.TIMESTAMP))
                .isEqualTo(Instant.parse("2026-05-26T10:15:30Z"));
    }

    @Test
    void omitsErrorsArrayWhenEmpty() {
        ProblemDetail problem = factory.create(CoreProblemType.INTERNAL_ERROR, "boom");

        assertThat(problem.getProperties()).doesNotContainKey(ProblemDetailFactory.ERRORS);
    }

    private record StubTraceContext(String traceId) implements TraceContext {
        @Override
        public String currentTraceId() {
            return traceId;
        }

        @Override
        public String currentSpanId() {
            return null;
        }
    }
}
