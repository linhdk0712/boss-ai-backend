package ai.content.auto.dtos;

import ai.content.auto.entity.UserRole;

import java.io.Serializable;

/** DTO for {@link UserRole} */
public record UserRoleDto(UserRoleIdDto id, UserDto user) implements Serializable {}
