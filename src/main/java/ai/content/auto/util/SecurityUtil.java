package ai.content.auto.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for security-related operations
 * Uses request-scoped caching to avoid redundant database queries within a
 * single request
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtil {

    private final UserRepository userRepository;

    // Request-scoped cache to avoid multiple database queries for the same user in
    // a single request
    private static final ThreadLocal<User> CURRENT_USER_CACHE = new ThreadLocal<>();

    /**
     * Get the current authenticated user's ID from SecurityContextHolder
     * Uses cached user from request scope if available to avoid redundant database
     * queries
     * 
     * @return Current user's ID
     * @throws BusinessException if user is not authenticated or not found
     */
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user.getId();
    }

    /**
     * Get the current authenticated user entity from SecurityContextHolder
     * Uses request-scoped cache to avoid redundant database queries within a single
     * request
     * 
     * @return Current user entity
     * @throws BusinessException if user is not authenticated or not found
     */
    public User getCurrentUser() {
        // Check cache first
        User cachedUser = CURRENT_USER_CACHE.get();
        if (cachedUser != null) {
            log.debug("Returning cached user: {} (ID: {})", cachedUser.getUsername(), cachedUser.getId());
            return cachedUser;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new BusinessException("User is not authenticated");
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof UserDetails)) {
                throw new BusinessException("Invalid authentication principal");
            }

            UserDetails userDetails = (UserDetails) principal;
            String username = userDetails.getUsername();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException("User not found: " + username));

            // Cache the user for this request
            CURRENT_USER_CACHE.set(user);

            log.debug("Current authenticated user: {} (ID: {})", username, user.getId());
            return user;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting current user", e);
            throw new BusinessException("Failed to get current user information");
        }
    }

    /**
     * Clear the request-scoped user cache
     * Should be called at the end of request processing (e.g., in a filter or
     * interceptor)
     * to prevent memory leaks
     */
    public void clearUserCache() {
        CURRENT_USER_CACHE.remove();
    }

    /**
     * Get the current authenticated username from SecurityContextHolder
     * 
     * @return Current username
     * @throws BusinessException if user is not authenticated
     */
    public String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new BusinessException("User is not authenticated");
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof UserDetails)) {
                throw new BusinessException("Invalid authentication principal");
            }

            UserDetails userDetails = (UserDetails) principal;
            return userDetails.getUsername();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting current username", e);
            throw new BusinessException("Failed to get current user information");
        }
    }

    /**
     * Check if the current authenticated user has admin role
     * 
     * @return true if user has ADMIN role, false otherwise
     * @throws BusinessException if user is not authenticated
     */
    public boolean isCurrentUserAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new BusinessException("User is not authenticated");
            }

            // Check if user has ADMIN role from authorities
            boolean hasAdminRole = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

            log.debug("Current user admin status: {}", hasAdminRole);
            return hasAdminRole;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking admin status", e);
            throw new BusinessException("Failed to check user admin status");
        }
    }

    /**
     * Check if the current authenticated user has a specific role
     * 
     * @param role Role to check (without ROLE_ prefix, e.g., "ADMIN", "USER")
     * @return true if user has the specified role, false otherwise
     * @throws BusinessException if user is not authenticated
     */
    public boolean hasRole(String role) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new BusinessException("User is not authenticated");
            }

            String roleWithPrefix = "ROLE_" + role.toUpperCase();
            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));

            log.debug("Current user has role {}: {}", roleWithPrefix, hasRole);
            return hasRole;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking role: {}", role, e);
            throw new BusinessException("Failed to check user role");
        }
    }
}