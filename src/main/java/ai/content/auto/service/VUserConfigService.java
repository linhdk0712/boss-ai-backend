package ai.content.auto.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.content.auto.dtos.VUserConfigDto;
import ai.content.auto.entity.ConfigsPrimary;
import ai.content.auto.entity.ConfigsUser;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.VUserConfigMapper;
import ai.content.auto.repository.ConfigsPrimaryRepository;
import ai.content.auto.repository.ConfigsUserRepository;
import ai.content.auto.repository.VUserConfigRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VUserConfigService {
    private final VUserConfigRepository vUserConfigRepository;
    private final ConfigsUserRepository configsUserRepository;
    private final ConfigsPrimaryRepository configsPrimaryRepository;
    private final VUserConfigMapper vUserConfigMapper;
    private final SecurityUtil securityUtil;

    // ✅ CORRECT: No @Transactional on main method
    public List<VUserConfigDto> findAllByCategory(String category) {
        try {
            // 1. Validate input
            if (category == null || category.trim().isEmpty()) {
                log.warn("Invalid category provided: {}", category);
                throw new BusinessException("Category is required");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            if (userId == null) {
                log.warn("No authenticated user found");
                throw new BusinessException("User authentication required");
            }

            log.info("Fetching settings for category: {} and user: {}", category, userId);

            // 3. Database query
            List<VUserConfigDto> result = findAllByCategoryFromDb(userId, category);

            log.info("Found {} settings for category: {} and user: {}", result.size(), category, userId);
            return result;

        } catch (BusinessException e) {
            throw e; // Re-throw business exceptions as-is
        } catch (Exception e) {
            log.error("Unexpected error fetching settings for category: {} and user: {}", category,
                    securityUtil.getCurrentUserId(), e);
            throw new BusinessException("Failed to fetch settings");
        }
    }

    // ✅ CORRECT: Database operations without explicit transaction (let Spring Data
    // handle it)
    private List<VUserConfigDto> findAllByCategoryFromDb(Long userId, String category) {
        return vUserConfigRepository.findAllByCategory(userId, category)
                .stream()
                .map(vUserConfigMapper::toDto)
                .toList();
    }

    @Transactional
    public void updateConfig(VUserConfigDto vUserConfigDto) {
        try {
            // 1. Validate input
            if (vUserConfigDto == null) {
                log.error("VUserConfigDto is null");
                throw new BusinessException("Configuration data is required");
            }

            if (vUserConfigDto.id() == null) {
                log.error("Config ID is null");
                throw new BusinessException("Configuration ID is required");
            }

            // 2. Get current user
            User currentUser = securityUtil.getCurrentUser();
            if (currentUser == null) {
                log.error("No authenticated user found");
                throw new BusinessException("User authentication required");
            }

            // 3. Find config primary
            ConfigsPrimary configsPrimary = findConfigsPrimary(vUserConfigDto.id());

            Boolean isSelected = vUserConfigDto.isSelected();
            log.info("Updating config {} for user {} - isSelected: {}",
                    vUserConfigDto.id(), currentUser.getId(), isSelected);

            // 4. Update configuration
            updateUserConfigSelection(currentUser, configsPrimary, isSelected);

            log.info("Successfully updated config {} for user {}",
                    vUserConfigDto.id(), currentUser.getId());

        } catch (BusinessException e) {
            throw e; // Re-throw business exceptions as-is
        } catch (Exception e) {
            log.error("Unexpected error updating config for user: {}",
                    securityUtil.getCurrentUserId(), e);
            throw new BusinessException("Failed to update configuration");
        }
    }

    // ✅ CORRECT: Database operations with try-catch to handle any transaction
    // issues
    private ConfigsPrimary findConfigsPrimary(Long configId) {
        try {
            return configsPrimaryRepository.findById(configId)
                    .orElseThrow(() -> {
                        log.error("ConfigsPrimary not found with id: {}", configId);
                        return new BusinessException("Configuration not found: " + configId);
                    });
        } catch (Exception e) {
            log.error("Database error finding config primary with id: {}", configId, e);
            throw new BusinessException("Failed to find configuration: " + configId);
        }
    }

    // ✅ CORRECT: Database operations with try-catch to handle any transaction
    // issues
    private void updateUserConfigSelection(User currentUser, ConfigsPrimary configsPrimary,
            Boolean isSelected) {
        try {
            if (Boolean.TRUE.equals(isSelected)) {
                // Check if already exists
                ConfigsUser existingConfig = configsUserRepository
                        .findConfigsUserByUserAndConfigsPrimary(currentUser, configsPrimary);

                if (existingConfig == null) {
                    ConfigsUser configsUser = new ConfigsUser();
                    configsUser.setConfigsPrimary(configsPrimary);
                    configsUser.setUser(currentUser);
                    Instant now = Instant.now();
                    configsUser.setCreatedAt(now);
                    configsUser.setUpdatedAt(now);
                    configsUserRepository.save(configsUser);
                    log.debug("Created new user config selection");
                } else {
                    log.debug("User config selection already exists");
                }
            } else {
                // Remove selection
                ConfigsUser configsUser = configsUserRepository
                        .findConfigsUserByUserAndConfigsPrimary(currentUser, configsPrimary);

                if (configsUser != null) {
                    configsUserRepository.delete(configsUser);
                    log.debug("Removed user config selection");
                } else {
                    log.debug("User config selection not found to remove");
                }
            }
        } catch (Exception e) {
            log.error("Database error updating user config selection for user: {} and config: {}",
                    currentUser.getId(), configsPrimary.getId(), e);
            throw new BusinessException("Failed to update configuration selection");
        }
    }
}
