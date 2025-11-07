package ai.content.auto.service;

import ai.content.auto.dtos.ContentTemplateDto;
import ai.content.auto.dtos.CreateTemplateRequest;
import ai.content.auto.entity.ContentTemplate;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.ContentTemplateMapper;
import ai.content.auto.mapper.TemplateUsageLogMapper;
import ai.content.auto.mapper.UserPresetMapper;
import ai.content.auto.repository.ContentTemplateRepository;
import ai.content.auto.repository.TemplateUsageLogRepository;
import ai.content.auto.repository.UserPresetRepository;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateManagementServiceTest {

    @Mock
    private ContentTemplateRepository templateRepository;

    @Mock
    private UserPresetRepository presetRepository;

    @Mock
    private TemplateUsageLogRepository usageLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentTemplateMapper templateMapper;

    @Mock
    private UserPresetMapper presetMapper;

    @Mock
    private TemplateUsageLogMapper usageLogMapper;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private TemplateManagementService templateManagementService;

    private User testUser;
    private ContentTemplate testTemplate;
    private ContentTemplateDto testTemplateDto;
    private CreateTemplateRequest createTemplateRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Setup test template
        testTemplate = new ContentTemplate();
        testTemplate.setId(1L);
        testTemplate.setName("Test Template");
        testTemplate.setDescription("Test Description");
        testTemplate.setCategory("Marketing");
        testTemplate.setPromptTemplate("Generate content about {topic}");
        testTemplate.setDefaultParams(Map.of("topic", "technology"));
        testTemplate.setContentType("Blog Post");
        testTemplate.setVisibility("PUBLIC");
        testTemplate.setStatus("ACTIVE");
        testTemplate.setUsageCount(0);
        testTemplate.setCreatedBy(testUser);
        testTemplate.setCreatedAt(OffsetDateTime.now());

        // Setup test template DTO
        testTemplateDto = new ContentTemplateDto();
        testTemplateDto.setId(1L);
        testTemplateDto.setName("Test Template");
        testTemplateDto.setDescription("Test Description");
        testTemplateDto.setCategory("Marketing");
        testTemplateDto.setContentType("Blog Post");

        // Setup create template request
        createTemplateRequest = new CreateTemplateRequest();
        createTemplateRequest.setName("New Template");
        createTemplateRequest.setDescription("New Description");
        createTemplateRequest.setCategory("Technology");
        createTemplateRequest.setPromptTemplate("Create content for {subject}");
        createTemplateRequest.setDefaultParams(Map.of("subject", "AI"));
        createTemplateRequest.setContentType("Article");
        createTemplateRequest.setVisibility("PRIVATE");
    }

    @Test
    void testGetTemplatesByCategory_Success() {
        // Arrange
        String category = "Marketing";
        String industry = "Technology";
        Long userId = 1L;

        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findByCategoryAndIndustryOrderByUsageCountDesc(category, industry, userId))
                .thenReturn(List.of(testTemplate));
        when(templateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        List<ContentTemplateDto> result = templateManagementService.getTemplatesByCategory(category, industry);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTemplateDto.getName(), result.get(0).getName());
        verify(templateRepository).findByCategoryAndIndustryOrderByUsageCountDesc(category, industry, userId);
        verify(templateMapper).toDto(testTemplate);
    }

    @Test
    void testCreateTemplate_Success() {
        // Arrange
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findByNameAndCreatedBy_Id(createTemplateRequest.getName(), testUser.getId()))
                .thenReturn(Optional.empty());
        when(templateMapper.toEntity(createTemplateRequest, testUser)).thenReturn(testTemplate);
        when(templateRepository.save(testTemplate)).thenReturn(testTemplate);
        when(templateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        ContentTemplateDto result = templateManagementService.createTemplate(createTemplateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testTemplateDto.getName(), result.getName());
        verify(templateRepository).findByNameAndCreatedBy_Id(createTemplateRequest.getName(), testUser.getId());
        verify(templateRepository).save(testTemplate);
        verify(templateMapper).toEntity(createTemplateRequest, testUser);
        verify(templateMapper).toDto(testTemplate);
    }

    @Test
    void testCreateTemplate_DuplicateName_ThrowsException() {
        // Arrange
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findByNameAndCreatedBy_Id(createTemplateRequest.getName(), testUser.getId()))
                .thenReturn(Optional.of(testTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            templateManagementService.createTemplate(createTemplateRequest);
        });

        assertEquals("Template with this name already exists", exception.getMessage());
        verify(templateRepository).findByNameAndCreatedBy_Id(createTemplateRequest.getName(), testUser.getId());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void testGetTemplateById_Success() {
        // Arrange
        Long templateId = 1L;
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(templateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        ContentTemplateDto result = templateManagementService.getTemplateById(templateId);

        // Assert
        assertNotNull(result);
        assertEquals(testTemplateDto.getName(), result.getName());
        verify(templateRepository).findById(templateId);
        verify(templateMapper).toDto(testTemplate);
    }

    @Test
    void testGetTemplateById_NotFound_ThrowsException() {
        // Arrange
        Long templateId = 999L;
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            templateManagementService.getTemplateById(templateId);
        });

        assertEquals("Template not found", exception.getMessage());
        verify(templateRepository).findById(templateId);
        verify(templateMapper, never()).toDto(any());
    }

    @Test
    void testGetTemplateById_PrivateTemplate_AccessDenied() {
        // Arrange
        Long templateId = 1L;
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        testTemplate.setVisibility("PRIVATE");
        testTemplate.setCreatedBy(otherUser);

        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            templateManagementService.getTemplateById(templateId);
        });

        assertEquals("You don't have access to this template", exception.getMessage());
        verify(templateRepository).findById(templateId);
        verify(templateMapper, never()).toDto(any());
    }

    @Test
    void testDeleteTemplate_Success() {
        // Arrange
        Long templateId = 1L;
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(testTemplate)).thenReturn(testTemplate);

        // Act
        assertDoesNotThrow(() -> {
            templateManagementService.deleteTemplate(templateId);
        });

        // Assert
        verify(templateRepository).findById(templateId);
        verify(templateRepository).save(testTemplate);
        assertEquals("DELETED", testTemplate.getStatus());
    }

    @Test
    void testDeleteTemplate_NotOwner_ThrowsException() {
        // Arrange
        Long templateId = 1L;
        User otherUser = new User();
        otherUser.setId(2L);
        testTemplate.setCreatedBy(otherUser);

        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            templateManagementService.deleteTemplate(templateId);
        });

        assertEquals("You don't have permission to delete this template", exception.getMessage());
        verify(templateRepository).findById(templateId);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void testDeleteTemplate_SystemTemplate_ThrowsException() {
        // Arrange
        Long templateId = 1L;
        testTemplate.setIsSystemTemplate(true);

        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            templateManagementService.deleteTemplate(templateId);
        });

        assertEquals("System templates cannot be deleted", exception.getMessage());
        verify(templateRepository).findById(templateId);
        verify(templateRepository, never()).save(any());
    }
}