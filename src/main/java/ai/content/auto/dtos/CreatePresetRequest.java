package ai.content.auto.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreatePresetRequest {

    @NotBlank(message = "Preset name is required")
    @Size(max = 200, message = "Preset name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Configuration is required")
    private Map<String, Object> configuration;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Size(max = 50, message = "Content type must not exceed 50 characters")
    private String contentType;

    private Boolean isDefault = false;

    private Boolean isFavorite = false;

    private Boolean isShared = false;

    private Boolean sharedWithWorkspace = false;

    private Long workspaceId;

    private List<String> tags;
}