package ai.content.auto.dtos;

import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.entity.GenerationJob.JobPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Detailed job information response DTO
 * Contains complete job information for detail view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailsResponse {

    private Long id;
    private String jobId;
    private String contentType;
    private JobStatus status;
    private JobPriority priority;
    private String aiProvider;
    private String aiModel;
    private Map<String, Object> parameters;
    private String result;
    private String errorMessage;
    private Map<String, Object> errorDetails;
    private List<String> executionLogs;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Long executionTimeMs;
    private Integer retryCount;
    private Integer maxRetries;
    private Long originalJobId;
    private Integer tokensUsed;
    private BigDecimal generationCost;
    private Map<String, Object> metadata;

    /**
     * Check if job can be retried
     */
    public boolean canRetry() {
        return (status == JobStatus.FAILED || status == JobStatus.CANCELLED) &&
                retryCount < maxRetries;
    }

    /**
     * Check if job can generate video
     */
    public boolean canGenerateVideo() {
        return status == JobStatus.COMPLETED && result != null && !result.trim().isEmpty();
    }

    /**
     * Get processing duration in a human-readable format
     */
    public String getFormattedDuration() {
        if (executionTimeMs == null) {
            return "N/A";
        }

        long seconds = executionTimeMs / 1000;
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + " minutes " + remainingSeconds + " seconds";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + " hours " + minutes + " minutes";
        }
    }

    /**
     * Get job progress percentage (estimated)
     */
    public int getProgressPercentage() {
        switch (status) {
            case QUEUED:
                return 0;
            case PROCESSING:
                return 50;
            case COMPLETED:
                return 100;
            case FAILED:
            case CANCELLED:
            case EXPIRED:
                return 0;
            default:
                return 0;
        }
    }
}