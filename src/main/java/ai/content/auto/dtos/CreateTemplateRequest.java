package ai.content.auto.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 200, message = "Template name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotBlank(message = "Prompt template is required")
    private String promptTemplate;

    @NotNull(message = "Default parameters are required")
    private Map<String, Object> defaultParams;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @NotBlank(message = "Content type is required")
    @Size(max = 50, message = "Content type must not exceed 50 characters")
    private String contentType;

    @Size(max = 200, message = "Target audience must not exceed 200 characters")
    private String targetAudience;

    @Size(max = 50, message = "Tone must not exceed 50 characters")
    private String tone;

    @Size(max = 10, message = "Language must not exceed 10 characters")
    private String language = "vi";

    private String visibility = "PRIVATE"; // PUBLIC, PRIVATE

    private Boolean isFeatured = false;

    private List<String> tags;

    private List<String> requiredFields;

    private List<String> optionalFields;

    private Map<String, Object> validationRules;
}