package ai.content.auto.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Debug filter to log CORS-related headers and requests
 * Only active when DEBUG logging is enabled for this class
 */
@Slf4j
@Component
@Order(1)
public class CorsDebugFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {

            // Log CORS-related information only for API requests
            String requestURI = httpRequest.getRequestURI();
            if (requestURI.startsWith("/api/")) {
                String method = httpRequest.getMethod();
                String origin = httpRequest.getHeader("Origin");
                String referer = httpRequest.getHeader("Referer");

                log.debug("CORS Debug - Method: {}, URI: {}, Origin: {}, Referer: {}",
                        method, requestURI, origin, referer);

                // Log all CORS-related request headers
                if (log.isDebugEnabled()) {
                    log.debug("Request Headers:");
                    log.debug("  Origin: {}", httpRequest.getHeader("Origin"));
                    log.debug("  Access-Control-Request-Method: {}",
                            httpRequest.getHeader("Access-Control-Request-Method"));
                    log.debug("  Access-Control-Request-Headers: {}",
                            httpRequest.getHeader("Access-Control-Request-Headers"));
                    log.debug("  Authorization: {}",
                            httpRequest.getHeader("Authorization") != null ? "Present" : "Not present");
                }
            }
        }

        chain.doFilter(request, response);

        // Log response headers for CORS debugging
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String requestURI = httpRequest.getRequestURI();
            if (requestURI.startsWith("/api/") && log.isDebugEnabled()) {
                log.debug("Response Headers:");
                log.debug("  Access-Control-Allow-Origin: {}", httpResponse.getHeader("Access-Control-Allow-Origin"));
                log.debug("  Access-Control-Allow-Credentials: {}",
                        httpResponse.getHeader("Access-Control-Allow-Credentials"));
                log.debug("  Access-Control-Allow-Methods: {}", httpResponse.getHeader("Access-Control-Allow-Methods"));
                log.debug("  Access-Control-Allow-Headers: {}", httpResponse.getHeader("Access-Control-Allow-Headers"));
            }
        }
    }
}