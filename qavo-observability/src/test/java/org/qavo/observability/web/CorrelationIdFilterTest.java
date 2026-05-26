/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.observability.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.qavo.core.observability.MdcKeys;
import org.qavo.core.security.AnonymousSecurityContextAccessor;
import org.qavo.observability.config.QavoObservabilityProperties;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final QavoObservabilityProperties properties = new QavoObservabilityProperties();
    private final CorrelationIdFilter filter =
            new CorrelationIdFilter(properties, new AnonymousSecurityContextAccessor());

    @Test
    void reusesTraceIdFromTraceparentHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String traceId = "0af7651916cd43dd8448eb211c80319c";
        request.addHeader("traceparent", "00-" + traceId + "-b7ad6b7169203331-01");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var capturingChain = new MockFilterChain() {
            String observedTraceId;

            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                observedTraceId = MDC.get(MdcKeys.TRACE_ID);
            }
        };

        filter.doFilter(request, response, capturingChain);

        assertThat(capturingChain.observedTraceId).isEqualTo(traceId);
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo(traceId);
        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
    }

    @Test
    void generatesTraceIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Trace-Id")).isNotBlank();
    }
}
