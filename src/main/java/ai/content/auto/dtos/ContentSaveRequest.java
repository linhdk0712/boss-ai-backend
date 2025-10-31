package ai.content.auto.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContentSaveRequest {
    @NotBlank(message = "Generated content is required")
    private String generatedContent;

    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 20, message = "Content type must not exceed 20 characters")
    private String contentType;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 200, message = "Target audience must not exceed 200 characters")
    private String targetAudience;

    @Size(max = 50, message = "Tone must not exceed 50 characters")
    private String tone;

    @Size(max = 10, message = "Language must not exceed 10 characters")
    private String language = "vi";

    private String prompt;
}