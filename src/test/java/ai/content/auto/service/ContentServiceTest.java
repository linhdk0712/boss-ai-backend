package ai.content.auto.service;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.User;
import ai.content.auto.mapper.ContentGenerationMapper;
import ai.content.auto.repository.ContentGenerationRepository;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.service.ai.AIProviderManager;
import ai.content.auto.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentGenerationRepository contentGenerationRepository;

    @Mock
    private ContentGenerationMapper contentGenerationMapper;

    @Mock
    private AIProviderManager aiProviderManager;

    @Mock
    private N8nService n8nService;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private ConfigurationValidationService configurationValidationService;

    @Mock
    private ContentNormalizationService contentNormalizationService;

    @Mock
    private ContentVersioningService contentVersioningService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContentService contentService;

    private User testUser;
    private ContentGenerateRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testRequest = new ContentGenerateRequest();
        testRequest.setContent("Test content for generation");
        testRequest.setContentType("blog");
        testRequest.setLanguage("vi");
    }

    @Test
    void generateContent_Success_AutoSavesToDatabase() {
        // Given
        ContentGenerateResponse aiResponse = new ContentGenerateResponse();
        aiResponse.setGeneratedContent("Generated test content");
        aiResponse.setWordCount(10);
        aiResponse.setCharacterCount(50);
        aiResponse.setTokensUsed(20);
        aiResponse.setProcessingTimeMs(1000L);
        aiResponse.setStatus("COMPLETED");
        aiResponse.setAiProvider("OpenAI");
        aiResponse.setAiModel("gpt-3.5-turbo");

        ContentGeneration savedContent = new ContentGeneration();
        savedContent.setId(123L);
        savedContent.setUser(testUser);
        savedContent.setGeneratedContent("Generated test content");
        savedContent.setStatus(ContentConstants.STATUS_GENERATED);

        // Mock all dependencies
        when(contentNormalizationService.normalizeGenerateRequest(any())).thenReturn(testRequest);
        when(configurationValidationService.isValidContentType(any())).thenReturn(true);
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(aiProviderManager.generateContent(any(ContentGenerateRequest.class), any(User.class)))
                .thenReturn(aiResponse);
        when(contentGenerationRepository.save(any(ContentGeneration.class))).thenReturn(savedContent);

        // When
        ContentGenerateResponse response = contentService.generateContent(testRequest);

        // Then
        assertNotNull(response);
        assertEquals("Generated test content", response.getGeneratedContent());
        assertEquals(10, response.getWordCount());
        assertEquals(50, response.getCharacterCount());
        assertEquals(20, response.getTokensUsed());
        assertEquals(1000L, response.getProcessingTimeMs());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(123L, response.getContentId()); // Verify content ID is set

        // Verify that content was saved to database
        verify(contentGenerationRepository).save(any(ContentGeneration.class));
        verify(contentVersioningService).createVersion(eq(123L), any(ContentGenerateResponse.class));
        verify(securityUtil).getCurrentUser();
        verify(aiProviderManager).generateContent(testRequest, testUser);
    }

    @Test
    void generateContent_WithTitle_AutoSavesToDatabase() {
        // Given
        testRequest.setTitle("Custom Title");

        ContentGenerateResponse aiResponse = new ContentGenerateResponse();
        aiResponse.setGeneratedContent("Generated test content");
        aiResponse.setStatus("COMPLETED");
        aiResponse.setAiProvider("OpenAI");
        aiResponse.setAiModel("gpt-3.5-turbo");

        ContentGeneration savedContent = new ContentGeneration();
        savedContent.setId(456L);
        savedContent.setUser(testUser);
        savedContent.setTitle("Custom Title");
        savedContent.setStatus(ContentConstants.STATUS_GENERATED);

        // Mock all dependencies
        when(contentNormalizationService.normalizeGenerateRequest(any())).thenReturn(testRequest);
        when(configurationValidationService.isValidContentType(any())).thenReturn(true);
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(aiProviderManager.generateContent(any(ContentGenerateRequest.class), any(User.class)))
                .thenReturn(aiResponse);
        when(contentGenerationRepository.save(any(ContentGeneration.class))).thenReturn(savedContent);

        // When
        ContentGenerateResponse response = contentService.generateContent(testRequest);

        // Then
        assertNotNull(response);
        assertEquals("Custom Title", response.getTitle());
        assertEquals(456L, response.getContentId());

        // Verify that content was saved with correct title
        verify(contentGenerationRepository).save(argThat(content -> "Custom Title".equals(content.getTitle()) &&
                ContentConstants.STATUS_GENERATED.equals(content.getStatus())));
    }

    @Test
    void generateContentForUser_AsyncProcessing_AutoSavesToDatabase() {
        // Given
        Long userId = 1L;

        ContentGenerateResponse aiResponse = new ContentGenerateResponse();
        aiResponse.setGeneratedContent("Async generated content");
        aiResponse.setWordCount(15);
        aiResponse.setCharacterCount(75);
        aiResponse.setAiProvider("OpenAI");
        aiResponse.setAiModel("gpt-4");

        ContentGeneration savedContent = new ContentGeneration();
        savedContent.setId(789L);
        savedContent.setUser(testUser);
        savedContent.setGeneratedContent("Async generated content");
        savedContent.setStatus(ContentConstants.STATUS_GENERATED);

        // Mock all dependencies
        when(contentNormalizationService.normalizeGenerateRequest(any())).thenReturn(testRequest);
        when(configurationValidationService.isValidContentType(any())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(testUser));
        when(aiProviderManager.generateContent(any(ContentGenerateRequest.class), any(User.class)))
                .thenReturn(aiResponse);
        when(contentGenerationRepository.save(any(ContentGeneration.class))).thenReturn(savedContent);

        // When
        ContentGenerateResponse response = contentService.generateContentForUser(testRequest, userId);

        // Then
        assertNotNull(response);
        assertEquals("Async generated content", response.getGeneratedContent());
        assertEquals(15, response.getWordCount());
        assertEquals(75, response.getCharacterCount());
        assertEquals(789L, response.getContentId());

        // Verify async processing saved content correctly
        verify(userRepository).findById(userId);
        verify(contentGenerationRepository).save(any(ContentGeneration.class));
        verify(contentVersioningService).createVersion(eq(789L), any(ContentGenerateResponse.class));
        verify(aiProviderManager).generateContent(testRequest, testUser);
    }
}