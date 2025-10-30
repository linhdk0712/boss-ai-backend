package ai.content.auto.mapper;

import ai.content.auto.entity.UserRole;
import ai.content.auto.dtos.UserRoleDto;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {UserMapper.class})
public interface UserRoleMapper {
  UserRole toEntity(UserRoleDto userRoleDto);

  UserRoleDto toDto(UserRole userRole);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  UserRole partialUpdate(UserRoleDto userRoleDto, @MappingTarget UserRole userRole);
}
