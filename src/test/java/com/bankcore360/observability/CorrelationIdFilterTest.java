package com.bankcore360.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CorrelationIdFilterTest {

    @Mock
    private FilterChain filterChain;

    private final CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should generate new correlation ID when client provides none")
    void testGenerateCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            String mdcVal = MDC.get(CorrelationIdFilter.MDC_CORRELATION_KEY);
            assertNotNull(mdcVal);
            assertTrue(mdcVal.startsWith("CORR-"));
            return null;
        }).when(filterChain).doFilter(request, response);

        correlationIdFilter.doFilter(request, response, filterChain);

        String header = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(header);
        assertTrue(header.startsWith("CORR-"));
        // Cleaned up after filter execution
        assertNull(MDC.get(CorrelationIdFilter.MDC_CORRELATION_KEY));
    }

    @Test
    @DisplayName("Should preserve and propagate existing client correlation ID")
    void testPreserveClientCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ledger/transfer");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "CORR-CUSTOM-CLIENT-999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        correlationIdFilter.doFilter(request, response, filterChain);

        assertEquals("CORR-CUSTOM-CLIENT-999", response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
