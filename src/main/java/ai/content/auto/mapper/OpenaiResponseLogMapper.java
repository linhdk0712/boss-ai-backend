package ai.content.auto.mapper;

import ai.content.auto.dtos.OpenaiResponseLogDto;
import ai.content.auto.entity.OpenaiResponseLog;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {UserMapper.class})
public interface OpenaiResponseLogMapper {
  OpenaiResponseLog toEntity(OpenaiResponseLogDto openaiResponseLogDto);

  OpenaiResponseLogDto toDto(OpenaiResponseLog openaiResponseLog);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  OpenaiResponseLog partialUpdate(
      OpenaiResponseLogDto openaiResponseLogDto,
      @MappingTarget OpenaiResponseLog openaiResponseLog);
}
