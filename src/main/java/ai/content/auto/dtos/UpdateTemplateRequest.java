package ai.content.auto.dtos;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UpdateTemplateRequest {

    @Size(max = 200, message = "Template name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    private String promptTemplate;

    private Map<String, Object> defaultParams;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 50, message = "Content type must not exceed 50 characters")
    private String contentType;

    @Size(max = 200, message = "Target audience must not exceed 200 characters")
    private String targetAudience;

    @Size(max = 50, message = "Tone must not exceed 50 characters")
    private String tone;

    @Size(max = 10, message = "Language must not exceed 10 characters")
    private String language;

    private String visibility; // PUBLIC, PRIVATE

    private Boolean isFeatured;

    private List<String> tags;

    private List<String> requiredFields;

    private List<String> optionalFields;

    private Map<String, Object> validationRules;
}