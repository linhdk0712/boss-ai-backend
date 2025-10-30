package ai.content.auto.service;

import ai.content.auto.annotation.Loggable;
import ai.content.auto.dtos.auth.*;
import ai.content.auto.entity.User;
import ai.content.auto.entity.UserRole;
import ai.content.auto.entity.UserRoleId;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final EmailService emailService;
  private final UserUpdateService userUpdateService;

  @Value("${app.jwt.access-token-expiration:3600}")
  private long accessTokenExpiration;

  @Value("${app.jwt.refresh-token-expiration:604800}")
  private long refreshTokenExpiration;

  @Value("${app.account.max-failed-attempts:5}")
  private int maxFailedAttempts = 5;

  @Value("${app.account.lock-duration-minutes:30}")
  private int lockDurationMinutes = 30;

  @Loggable
  public AuthResponse login(LoginRequest request) {
    try {
      // Check if user exists and is not locked (no transaction needed for read)
      User user = userRepository
          .findByUsername(request.getUsername())
          .orElseThrow(() -> new BusinessException("Invalid username or password"));
      // Check if account is locked
      if (user.getAccountLockedUntil() != null
          && user.getAccountLockedUntil().isAfter(Instant.now())) {
        throw new BusinessException("Account is locked. Please try again later.");
      }

      // Authenticate user (no transaction needed)
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(), request.getPassword()));

      // Update user login info in separate transaction (async to avoid blocking
      // login)
      try {
        updateSuccessfulLogin(user.getId());
      } catch (Exception e) {
        log.error("Failed to update login timestamp for user: {}, but login will continue", user.getId(), e);
        // Don't let database update failures break the login flow
      }

      // Generate tokens
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String accessToken = jwtService.generateToken(userDetails);
      String refreshToken = jwtService.generateRefreshToken(userDetails);

      // Get user role
      String role = getUserRole(user.getId());

      return AuthResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .tokenType("Bearer")
          .expiresIn(accessTokenExpiration)
          .refreshExpiresIn(refreshTokenExpiration)
          .user(
              AuthResponse.UserInfo.builder()
                  .id(user.getId())
                  .username(user.getUsername())
                  .email(user.getEmail())
                  .firstName(user.getFirstName())
                  .lastName(user.getLastName())
                  .role(role)
                  .emailVerified(user.getEmailVerified())
                  .profilePictureUrl(user.getProfilePictureUrl())
                  .build())
          .build();

    } catch (BadCredentialsException e) {
      // Handle failed login attempt (don't let this fail the authentication error)
      try {
        handleFailedLoginAttempt(request.getUsername());
      } catch (Exception dbError) {
        log.error("Failed to record failed login attempt for: {}", request.getUsername(), dbError);
      }
      throw new BusinessException("Invalid username or password");
    } catch (LockedException e) {
      try {
        handleFailedLoginAttempt(request.getUsername());
      } catch (Exception dbError) {
        log.error("Failed to record failed login attempt for: {}", request.getUsername(), dbError);
      }
      throw new BusinessException("Account is locked. Please try again later.");
    } catch (BusinessException e) {
      // Re-throw business exceptions as-is
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error during login for username: {}", request.getUsername(), e);
      try {
        handleFailedLoginAttempt(request.getUsername());
      } catch (Exception dbError) {
        log.error("Failed to record failed login attempt for: {}", request.getUsername(), dbError);
      }
      throw new BusinessException("Invalid username or password");
    }
  }

  @Loggable
  @Transactional
  public void register(RegisterRequest request) {
    // Check if username already exists
    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
      throw new BusinessException("Username already exists");
    }

    // Check if email already exists
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new BusinessException("Email already exists");
    }

    // Create new user
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setPhoneNumber(request.getPhoneNumber());
    user.setEmailVerified(false);
    user.setIsActive(false); // User needs to activate account via email
    user.setFailedLoginAttempts(0);
    user.setTwoFactorEnabled(false);
    user.setLanguage("vi");
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());

    // Generate email verification token
    String verificationToken = UUID.randomUUID().toString();
    user.setEmailVerificationToken(verificationToken);
    user.setEmailVerificationExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

    // Save user
    User savedUser = userRepository.save(user);

    // Assign default USER role
    UserRole userRole = new UserRole();
    UserRoleId userRoleId = new UserRoleId();
    userRoleId.setUserId(savedUser.getId());
    userRoleId.setRole("USER");
    userRole.setId(userRoleId);
    userRole.setUser(savedUser);
    userRoleRepository.save(userRole);

    // Send verification email
    emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);

    log.info("User registered successfully: {}", user.getUsername());
  }

  @Loggable
  @Transactional
  public void activateUser(UserActivationRequest request) {
    User user = userRepository
        .findByEmailVerificationToken(request.getToken())
        .orElseThrow(() -> new BusinessException("Invalid or expired activation token"));

    // Check if token is expired
    if (user.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
      throw new BusinessException("Activation token has expired");
    }

    // Activate user
    user.setEmailVerified(true);
    user.setIsActive(true);
    user.setEmailVerificationToken(null);
    user.setEmailVerificationExpiresAt(null);
    user.setUpdatedAt(Instant.now());

    userRepository.save(user);

    log.info("User activated successfully: {}", user.getUsername());
  }

  @Loggable
  @Transactional(readOnly = true)
  public AuthResponse refreshToken(RefreshTokenRequest request) {
    try {
      String refreshToken = request.getRefreshToken();

      // Validate refresh token
      if (!jwtService.validateToken(refreshToken)) {
        throw new BusinessException("Invalid refresh token");
      }

      // Check if it's actually a refresh token
      if (!jwtService.isRefreshToken(refreshToken)) {
        throw new BusinessException("Invalid token type");
      }

      // Extract username from refresh token
      String username = jwtService.extractUsername(refreshToken);

      // Get user details
      User user = userRepository
          .findByUsername(username)
          .orElseThrow(() -> new BusinessException("User not found"));

      // Check if user is active
      if (!user.getIsActive()) {
        throw new BusinessException("User account is not active");
      }

      // Check if account is locked
      if (user.getAccountLockedUntil() != null
          && user.getAccountLockedUntil().isAfter(Instant.now())) {
        throw new BusinessException("Account is locked");
      }

      // Generate new tokens using username
      String newAccessToken = jwtService.generateToken(username);
      String newRefreshToken = jwtService.generateRefreshToken(username);

      // Get user role
      String role = getUserRole(user.getId());

      log.info("Tokens refreshed successfully for user: {}", username);

      return AuthResponse.builder()
          .accessToken(newAccessToken)
          .refreshToken(newRefreshToken)
          .tokenType("Bearer")
          .expiresIn(accessTokenExpiration)
          .refreshExpiresIn(refreshTokenExpiration)
          .user(
              AuthResponse.UserInfo.builder()
                  .id(user.getId())
                  .username(user.getUsername())
                  .email(user.getEmail())
                  .firstName(user.getFirstName())
                  .lastName(user.getLastName())
                  .role(role)
                  .emailVerified(user.getEmailVerified())
                  .profilePictureUrl(user.getProfilePictureUrl())
                  .build())
          .build();

    } catch (BusinessException e) {
      log.error("Business error refreshing token: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error refreshing token", e);
      throw new BusinessException("Failed to refresh token");
    }
  }

  // NO @Transactional - delegate to separate service
  protected void updateSuccessfulLogin(Long userId) {
    try {
      userUpdateService.resetFailedLoginAttempts(userId);
    } catch (Exception e) {
      log.error("Failed to update successful login for user ID: {}, but login will continue", userId, e);
      // Don't throw exception here to avoid breaking the login flow
    }
  }

  // NO @Transactional - delegate to separate service
  protected void handleFailedLoginAttempt(String username) {
    try {
      userUpdateService.incrementFailedLoginAttempts(username, maxFailedAttempts, lockDurationMinutes);
    } catch (Exception e) {
      log.error("Failed to handle failed login attempt for username: {}, but authentication error will be preserved",
          username, e);
      // Don't throw exception here to avoid masking the original authentication error
    }
  }

  private String getUserRole(Long userId) {
    return userRoleRepository.findByIdUserId(userId).stream()
        .findFirst()
        .map(userRole -> userRole.getId().getRole())
        .orElse("USER");
  }
}
