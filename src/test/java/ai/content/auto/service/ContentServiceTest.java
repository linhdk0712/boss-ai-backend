package ai.content.auto.service;

import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.entity.User;
import ai.content.auto.mapper.ContentGenerationMapper;
import ai.content.auto.repository.ContentGenerationRepository;
import ai.content.auto.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

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
    private OpenAiService openAiService;

    @Mock
    private N8nService n8nService;

    @Mock
    private SecurityUtil securityUtil;

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
    void generateContent_Success() {
        // Given
        Map<String, Object> openaiResult = new HashMap<>();
        openaiResult.put("generatedContent", "Generated test content");
        openaiResult.put("wordCount", 10);
        openaiResult.put("characterCount", 50);
        openaiResult.put("tokensUsed", 20);
        openaiResult.put("processingTimeMs", 1000L);
        openaiResult.put("status", "COMPLETED");

        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(openAiService.generateContent(any(ContentGenerateRequest.class), any(User.class)))
                .thenReturn(openaiResult);

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

        verify(securityUtil).getCurrentUser();
        verify(openAiService).generateContent(testRequest, testUser);
    }

    @Test
    void generateContent_WithTitle() {
        // Given
        testRequest.setTitle("Custom Title");

        Map<String, Object> openaiResult = new HashMap<>();
        openaiResult.put("generatedContent", "Generated test content");
        openaiResult.put("status", "COMPLETED");

        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(openAiService.generateContent(any(ContentGenerateRequest.class), any(User.class)))
                .thenReturn(openaiResult);

        // When
        ContentGenerateResponse response = contentService.generateContent(testRequest);

        // Then
        assertNotNull(response);
        assertEquals("Custom Title", response.getTitle());
    }
}