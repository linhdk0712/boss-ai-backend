package ai.content.auto.mapper;

import ai.content.auto.dtos.ContentGenerationDto;
import ai.content.auto.entity.ContentGeneration;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {UserMapper.class})
public interface ContentGenerationMapper {
  ContentGeneration toEntity(ContentGenerationDto contentGenerationDto);

  ContentGenerationDto toDto(ContentGeneration contentGeneration);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  ContentGeneration partialUpdate(
      ContentGenerationDto contentGenerationDto,
      @MappingTarget ContentGeneration contentGeneration);
}
