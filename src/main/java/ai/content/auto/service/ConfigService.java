package ai.content.auto.service;

import ai.content.auto.dtos.ConfigsPrimaryDto;
import ai.content.auto.dtos.ConfigsUserDto;
import ai.content.auto.entity.ConfigsPrimary;
import ai.content.auto.entity.ConfigsUser;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.exception.InternalServerException;
import ai.content.auto.exception.NotFoundException;
import ai.content.auto.mapper.ConfigsPrimaryMapper;
import ai.content.auto.mapper.ConfigsUserMapper;
import ai.content.auto.repository.ConfigsPrimaryRepository;
import ai.content.auto.repository.ConfigsUserRepository;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigService {

  private final ConfigsPrimaryRepository configsPrimaryRepository;
  private final ConfigsUserRepository configsUserRepository;
  private final UserRepository userRepository;
  private final ConfigsUserMapper configsUserMapper;
  private final ConfigsPrimaryMapper configsPrimaryMapper;
  private final SecurityUtil securityUtil;

  /**
   * Get configurations by category with role-based filtering
   * - ADMIN: Returns all available configurations (ConfigsPrimary)
   * - USER: Returns only user's selected configurations (ConfigsUser ->
   * ConfigsPrimary)
   * Business Logic: Admin gets full access, regular users get filtered data
   */
  public List<ConfigsPrimaryDto> getConfigsByCategory(String category) {
    try {
      log.debug("Fetching configurations for category: {}", category);

      // Validate input
      if (category == null || category.trim().isEmpty()) {
        throw new BusinessException("Category cannot be null or empty");
      }

      // Get current user info
      Long currentUserId = securityUtil.getCurrentUserId();
      boolean isAdmin = securityUtil.isCurrentUserAdmin();
      log.debug("Current user admin status: {}", isAdmin);

      if (isAdmin) {
        log.debug("Admin user detected, returning all available configurations for category: {}", category);
        return getAllAvailableConfigsInTransaction(category);
      } else {
        log.debug("Regular user detected, returning user-selected configurations for category: {}", category);
        return getUserSelectedConfigsInTransaction(currentUserId, category);
      }

    } catch (BusinessException e) {
      log.error("Business error getting configurations for category: {}", category, e);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error getting configurations for category: {}", category, e);
      throw new InternalServerException("Failed to retrieve configurations");
    }
  }

  /**
   * Get user's selected configurations by category (for regular users)
   * This method returns ConfigsPrimary data but only for items the user has
   * selected
   * Business Logic: Return user's selected configurations from ConfigsUser joined
   * with ConfigsPrimary
   */
  public List<ConfigsPrimaryDto> getUserSelectedConfigsByCategory(String category) {
    try {
      log.debug("Fetching user-selected configurations for category: {}", category);

      // Validate input
      if (category == null || category.trim().isEmpty()) {
        throw new BusinessException("Category cannot be null or empty");
      }

      // Get current authenticated user ID from SecurityContextHolder
      Long currentUserId = securityUtil.getCurrentUserId();

      return getUserSelectedConfigsInTransaction(currentUserId, category);

    } catch (BusinessException e) {
      log.error("Business error getting user-selected configurations for category: {}", category, e);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error getting user-selected configurations for category: {}", category, e);
      throw new InternalServerException("Failed to retrieve user-selected configurations");
    }
  }

  /**
   * Private transactional method for getting user-selected configurations
   */
  @Transactional(readOnly = true)
  private List<ConfigsPrimaryDto> getUserSelectedConfigsInTransaction(Long userId, String category) {
    // Query user's selected configurations for this category
    List<ConfigsUser> userConfigs = configsUserRepository.findByUserIdAndCategory(userId, category);

    if (userConfigs.isEmpty()) {
      log.info("No user-selected configurations found for category: {} and userId: {}", category, userId);
      return List.of(); // Return empty list if user hasn't selected any configurations
    }

    // Extract ConfigsPrimary from user's selections and map to DTOs
    List<ConfigsPrimaryDto> result = userConfigs.stream()
        .map(ConfigsUser::getConfigsPrimary) // Get the associated ConfigsPrimary
        .map(configsPrimaryMapper::toDto) // Map to DTO
        .toList();

    log.debug("Successfully retrieved {} user-selected configurations for category: {} and userId: {}",
        result.size(), category, userId);
    return result;
  }

  /**
   * Get all available configurations by category (admin view)
   * This method returns all ConfigsPrimary data regardless of user selection
   */
  public List<ConfigsPrimaryDto> getAllConfigsByCategory(String category) {
    try {
      log.debug("Fetching all available configurations for category: {}", category);

      // Validate input
      if (category == null || category.trim().isEmpty()) {
        throw new BusinessException("Category cannot be null or empty");
      }

      return getAllAvailableConfigsInTransaction(category);

    } catch (BusinessException e) {
      log.error("Business error getting all configurations for category: {}", category, e);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error getting all configurations for category: {}", category, e);
      throw new InternalServerException("Failed to retrieve all configurations");
    }
  }

  /**
   * Private transactional method for getting all available configurations
   */
  @Transactional(readOnly = true)
  private List<ConfigsPrimaryDto> getAllAvailableConfigsInTransaction(String category) {
    log.debug("Fetching all available configurations for category: {}", category);

    // Query database for all active configurations
    List<ConfigsPrimary> configs = configsPrimaryRepository.findByCategoryAndActiveOrderBySortOrder(
        category.trim(), true);

    if (configs.isEmpty()) {
      log.warn("No active configurations found for category: {}", category);
    }

    // Map to DTOs
    List<ConfigsPrimaryDto> result = configs.stream()
        .map(configsPrimaryMapper::toDto)
        .toList();

    log.debug("Successfully retrieved {} total configurations for category: {}", result.size(), category);
    return result;
  }

  public List<ConfigsPrimaryDto> getConfigsPrimaryByCategory(String category) {
    return getConfigsByCategory(category);
  }

  public List<ConfigsUserDto> getConfigsUserByUserIdAndCategory(Long userId, String category) {
    try {
      log.debug("Fetching user configurations for userId: {} and category: {}", userId, category);

      // Validate input
      if (userId == null) {
        throw new BusinessException("User ID cannot be null");
      }
      if (category == null || category.trim().isEmpty()) {
        throw new BusinessException("Category cannot be null or empty");
      }

      return getUserConfigsInTransaction(userId, category);

    } catch (BusinessException | NotFoundException e) {
      log.error("Error getting user configurations for userId: {} and category: {}", userId, category, e);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error getting user configurations for userId: {} and category: {}",
          userId, category, e);
      throw new InternalServerException("Failed to retrieve user configurations");
    }
  }

  /**
   * Private transactional method for getting user configurations by userId and
   * category
   */
  @Transactional(readOnly = true)
  private List<ConfigsUserDto> getUserConfigsInTransaction(Long userId, String category) {
    // Verify user exists
    if (!userRepository.existsById(userId)) {
      throw new NotFoundException("User not found with id: " + userId);
    }

    // Use optimized query to get user configurations by category directly
    List<ConfigsUserDto> result = configsUserRepository.findByUserIdAndCategory(userId, category.trim())
        .stream()
        .map(configsUserMapper::toDto)
        .toList();

    log.debug("Successfully retrieved {} user configurations for userId: {} and category: {}",
        result.size(), userId, category);
    return result;
  }

  /**
   * Get user-specific configurations by User entity and category
   * Returns ConfigsUserDto objects that include both user and primary config data
   * Use this method when you already have the User entity to avoid additional
   * database lookup
   */
  public List<ConfigsUserDto> getConfigsUserByUserAndCategory(User user, String category) {
    try {
      // Validate input
      if (user == null) {
        throw new BusinessException("User cannot be null");
      }
      if (category == null || category.trim().isEmpty()) {
        throw new BusinessException("Category cannot be null or empty");
      }

      log.debug("Fetching user configurations for user: {} and category: {}", user.getId(), category);

      return getUserConfigsByUserInTransaction(user, category);

    } catch (BusinessException e) {
      log.error("Business error getting user configurations for user: {} and category: {}",
          user != null ? user.getId() : "null", category, e);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error getting user configurations for user: {} and category: {}",
          user != null ? user.getId() : "null", category, e);
      throw new InternalServerException("Failed to retrieve user configurations");
    }
  }

  /**
   * Private transactional method for getting user configurations by User entity
   */
  @Transactional(readOnly = true)
  private List<ConfigsUserDto> getUserConfigsByUserInTransaction(User user, String category) {
    // Use optimized query to get user configurations by category directly
    List<ConfigsUserDto> result = configsUserRepository.findByUserAndCategory(user, category.trim())
        .stream()
        .map(configsUserMapper::toDto)
        .toList();

    log.debug("Successfully retrieved {} user configurations for user: {} and category: {}",
        result.size(), user.getId(), category);
    return result;
  }

  /**
   * Get user-specific configurations for current authenticated user by category
   * with role-based access
   * - ADMIN: Returns all users' configurations for the category
   * - USER: Returns only current user's configurations for the category
   * Returns ConfigsUserDto objects that include both user and primary config data
   */
  public List<ConfigsUserDto> getCurrentUserConfigsByCategory(String category) {
    try {
      log.debug("Fetching current user configurations for category: {}", category);

      // Validate input
      if (category == null || category.trim().isEmpty()) {
        throw new BusinessException("Category cannot be null or empty");
      }

      // Check if current user is admin
      boolean isAdmin = securityUtil.isCurrentUserAdmin();

      if (isAdmin) {
        log.debug("Admin user detected, returning all users' configurations for category: {}", category);
        return getAllUsersConfigsByCategory(category);
      } else {
        log.debug("Regular user detected, returning current user's configurations for category: {}", category);

        // Get current authenticated user ID from SecurityContextHolder
        Long currentUserId = securityUtil.getCurrentUserId();

        return getCurrentUserConfigsInTransaction(currentUserId, category);
      }

    } catch (BusinessException e) {
      log.error("Business error getting current user configurations for category: {}", category, e);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error getting current user configurations for category: {}", category, e);
      throw new InternalServerException("Failed to retrieve current user configurations");
    }
  }

  /**
   * Private transactional method for getting current user configurations
   */
  @Transactional(readOnly = true)
  private List<ConfigsUserDto> getCurrentUserConfigsInTransaction(Long userId, String category) {
    // Use optimized query to get user configurations by category directly
    List<ConfigsUserDto> result = configsUserRepository.findByUserIdAndCategory(userId, category.trim())
        .stream()
        .map(configsUserMapper::toDto)
        .toList();

    log.debug("Successfully retrieved {} current user configurations for category: {}",
        result.size(), category);
    return result;
  }

  /**
   * Get all users' configurations by category (admin only)
   * Returns ConfigsUserDto objects for all users who have selected configurations
   * in this category
   */
  public List<ConfigsUserDto> getAllUsersConfigsByCategory(String category) {
    try {
      log.debug("Fetching all users' configurations for category: {}", category);

      // Validate input
      if (category == null || category.trim().isEmpty()) {
        throw new BusinessException("Category cannot be null or empty");
      }

      return getAllUsersConfigsInTransaction(category);

    } catch (BusinessException e) {
      log.error("Business error getting all users' configurations for category: {}", category, e);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error getting all users' configurations for category: {}", category, e);
      throw new InternalServerException("Failed to retrieve all users' configurations");
    }
  }

  /**
   * Private transactional method for getting all users' configurations
   */
  @Transactional(readOnly = true)
  private List<ConfigsUserDto> getAllUsersConfigsInTransaction(String category) {
    // Query all user configurations for this category
    List<ConfigsUserDto> result = configsUserRepository.findAllByCategory(category.trim())
        .stream()
        .map(configsUserMapper::toDto)
        .toList();

    log.debug("Successfully retrieved {} total user configurations for category: {}", result.size(), category);
    return result;
  }

}