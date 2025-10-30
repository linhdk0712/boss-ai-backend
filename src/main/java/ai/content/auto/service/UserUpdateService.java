package ai.content.auto.service;

import ai.content.auto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserRepository userRepository;

    @Transactional
    public void resetFailedLoginAttempts(Long userId) {
        try {
            int updated = userRepository.resetFailedLoginAttempts(userId);
            if (updated > 0) {
                log.info("Reset failed login attempts for user ID: {}", userId);
            } else {
                log.warn("No user found to reset login attempts for ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to reset login attempts for user ID: {}", userId, e);
            throw e; // Let it propagate but it's caught in AuthService
        }
    }

    @Transactional
    public void incrementFailedLoginAttempts(String username, int maxAttempts, int lockDurationMinutes) {
        try {
            int updated = userRepository.incrementFailedLoginAttempts(username, maxAttempts, lockDurationMinutes);
            if (updated > 0) {
                log.info("Incremented failed login attempts for username: {}", username);
            } else {
                log.warn("No user found to increment failed attempts for: {}", username);
            }
        } catch (Exception e) {
            log.error("Failed to increment failed login attempts for username: {}", username, e);
            throw e; // Let it propagate but it's caught in AuthService
        }
    }
}