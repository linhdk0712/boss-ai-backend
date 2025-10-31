package ai.content.auto.dtos;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ContentGenerateResponse {
    private String generatedContent;
    private String title;
    private Integer wordCount;
    private Integer characterCount;
    private Integer tokensUsed;
    private BigDecimal generationCost;
    private Long processingTimeMs;
    private String status;
    private String errorMessage;
}