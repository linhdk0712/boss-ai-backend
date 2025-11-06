package ai.content.auto.dtos;

import ai.content.auto.entity.GenerationJob.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Summary DTO for job list display
 * Contains essential job information for table view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSummaryDto {

    private Long id;
    private String jobId;
    private String contentType;
    private JobStatus status;
    private Instant createdAt;
    private Instant completedAt;
    private Integer retryCount;
    private Long executionTimeMs;
    private boolean canRetry;
    private boolean canGenerateVideo;
    private String errorMessage;

    /**
     * Check if job can be retried based on status and retry count
     */
    public boolean isRetryable() {
        return (status == JobStatus.FAILED || status == JobStatus.CANCELLED) &&
                retryCount < 3; // Max retries from entity
    }

    /**
     * Check if job can generate video (completed with content)
     */
    public boolean isVideoGenerationEligible() {
        return status == JobStatus.COMPLETED;
    }

    /**
     * Get formatted execution time
     */
    public String getFormattedExecutionTime() {
        if (executionTimeMs == null) {
            return null;
        }

        long seconds = executionTimeMs / 1000;
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
}