package ai.content.auto.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class UserPresetDto {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String description;
    private Map<String, Object> configuration;
    private String category;
    private String contentType;
    private Boolean isDefault;
    private Boolean isFavorite;
    private Integer usageCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime lastUsedAt;

    private Boolean isShared;
    private Boolean sharedWithWorkspace;
    private Long workspaceId;
    private List<String> tags;
    private Long averageGenerationTimeMs;
    private BigDecimal averageQualityScore;
    private BigDecimal successRate;
    private Integer totalUses;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime updatedAt;

    private Long version;
}