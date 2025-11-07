package ai.content.auto.mapper;

import ai.content.auto.dtos.TemplateUsageLogDto;
import ai.content.auto.entity.TemplateUsageLog;
import org.springframework.stereotype.Component;

@Component
public class TemplateUsageLogMapper {

    public TemplateUsageLogDto toDto(TemplateUsageLog entity) {
        if (entity == null) {
            return null;
        }

        TemplateUsageLogDto dto = new TemplateUsageLogDto();
        dto.setId(entity.getId());
        dto.setUsageType(entity.getUsageType());
        dto.setUsageSource(entity.getUsageSource());
        dto.setGenerationTimeMs(entity.getGenerationTimeMs());
        dto.setTokensUsed(entity.getTokensUsed());
        dto.setGenerationCost(entity.getGenerationCost());
        dto.setQualityScore(entity.getQualityScore());
        dto.setUserRating(entity.getUserRating());
        dto.setUserFeedback(entity.getUserFeedback());
        dto.setWasSuccessful(entity.getWasSuccessful());
        dto.setParametersUsed(entity.getParametersUsed());
        dto.setCustomizationsMade(entity.getCustomizationsMade());
        dto.setFieldsModified(entity.getFieldsModified());
        dto.setSessionId(entity.getSessionId());
        dto.setUserAgent(entity.getUserAgent());
        dto.setIpAddress(entity.getIpAddress());
        dto.setCountryCode(entity.getCountryCode());
        dto.setTimezone(entity.getTimezone());
        dto.setDeviceType(entity.getDeviceType());
        dto.setUsedAt(entity.getUsedAt());
        dto.setCreatedAt(entity.getCreatedAt());

        // Map template information
        if (entity.getTemplate() != null) {
            dto.setTemplateId(entity.getTemplate().getId());
            dto.setTemplateName(entity.getTemplate().getName());
        }

        // Map user information
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUsername(entity.getUser().getUsername());
        }

        // Map content generation information
        if (entity.getContentGeneration() != null) {
            dto.setContentGenerationId(entity.getContentGeneration().getId());
        }

        return dto;
    }
}