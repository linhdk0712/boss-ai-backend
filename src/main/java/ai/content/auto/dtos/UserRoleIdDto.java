package ai.content.auto.dtos;

import ai.content.auto.entity.UserRoleId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/** DTO for {@link UserRoleId} */
public record UserRoleIdDto(@NotNull Long userId, @NotNull @Size(max = 50) String role)
    implements Serializable {}
