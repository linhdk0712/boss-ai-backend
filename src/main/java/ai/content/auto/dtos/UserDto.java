package ai.content.auto.dtos;

import ai.content.auto.entity.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;

/** DTO for {@link User} */
public record UserDto(
    Long id,
    @NotNull @Size(max = 50) String username,
    @NotNull @Size(max = 100) String email,
    @Size(max = 255) String passwordHash,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @NotNull Boolean emailVerified,
    @Size(max = 255) String emailVerificationToken,
    Instant emailVerificationExpiresAt,
    @Size(max = 255) String passwordResetToken,
    Instant passwordResetExpiresAt,
    @Size(max = 50) String oauthProvider,
    @Size(max = 100) String oauthProviderId,
    @Size(max = 500) String profilePictureUrl,
    @Size(max = 20) String phoneNumber,
    @Size(max = 50) String timezone,
    @Size(max = 10) String language,
    @NotNull Boolean isActive,
    Instant lastLoginAt,
    @NotNull Integer failedLoginAttempts,
    Instant accountLockedUntil,
    @NotNull Boolean twoFactorEnabled,
    @Size(max = 32) String twoFactorSecret,
    @NotNull Instant createdAt,
    Instant updatedAt,
    Long version)
    implements Serializable {}
