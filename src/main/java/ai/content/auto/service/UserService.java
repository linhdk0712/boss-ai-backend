package ai.content.auto.service;

import ai.content.auto.entity.User;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Get user roles from user_roles table
        List<SimpleGrantedAuthority> authorities = userRoleRepository.findByIdUserId(user.getId())
                .stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getId().getRole()))
                .collect(Collectors.toList());

        // If no roles found, assign default USER role
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        log.debug("User {} has authorities: {}", username, authorities);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getAccountLockedUntil() != null &&
                        user.getAccountLockedUntil().isAfter(java.time.Instant.now()))
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }
}
