package ai.content.auto.dtos;

import ai.content.auto.entity.GenerationJob.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for job retry operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryJobResponse {

    private Long originalJobId;
    private Long newJobId;
    private String newJobUuid;
    private JobStatus newJobStatus;
    private String message;
    private Instant retryInitiatedAt;
    private Integer queuePosition;
    private String websocketChannel;

    /**
     * Create successful retry response
     */
    public static RetryJobResponse success(Long originalJobId, Long newJobId, String newJobUuid,
            Integer queuePosition, String websocketChannel) {
        return RetryJobResponse.builder()
                .originalJobId(originalJobId)
                .newJobId(newJobId)
                .newJobUuid(newJobUuid)
                .newJobStatus(JobStatus.QUEUED)
                .message("Job retry initiated successfully")
                .retryInitiatedAt(Instant.now())
                .queuePosition(queuePosition)
                .websocketChannel(websocketChannel)
                .build();
    }

    /**
     * Create failed retry response
     */
    public static RetryJobResponse failure(Long originalJobId, String errorMessage) {
        return RetryJobResponse.builder()
                .originalJobId(originalJobId)
                .message(errorMessage)
                .retryInitiatedAt(Instant.now())
                .build();
    }
}