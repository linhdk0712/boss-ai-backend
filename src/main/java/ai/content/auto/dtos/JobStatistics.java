package ai.content.auto.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Job statistics DTO for dashboard metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatistics {

    private long totalJobs;
    private long completedJobs;
    private long failedJobs;
    private long processingJobs;
    private long queuedJobs;
    private long cancelledJobs;
    private double successRate;
    private Long averageProcessingTimeMs;
    private Long totalProcessingTimeMs;
    private Long totalTokensUsed;
    private BigDecimal totalGenerationCost;

    /**
     * Calculate success rate percentage
     */
    public double getSuccessRatePercentage() {
        if (totalJobs == 0) {
            return 0.0;
        }
        return (completedJobs * 100.0) / totalJobs;
    }

    /**
     * Get formatted average processing time
     */
    public String getFormattedAverageProcessingTime() {
        if (averageProcessingTimeMs == null) {
            return "N/A";
        }

        long seconds = averageProcessingTimeMs / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    /**
     * Check if there are any active jobs
     */
    public boolean hasActiveJobs() {
        return processingJobs > 0 || queuedJobs > 0;
    }
}