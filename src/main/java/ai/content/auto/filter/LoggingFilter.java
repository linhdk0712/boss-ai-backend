package ai.content.auto.filter;

import ai.content.auto.util.LoggingUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to add correlation ID and request logging
 * This filter runs early in the request processing chain
 */
@Slf4j
@Component
@Order(1)
public class LoggingFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        try {
            // Generate or use existing correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = LoggingUtil.generateCorrelationId();
            } else {
                LoggingUtil.generateCorrelationId(); // This sets the MDC
            }

            // Add correlation ID to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Log request start
            String method = httpRequest.getMethod();
            String uri = httpRequest.getRequestURI();
            String queryString = httpRequest.getQueryString();
            String fullUrl = queryString != null ? uri + "?" + queryString : uri;

            log.info("Request started: {} {}", method, fullUrl);

            // Continue with the request
            chain.doFilter(request, response);

            // Log request completion
            long executionTime = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            if (status >= 400) {
                log.warn("Request completed: {} {} - Status: {} - Time: {}ms",
                        method, fullUrl, status, executionTime);
            } else {
                log.info("Request completed: {} {} - Status: {} - Time: {}ms",
                        method, fullUrl, status, executionTime);
            }

            // Log performance warning for slow requests
            LoggingUtil.logPerformanceWarning("HTTP Request: " + method + " " + uri, executionTime, 2000);

        } finally {
            // Clear MDC context after request
            LoggingUtil.clearContext();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("LoggingFilter initialized");
    }

    @Override
    public void destroy() {
        log.info("LoggingFilter destroyed");
    }
}