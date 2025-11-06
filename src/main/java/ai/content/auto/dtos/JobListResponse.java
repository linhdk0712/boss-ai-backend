package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for job list with pagination and statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobListResponse {

    private List<JobSummaryDto> jobs;
    private PaginatedResponse.PaginationMetadata pagination;
    private JobStatistics statistics;

    /**
     * Create job list response from paginated data
     */
    public static JobListResponse of(List<JobSummaryDto> jobs,
            PaginatedResponse.PaginationMetadata pagination,
            JobStatistics statistics) {
        return JobListResponse.builder()
                .jobs(jobs)
                .pagination(pagination)
                .statistics(statistics)
                .build();
    }
}