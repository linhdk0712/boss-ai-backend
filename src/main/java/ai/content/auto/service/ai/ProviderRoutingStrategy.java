package ai.content.auto.service.ai;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Provider routing strategy configuration
 */
@Data
@Builder
public class ProviderRoutingStrategy {

    private RoutingStrategy strategy;
    private Map<String, Double> routingWeights;

    private boolean loadBalancingEnabled;
    private List<String> failoverOrder;

    private Instant lastOptimized;
    private String optimizationReason;

    // Configuration
    private int maxRetries;
    private long retryDelayMs;
    private double circuitBreakerThreshold;

    // Monitoring
    private Map<String, Integer> requestCounts;
    private Map<String, Long> lastUsed;
}