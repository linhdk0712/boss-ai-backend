package ai.content.auto.repository;

import ai.content.auto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);

    // Native SQL for atomic update - uses database's implicit transaction
    @Modifying
    @Query(value = "UPDATE users SET failed_login_attempts = 0, account_locked_until = null, last_login_at = CURRENT_TIMESTAMP WHERE id = :userId", nativeQuery = true)
    int resetFailedLoginAttempts(@Param("userId") Long userId);

    // Native SQL for atomic failed login update
    @Modifying
    @Query(value = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1, " +
            "account_locked_until = CASE WHEN (failed_login_attempts + 1) >= :maxAttempts " +
            "THEN CURRENT_TIMESTAMP + (:lockMinutes || ' minutes')::INTERVAL ELSE account_locked_until END " +
            "WHERE username = :username", nativeQuery = true)
    int incrementFailedLoginAttempts(@Param("username") String username,
            @Param("maxAttempts") int maxAttempts,
            @Param("lockMinutes") int lockMinutes);
}
