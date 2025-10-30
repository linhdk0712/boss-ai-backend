package ai.content.auto.config;

import ai.content.auto.entity.User;
import ai.content.auto.entity.UserRole;
import ai.content.auto.entity.UserRoleId;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Initialize demo data for development and testing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDemoUsers();
    }

    private void initializeDemoUsers() {
        // Create admin user if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            createUser("admin", "admin123", "admin@bossai.com.vn", "Admin", "User", "ADMIN");
            log.info("Created demo admin user: admin/admin123");
        }

        // Create regular user if not exists
        if (userRepository.findByUsername("user").isEmpty()) {
            createUser("user", "user123", "user@bossai.com.vn", "Demo", "User", "USER");
            log.info("Created demo user: user/user123");
        }
    }

    private void createUser(String username, String password, String email, String firstName, String lastName,
            String role) {
        // Create user entity
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        user.setTwoFactorEnabled(false);
        user.setLanguage("vi");

        // Add sample avatar for demo
        if ("admin".equals(username)) {
            user.setProfilePictureUrl("https://cdn.vuetifyjs.com/images/john.jpg");
        } else if ("user".equals(username)) {
            user.setProfilePictureUrl("https://cdn.vuetifyjs.com/images/lists/1.jpg");
        }

        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        // Save user
        User savedUser = userRepository.save(user);

        // Create user role
        UserRole userRole = new UserRole();
        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(savedUser.getId());
        userRoleId.setRole(role);
        userRole.setId(userRoleId);
        userRole.setUser(savedUser);
        userRoleRepository.save(userRole);

        log.info("Created user: {} with role: {}", username, role);
    }
}