package ai.content.auto.service;

import ai.content.auto.dtos.*;
import ai.content.auto.entity.GenerationJob;
import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.OptimizedGenerationJobRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimized service for job queue management with performance enhancements
 * Includes caching, batch operations, and optimized query patterns
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizedJobQueueService {

    private final OptimizedGenerationJobRepository optimizedJobRepository;
    private final QueueManagementService queueManagementService;
    private final SecurityUtil securityUtil;

    // Cache configuration constants
    private static final String USER_JOBS_CACHE = "userJobs";
    private static final String USER_STATS_CACHE = "userJobStatistics";
    private static final String CONTENT_TYPES_CACHE = "userContentTypes";
    private static final String ACTIVE_JOBS_CACHE = "activeJobCount";

    // =============================================================================
    // OPTIMIZED JOB LISTING WITH CACHING
    // =============================================================================

    /**
     * Get jobs by user with optimized queries and caching
     */
    @Cacheable(value = USER_JOBS_CACHE, key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #criteria.hashCode()", unless = "#result == null")
    public JobListResponse getJobsByUserOptimized(Long userId, Pageable pageable, JobFilterCriteria criteria) {
        try {
            validateUserAccess(userId);
            criteria.setUserId(userId);

            // Use optimized query based on filter criteria
            Page<GenerationJob> jobsPage = getOptimizedJobsPage(userId, pageable, criteria);

            // Convert to DTOs with batch processing
            List<JobSummaryDto> jobSummaries = convertToJobSummariesBatch(jobsPage.getContent());

            // Get cached statistics
            JobStatistics statistics = getUserJobStatisticsCached(userId);

            // Create pagination metadata
            PaginatedResponse.PaginationMetadata pagination = createPaginationMetadata(jobsPage);

            log.debug("Retrieved {} jobs for user {} with {} total pages",
                    jobSummaries.size(), userId, jobsPage.getTotalPages());

            return JobListResponse.of(jobSummaries, pagination, statistics);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get optimized jobs for user: {}", userId, e);
            throw new BusinessException("Failed to retrieve user jobs");
        }
    }

    /**
     * Get job details with caching
     */
    @Cacheable(value = "jobDetails", key = "#jobId + '_' + #userId")
    public JobDetailsResponse getJobDetailsOptimized(Long jobId, Long userId) {
        try {
            validateUserAccess(userId);

            // Try to get from covering index first
            Optional<Object[]> projectionOpt = optimizedJobRepository.findJobDetailsProjection(jobId);

            if (projectionOpt.isPresent()) {
                Object[] projection = projectionOpt.get();
                // Validate user access from projection
                Long jobUserId = (Long) projection[2];
                if (!jobUserId.equals(userId)) {
                    throw new BusinessException("Access denied to job: " + jobId);
                }

                return convertProjectionToJobDetails(projection, jobId);
            }

            // Fallback to full entity fetch
            GenerationJob job = optimizedJobRepository.findById(jobId)
                    .orElseThrow(() -> new BusinessException("Job not found: " + jobId));

            if (!job.getUserId().equals(userId)) {
                throw new BusinessException("Access denied to job: " + jobId);
            }

            return convertToJobDetails(job);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get optimized job details: {} for user: {}", jobId, userId, e);
            throw new BusinessException("Failed to retrieve job details");
        }
    }

    // =============================================================================
    // OPTIMIZED SEARCH WITH CACHING
    // =============================================================================

    /**
     * Search jobs with full-text search optimization
     */
    @Cacheable(value = "searchResults", key = "#userId + '_' + #searchText + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public JobListResponse searchJobsOptimized(Long userId, String searchText, Pageable pageable) {
        try {
            validateUserAccess(userId);

            if (searchText == null || searchText.trim().isEmpty()) {
                return getJobsByUserOptimized(userId, pageable, new JobFilterCriteria());
            }

            // Use optimized full-text search
            Page<GenerationJob> jobsPage = optimizedJobRepository.searchJobsByUserOptimized(
                    userId, searchText.trim(), pageable);

            List<JobSummaryDto> jobSummaries = convertToJobSummariesBatch(jobsPage.getContent());
            JobStatistics statistics = getUserJobStatisticsCached(userId);
            PaginatedResponse.PaginationMetadata pagination = createPaginationMetadata(jobsPage);

            log.debug("Search found {} jobs for user {} with query: '{}'",
                    jobSummaries.size(), userId, searchText);

            return JobListResponse.of(jobSummaries, pagination, statistics);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to search jobs for user: {} with query: '{}'", userId, searchText, e);
            throw new BusinessException("Failed to search jobs");
        }
    }

    /**
     * Content-specific search with relevance ranking
     */
    @Cacheable(value = "contentSearch", key = "#userId + '_' + #searchText + '_' + #pageable.pageNumber")
    public JobListResponse searchJobContentOptimized(Long userId, String searchText, Pageable pageable) {
        try {
            validateUserAccess(userId);

            Page<GenerationJob> jobsPage = optimizedJobRepository.searchJobContentByUserOptimized(
                    userId, searchText.trim(), pageable);

            List<JobSummaryDto> jobSummaries = convertToJobSummariesBatch(jobsPage.getContent());
            JobStatistics statistics = getUserJobStatisticsCached(userId);
            PaginatedResponse.PaginationMetadata pagination = createPaginationMetadata(jobsPage);

            return JobListResponse.of(jobSummaries, pagination, statistics);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to search job content for user: {} with query: '{}'", userId, searchText, e);
            throw new BusinessException("Failed to search job content");
        }
    }

    // =============================================================================
    // CACHED STATISTICS AND METADATA
    // =============================================================================

    /**
     * Get user job statistics with caching and materialized view optimization
     */
    @Cacheable(value = USER_STATS_CACHE, key = "#userId")
    public JobStatistics getUserJobStatisticsCached(Long userId) {
        try {
            validateUserAccess(userId);

            // Try to get from materialized view first
            Optional<Object[]> mvStats = optimizedJobRepository.getUserJobStatisticsFromMV(userId);

            if (mvStats.isPresent()) {
                return convertMaterializedViewToStatistics(mvStats.get());
            }

            // Fallback to real-time calculation with caching
            Object[] stats = optimizedJobRepository.getUserJobStatisticsOptimized(userId);
            return convertStatsArrayToJobStatistics(stats);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get cached statistics for user: {}", userId, e);
            throw new BusinessException("Failed to retrieve user statistics");
        }
    }

    /**
     * Get available content types with caching
     */
    @Cacheable(value = CONTENT_TYPES_CACHE, key = "#userId")
    public List<String> getAvailableContentTypesCached(Long userId) {
        try {
            validateUserAccess(userId);
            return optimizedJobRepository.findDistinctContentTypesByUserIdCached(userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get cached content types for user: {}", userId, e);
            throw new BusinessException("Failed to retrieve content types");
        }
    }

    /**
     * Get active job count with caching
     */
    @Cacheable(value = ACTIVE_JOBS_CACHE, key = "#userId")
    public long getActiveJobCountCached(Long userId) {
        try {
            validateUserAccess(userId);
            return optimizedJobRepository.countActiveJobsByUserOptimized(userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get cached active job count for user: {}", userId, e);
            return 0; // Return 0 on error to avoid blocking operations
        }
    }

    // =============================================================================
    // CACHE INVALIDATION ON UPDATES
    // =============================================================================

    /**
     * Retry job with cache invalidation
     */
    @CacheEvict(value = { USER_JOBS_CACHE, USER_STATS_CACHE, ACTIVE_JOBS_CACHE }, key = "#userId")
    public RetryJobResponse retryJobWithCacheInvalidation(Long jobId, Long userId) {
        try {
            validateUserAccess(userId);

            GenerationJob originalJob = findJobForRetry(jobId, userId);
            QueueJobRequest retryRequest = createRetryJobRequest(originalJob);
            QueueJobResponse queueResponse = queueManagementService.queueJob(retryRequest);

            updateOriginalJobForRetry(originalJob, queueResponse.getJobId());

            log.info("Job {} retried successfully as {} for user: {} (cache invalidated)",
                    jobId, queueResponse.getJobId(), userId);

            return RetryJobResponse.success(
                    jobId,
                    extractJobIdFromUuid(queueResponse.getJobId()),
                    queueResponse.getJobId(),
                    queueResponse.getQueuePosition(),
                    queueResponse.getWebsocketChannel());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to retry job with cache invalidation: {} for user: {}", jobId, userId, e);
            throw new BusinessException("Failed to retry job");
        }
    }

    /**
     * Update job status with cache invalidation
     */
    @CacheEvict(value = { USER_JOBS_CACHE, USER_STATS_CACHE, ACTIVE_JOBS_CACHE }, key = "#userId")
    public void updateJobStatusWithCacheInvalidation(Long userId, String jobId, JobStatus newStatus) {
        // This method would be called by job processing components
        // to ensure cache consistency when job status changes
        log.debug("Cache invalidated for user {} due to job {} status change to {}",
                userId, jobId, newStatus);
    }

    // =============================================================================
    // BATCH OPERATIONS FOR PERFORMANCE
    // =============================================================================

    /**
     * Get multiple jobs by IDs in batch
     */
    public List<JobSummaryDto> getJobsBatch(List<Long> jobIds) {
        try {
            if (jobIds == null || jobIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<GenerationJob> jobs = optimizedJobRepository.findJobsByIdsBatch(jobIds);
            return convertToJobSummariesBatch(jobs);

        } catch (Exception e) {
            log.error("Failed to get jobs in batch: {}", jobIds, e);
            throw new BusinessException("Failed to retrieve jobs in batch");
        }
    }

    /**
     * Get analytics data with caching
     */
    @Cacheable(value = "hourlyJobStats", key = "#since")
    public List<Object[]> getHourlyJobStatistics(Instant since) {
        try {
            return optimizedJobRepository.getHourlyJobStatistics(since);
        } catch (Exception e) {
            log.error("Failed to get hourly job statistics since: {}", since, e);
            throw new BusinessException("Failed to retrieve hourly statistics");
        }
    }

    // =============================================================================
    // PRIVATE HELPER METHODS
    // =============================================================================

    private void validateUserAccess(Long userId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            throw new BusinessException("Access denied to user jobs");
        }
    }

    private Page<GenerationJob> getOptimizedJobsPage(Long userId, Pageable pageable, JobFilterCriteria criteria) {
        // Choose optimal query based on filter criteria
        if (criteria.hasOnlyStatusFilter()) {
            return optimizedJobRepository.findByUserIdAndStatusOptimized(
                    userId, criteria.getStatuses().iterator().next(), pageable);
        } else if (criteria.hasOnlyContentTypeFilter()) {
            return optimizedJobRepository.findByUserIdAndContentTypeOptimized(
                    userId, criteria.getContentTypes().iterator().next(), pageable);
        } else if (criteria.hasTextSearch()) {
            return optimizedJobRepository.searchJobsByUserOptimized(
                    userId, criteria.getSearchText(), pageable);
        } else {
            return optimizedJobRepository.findByUserIdOptimized(userId, pageable);
        }
    }

    private List<JobSummaryDto> convertToJobSummariesBatch(List<GenerationJob> jobs) {
        return jobs.parallelStream()
                .map(this::convertToJobSummary)
                .collect(Collectors.toList());
    }

    private JobSummaryDto convertToJobSummary(GenerationJob job) {
        return JobSummaryDto.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .contentType(job.getContentType())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .retryCount(job.getRetryCount())
                .executionTimeMs(job.getProcessingTimeMs())
                .canRetry(job.canRetry())
                .canGenerateVideo(job.getStatus() == JobStatus.COMPLETED &&
                        job.getResultContent() != null &&
                        !job.getResultContent().trim().isEmpty())
                .errorMessage(job.getErrorMessage())
                .build();
    }

    private JobDetailsResponse convertToJobDetails(GenerationJob job) {
        return JobDetailsResponse.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .contentType(job.getContentType())
                .status(job.getStatus())
                .priority(job.getPriority())
                .aiProvider(job.getAiProvider())
                .aiModel(job.getAiModel())
                .parameters(job.getRequestParams())
                .result(job.getResultContent())
                .errorMessage(job.getErrorMessage())
                .errorDetails(job.getErrorDetails())
                .executionLogs(generateExecutionLogs(job))
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .executionTimeMs(job.getProcessingTimeMs())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .originalJobId(null)
                .tokensUsed(job.getTokensUsed())
                .generationCost(job.getGenerationCost())
                .metadata(job.getMetadata())
                .build();
    }

    private JobDetailsResponse convertProjectionToJobDetails(Object[] projection, Long jobId) {
        // Convert covering index projection to JobDetailsResponse
        // This avoids loading the full entity for better performance
        return JobDetailsResponse.builder()
                .id((Long) projection[0])
                .jobId((String) projection[1])
                .contentType((String) projection[4])
                .status((JobStatus) projection[3])
                .priority((GenerationJob.JobPriority) projection[5])
                .aiProvider((String) projection[6])
                .aiModel((String) projection[7])
                .createdAt((Instant) projection[8])
                .startedAt((Instant) projection[9])
                .completedAt((Instant) projection[10])
                .executionTimeMs((Long) projection[11])
                .retryCount((Integer) projection[12])
                .maxRetries((Integer) projection[13])
                .tokensUsed((Integer) projection[14])
                .generationCost((java.math.BigDecimal) projection[15])
                .build();
    }

    private JobStatistics convertStatsArrayToJobStatistics(Object[] stats) {
        long totalJobs = ((Number) stats[0]).longValue();
        long completedJobs = ((Number) stats[1]).longValue();
        long failedJobs = ((Number) stats[2]).longValue();
        long processingJobs = ((Number) stats[3]).longValue();
        long queuedJobs = ((Number) stats[4]).longValue();
        long cancelledJobs = ((Number) stats[5]).longValue();

        Double avgProcessingTimeDouble = (Double) stats[6];
        Long averageProcessingTime = avgProcessingTimeDouble != null ? avgProcessingTimeDouble.longValue() : null;

        Double totalProcessingTimeDouble = (Double) stats[7];
        Long totalProcessingTime = totalProcessingTimeDouble != null ? totalProcessingTimeDouble.longValue() : null;

        Long totalTokensUsed = stats.length > 8 ? ((Number) stats[8]).longValue() : null;
        java.math.BigDecimal totalGenerationCost = stats.length > 9 ? (java.math.BigDecimal) stats[9] : null;

        long totalFinishedJobs = completedJobs + failedJobs + cancelledJobs;
        double successRate = totalFinishedJobs > 0 ? (double) completedJobs / totalFinishedJobs : 0.0;

        return JobStatistics.builder()
                .totalJobs(totalJobs)
                .completedJobs(completedJobs)
                .failedJobs(failedJobs)
                .processingJobs(processingJobs)
                .queuedJobs(queuedJobs)
                .cancelledJobs(cancelledJobs)
                .successRate(successRate)
                .averageProcessingTimeMs(averageProcessingTime)
                .totalProcessingTimeMs(totalProcessingTime)
                .totalTokensUsed(totalTokensUsed)
                .totalGenerationCost(totalGenerationCost)
                .build();
    }

    private JobStatistics convertMaterializedViewToStatistics(Object[] mvStats) {
        // Convert materialized view result to JobStatistics
        // Implementation depends on the exact structure of mv_user_job_statistics
        return convertStatsArrayToJobStatistics(mvStats);
    }

    private PaginatedResponse.PaginationMetadata createPaginationMetadata(Page<GenerationJob> page) {
        return PaginatedResponse.PaginationMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    private List<String> generateExecutionLogs(GenerationJob job) {
        List<String> logs = new ArrayList<>();
        logs.add(String.format("Job created at %s", job.getCreatedAt()));

        if (job.getStartedAt() != null) {
            logs.add(String.format("Processing started at %s", job.getStartedAt()));
        }

        if (job.getCompletedAt() != null) {
            logs.add(String.format("Job completed at %s with status: %s",
                    job.getCompletedAt(), job.getStatus()));
        }

        if (job.getRetryCount() > 0) {
            logs.add(String.format("Job has been retried %d times", job.getRetryCount()));
        }

        if (job.getErrorMessage() != null) {
            logs.add(String.format("Error: %s", job.getErrorMessage()));
        }

        return logs;
    }

    private GenerationJob findJobForRetry(Long jobId, Long userId) {
        GenerationJob job = optimizedJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("Job not found: " + jobId));

        if (!job.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to job: " + jobId);
        }

        if (!job.canRetry()) {
            throw new BusinessException("Job cannot be retried. Status: " + job.getStatus() +
                    ", Retry count: " + job.getRetryCount() + "/" + job.getMaxRetries());
        }

        return job;
    }

    private QueueJobRequest createRetryJobRequest(GenerationJob originalJob) {
        return QueueJobRequest.builder()
                .requestParams(originalJob.getRequestParams())
                .priority(originalJob.getPriority())
                .contentType(originalJob.getContentType())
                .maxRetries(originalJob.getMaxRetries())
                .expirationHours(24)
                .metadata(createRetryMetadata(originalJob))
                .build();
    }

    private Map<String, Object> createRetryMetadata(GenerationJob originalJob) {
        Map<String, Object> metadata = new HashMap<>();
        if (originalJob.getMetadata() != null) {
            metadata.putAll(originalJob.getMetadata());
        }
        metadata.put("originalJobId", originalJob.getId());
        metadata.put("retryOf", originalJob.getJobId());
        metadata.put("retryInitiatedAt", Instant.now().toString());
        return metadata;
    }

    @Transactional
    private void updateOriginalJobForRetry(GenerationJob originalJob, String newJobId) {
        log.info("Job {} retried as {}", originalJob.getJobId(), newJobId);
    }

    private Long extractJobIdFromUuid(String jobUuid) {
        return null; // Placeholder implementation
    }
}