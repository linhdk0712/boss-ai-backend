package ai.content.auto.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TemplateUsageLogDto {
    private Long id;
    private Long templateId;
    private String templateName;
    private Long userId;
    private String username;
    private Long contentGenerationId;
    private String usageType;
    private String usageSource;
    private Long generationTimeMs;
    private Integer tokensUsed;
    private BigDecimal generationCost;
    private BigDecimal qualityScore;
    private Integer userRating;
    private String userFeedback;
    private Boolean wasSuccessful;
    private Map<String, Object> parametersUsed;
    private Map<String, Object> customizationsMade;
    private List<String> fieldsModified;
    private String sessionId;
    private String userAgent;
    private InetAddress ipAddress;
    private String countryCode;
    private String timezone;
    private String deviceType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime usedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;
}