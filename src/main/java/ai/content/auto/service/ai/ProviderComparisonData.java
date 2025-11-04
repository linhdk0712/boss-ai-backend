package ai.content.auto.service.ai;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderComparisonData {
    private List<ProviderComparisonItem> providers;
    private Instant generatedAt;
}
