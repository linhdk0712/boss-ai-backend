package ai.content.auto.mapper;

import ai.content.auto.dtos.CreatePresetRequest;
import ai.content.auto.dtos.UpdatePresetRequest;
import ai.content.auto.dtos.UserPresetDto;
import ai.content.auto.entity.User;
import ai.content.auto.entity.UserPreset;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;

@Component
public class UserPresetMapper {

    public UserPresetDto toDto(UserPreset entity) {
        if (entity == null) {
            return null;
        }

        UserPresetDto dto = new UserPresetDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setConfiguration(entity.getConfiguration());
        dto.setCategory(entity.getCategory());
        dto.setContentType(entity.getContentType());
        dto.setIsDefault(entity.getIsDefault());
        dto.setIsFavorite(entity.getIsFavorite());
        dto.setUsageCount(entity.getUsageCount());
        dto.setLastUsedAt(entity.getLastUsedAt());
        dto.setIsShared(entity.getIsShared());
        dto.setSharedWithWorkspace(entity.getSharedWithWorkspace());
        dto.setWorkspaceId(entity.getWorkspaceId());
        dto.setTags(entity.getTags());
        dto.setAverageGenerationTimeMs(entity.getAverageGenerationTimeMs());
        dto.setAverageQualityScore(entity.getAverageQualityScore());
        dto.setSuccessRate(entity.getSuccessRate());
        dto.setTotalUses(entity.getTotalUses());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setVersion(entity.getVersion());

        // Map user information
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUsername(entity.getUser().getUsername());
        }

        return dto;
    }

    public UserPreset toEntity(CreatePresetRequest request, User user) {
        if (request == null) {
            return null;
        }

        UserPreset entity = new UserPreset();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setConfiguration(request.getConfiguration());
        entity.setCategory(request.getCategory());
        entity.setContentType(request.getContentType());
        entity.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        entity.setIsFavorite(request.getIsFavorite() != null ? request.getIsFavorite() : false);
        entity.setIsShared(request.getIsShared() != null ? request.getIsShared() : false);
        entity.setSharedWithWorkspace(
                request.getSharedWithWorkspace() != null ? request.getSharedWithWorkspace() : false);
        entity.setWorkspaceId(request.getWorkspaceId());
        entity.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());

        // Set default values
        entity.setUsageCount(0);
        entity.setTotalUses(0);
        entity.setUser(user);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setVersion(0L);

        return entity;
    }

    public void updateEntityFromRequest(UserPreset entity, UpdatePresetRequest request) {
        if (request == null || entity == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getConfiguration() != null) {
            entity.setConfiguration(request.getConfiguration());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getContentType() != null) {
            entity.setContentType(request.getContentType());
        }
        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }
        if (request.getIsFavorite() != null) {
            entity.setIsFavorite(request.getIsFavorite());
        }
        if (request.getIsShared() != null) {
            entity.setIsShared(request.getIsShared());
        }
        if (request.getSharedWithWorkspace() != null) {
            entity.setSharedWithWorkspace(request.getSharedWithWorkspace());
        }
        if (request.getWorkspaceId() != null) {
            entity.setWorkspaceId(request.getWorkspaceId());
        }
        if (request.getTags() != null) {
            entity.setTags(request.getTags());
        }

        entity.setUpdatedAt(OffsetDateTime.now());
    }
}