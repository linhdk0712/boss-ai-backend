package ai.content.auto.service.ai;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderPerformanceInsights {
    private List<String> insights;
    private List<String> recommendations;
    private Instant generatedAt;
}
