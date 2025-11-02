package ai.content.auto.service;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.VUserConfigDto;
import ai.content.auto.entity.User;
import ai.content.auto.entity.VUserConfig;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.VUserConfigMapper;
import ai.content.auto.repository.ConfigsPrimaryRepository;
import ai.content.auto.repository.ConfigsUserRepository;
import ai.content.auto.repository.VUserConfigRepository;
import ai.content.auto.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VUserConfigServiceTest {

    @Mock
    private VUserConfigRepository vUserConfigRepository;

    @Mock
    private ConfigsUserRepository configsUserRepository;

    @Mock
    private ConfigsPrimaryRepository configsPrimaryRepository;

    @Mock
    private VUserConfigMapper vUserConfigMapper;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private VUserConfigService vUserConfigService;

    private User testUser;
    private Long testUserId = 1L;
    private VUserConfig testVUserConfig;
    private VUserConfigDto testVUserConfigDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(testUserId);

        testVUserConfig = new VUserConfig();
        testVUserConfigDto = new VUserConfigDto(
                1L, // id
                "tone", // category
                "professional", // value
                "Professional", // label
                "Professional Tone", // displayLabel
                "Professional writing tone", // description
                1, // sortOrder
                true, // configActive
                "en", // language
                null, // configCreatedAt
                null, // configUpdatedAt
                testUserId, // userId
                true, // isSelected
                null, // userSelectionCreatedAt
                null // userSelectionUpdatedAt
        );
    }

    @Test
    void findAllByCategory_WithValidCategory_ShouldReturnList() {
        // Given
        when(securityUtil.getCurrentUserId()).thenReturn(testUserId);
        when(vUserConfigRepository.findAllByCategory(testUserId, ContentConstants.CATEGORY_TONE))
                .thenReturn(List.of(testVUserConfig));
        when(vUserConfigMapper.toDto(testVUserConfig)).thenReturn(testVUserConfigDto);

        // When
        List<VUserConfigDto> result = vUserConfigService.findAllByCategory(ContentConstants.CATEGORY_TONE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testVUserConfigDto, result.get(0));
        verify(securityUtil).getCurrentUserId();
        verify(vUserConfigRepository).findAllByCategory(testUserId, ContentConstants.CATEGORY_TONE);
    }

    @Test
    void findAllByCategory_WithNullCategory_ShouldThrowBusinessException() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> vUserConfigService.findAllByCategory(null));

        assertEquals("Category is required", exception.getMessage());
        verify(securityUtil, never()).getCurrentUserId();
    }

    @Test
    void findAllByCategory_WithEmptyCategory_ShouldThrowBusinessException() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> vUserConfigService.findAllByCategory(""));

        assertEquals("Category is required", exception.getMessage());
        verify(securityUtil, never()).getCurrentUserId();
    }

    @Test
    void findAllByCategory_WithNullUserId_ShouldThrowBusinessException() {
        // Given
        when(securityUtil.getCurrentUserId()).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> vUserConfigService.findAllByCategory(ContentConstants.CATEGORY_TONE));

        assertEquals("User authentication required", exception.getMessage());
        verify(securityUtil).getCurrentUserId();
        verify(vUserConfigRepository, never()).findAllByCategory(any(), any());
    }

    @Test
    void findAllByCategory_WithValidData_ShouldReturnEmptyList() {
        // Given
        when(securityUtil.getCurrentUserId()).thenReturn(testUserId);
        when(vUserConfigRepository.findAllByCategory(testUserId, "unknown-category"))
                .thenReturn(new ArrayList<>());

        // When
        List<VUserConfigDto> result = vUserConfigService.findAllByCategory("unknown-category");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(securityUtil).getCurrentUserId();
        verify(vUserConfigRepository).findAllByCategory(testUserId, "unknown-category");
    }

    @Test
    void findAllByCategory_WithRepositoryException_ShouldThrowBusinessException() {
        // Given
        when(securityUtil.getCurrentUserId()).thenReturn(testUserId);
        when(vUserConfigRepository.findAllByCategory(testUserId, ContentConstants.CATEGORY_TONE))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> vUserConfigService.findAllByCategory(ContentConstants.CATEGORY_TONE));

        assertEquals("Failed to fetch settings", exception.getMessage());
    }
}