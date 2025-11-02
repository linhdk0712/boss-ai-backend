package ai.content.auto.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import ai.content.auto.util.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Security Cleanup Filter
 * Cleans up request-scoped ThreadLocal caches after request processing
 * Runs after all other filters to ensure cleanup happens at the end
 * Registered via FilterRegistrationBean in WebConfig
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityCleanupFilter extends OncePerRequestFilter {

    private final SecurityUtil securityUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear the ThreadLocal cache, even if an exception occurs
            securityUtil.clearUserCache();
            log.debug("Cleared request-scoped user cache");
        }
    }
}
