package ai.content.auto.util;

import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility class for security-related operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtil {

    private final UserRepository userRepository;

    /**
     * Get the current authenticated user's ID from SecurityContextHolder
     * 
     * @return Current user's ID
     * @throws BusinessException if user is not authenticated or not found
     */
    public Long getCurrentUserId() {
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

            log.debug("Current authenticated user ID: {} (username: {})", user.getId(), username);
            return user.getId();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting current user ID", e);
            throw new BusinessException("Failed to get current user information");
        }
    }

    /**
     * Get the current authenticated user entity from SecurityContextHolder
     * 
     * @return Current user entity
     * @throws BusinessException if user is not authenticated or not found
     */
    public User getCurrentUser() {
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