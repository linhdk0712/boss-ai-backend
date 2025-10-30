package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Size(max = 50)
  @NotNull
  @Column(name = "username", nullable = false, length = 50)
  private String username;

  @Size(max = 100)
  @NotNull
  @Column(name = "email", nullable = false, length = 100)
  private String email;

  @Size(max = 255)
  @Column(name = "password_hash")
  private String passwordHash;

  @Size(max = 100)
  @Column(name = "first_name", length = 100)
  private String firstName;

  @Size(max = 100)
  @Column(name = "last_name", length = 100)
  private String lastName;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  @Size(max = 255)
  @Column(name = "email_verification_token")
  private String emailVerificationToken;

  @Column(name = "email_verification_expires_at")
  private Instant emailVerificationExpiresAt;

  @Size(max = 255)
  @Column(name = "password_reset_token")
  private String passwordResetToken;

  @Column(name = "password_reset_expires_at")
  private Instant passwordResetExpiresAt;

  @Size(max = 50)
  @Column(name = "oauth_provider", length = 50)
  private String oauthProvider;

  @Size(max = 100)
  @Column(name = "oauth_provider_id", length = 100)
  private String oauthProviderId;

  @Size(max = 500)
  @Column(name = "profile_picture_url", length = 500)
  private String profilePictureUrl;

  @Size(max = 20)
  @Column(name = "phone_number", length = 20)
  private String phoneNumber;

  @Size(max = 50)
  @Column(name = "timezone", length = 50)
  private String timezone;

  @Size(max = 10)
  @ColumnDefault("'vi'")
  @Column(name = "language", length = 10)
  private String language;

  @NotNull
  @ColumnDefault("true")
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = false;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "failed_login_attempts", nullable = false)
  private Integer failedLoginAttempts;

  @Column(name = "account_locked_until")
  private Instant accountLockedUntil;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "two_factor_enabled", nullable = false)
  private Boolean twoFactorEnabled = false;

  @Size(max = 32)
  @Column(name = "two_factor_secret", length = 32)
  private String twoFactorSecret;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "updated_at")
  private Instant updatedAt;

  @ColumnDefault("0")
  @Column(name = "version")
  private Long version;
}
