package ai.content.auto.mapper;

import ai.content.auto.entity.User;
import ai.content.auto.dtos.UserDto;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
  User toEntity(UserDto userDto);

  UserDto toDto(User user);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  User partialUpdate(UserDto userDto, @MappingTarget User user);
}
