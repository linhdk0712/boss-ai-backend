package ai.content.auto.repository;

import ai.content.auto.entity.GenerationJob;
import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.entity.GenerationJob.JobPriority;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Optimized repository for GenerationJob entity with performance enhancements
 * Includes caching, optimized queries, and batch operations
 */
@Repository
public interface OptimizedGenerationJobRepository
        extends JpaRepository<GenerationJob, Long>, JpaSpecificationExecutor<GenerationJob> {

    // =============================================================================
    // OPTIMIZED USER JOB QUERIES WITH CACHING
    // =============================================================================

    /**
     * Find jobs by user with optimized composite index usage
     * Uses idx_generation_jobs_user_status_created index
     */
    @Query("SELECT j FROM GenerationJob j WHERE j.userId = :userId " +
            "ORDER BY j.createdAt DESC")
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "userJobs"),
            @QueryHint(name = "org.hibernate.readOnly", value = "true")
    })
    Page<GenerationJob> findByUserIdOptimized(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find jobs by user and status with optimized index
     * Uses idx_generation_jobs_user_status_created index
     */
    @Query("SELECT j FROM GenerationJob j WHERE j.userId = :userId AND j.status = :status " +
            "ORDER BY j.createdAt DESC")
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "userJobsByStatus")
    })
    Page<GenerationJob> findByUserIdAndStatusOptimized(
            @Param("userId") Long userId,
            @Param("status") JobStatus status,
            Pageable pageable);

    /**
     * Find jobs by user and content type with optimized index
     * Uses idx_generation_jobs_user_content_type_created index
     */
    @Query("SELECT j FROM GenerationJob j WHERE j.userId = :userId AND j.contentType = :contentType " +
            "ORDER BY j.createdAt DESC")
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "userJobsByContentType")
    })
    Page<GenerationJob> findByUserIdAndContentTypeOptimized(
            @Param("userId") Long userId,
            @Param("contentType") String contentType,
            Pageable pageable);

    // =============================================================================
    // OPTIMIZED TEXT SEARCH QUERIES
    // =============================================================================

    /**
     * Full-text search using GIN indexes for better performance
     * Uses idx_generation_jobs_combined_search GIN index
     */
    @Query(value = "SELECT * FROM generation_jobs j WHERE j.user_id = :userId " +
            "AND to_tsvector('english', " +
            "    COALESCE(j.content_type, '') || ' ' || " +
            "    COALESCE(j.result_content, '') || ' ' || " +
            "    COALESCE(j.error_message, '')) " +
            "@@ plainto_tsquery('english', :searchText) " +
            "ORDER BY j.created_at DESC", nativeQuery = true)
    Page<GenerationJob> searchJobsByUserOptimized(
            @Param("userId") Long userId,
            @Param("searchText") String searchText,
            Pageable pageable);

    /**
     * Content-only search with ranking for relevance
     * Uses idx_generation_jobs_content_search GIN index
     */
    @Query(value = "SELECT *, ts_rank(to_tsvector('english', COALESCE(result_content, '')), " +
            "                  plainto_tsquery('english', :searchText)) as rank " +
            "FROM generation_jobs j WHERE j.user_id = :userId " +
            "AND to_tsvector('english', COALESCE(result_content, '')) " +
            "@@ plainto_tsquery('english', :searchText) " +
            "ORDER BY rank DESC, j.created_at DESC", nativeQuery = true)
    Page<GenerationJob> searchJobContentByUserOptimized(
            @Param("userId") Long userId,
            @Param("searchText") String searchText,
            Pageable pageable);

    // =============================================================================
    // CACHED STATISTICS QUERIES
    // =============================================================================

    /**
     * Get user job statistics from materialized view for better performance
     */
    @Query(value = "SELECT * FROM mv_user_job_statistics WHERE user_id = :userId", nativeQuery = true)
    @Cacheable(value = "userJobStatistics", key = "#userId")
    Optional<Object[]> getUserJobStatisticsFromMV(@Param("userId") Long userId);

    /**
     * Get user job statistics with fallback to real-time calculation
     */
    @Query("SELECT " +
            "COUNT(j) as totalJobs, " +
            "COUNT(CASE WHEN j.status = 'COMPLETED' THEN 1 END) as completedJobs, " +
            "COUNT(CASE WHEN j.status = 'FAILED' THEN 1 END) as failedJobs, " +
            "COUNT(CASE WHEN j.status = 'PROCESSING' THEN 1 END) as processingJobs, " +
            "COUNT(CASE WHEN j.status = 'QUEUED' THEN 1 END) as queuedJobs, " +
            "COUNT(CASE WHEN j.status = 'CANCELLED' THEN 1 END) as cancelledJobs, " +
            "AVG(CASE WHEN j.processingTimeMs IS NOT NULL THEN j.processingTimeMs END) as avgProcessingTime, " +
            "SUM(CASE WHEN j.processingTimeMs IS NOT NULL THEN j.processingTimeMs ELSE 0 END) as totalProcessingTime, "
            +
            "SUM(COALESCE(j.tokensUsed, 0)) as totalTokensUsed, " +
            "SUM(COALESCE(j.generationCost, 0)) as totalGenerationCost " +
            "FROM GenerationJob j WHERE j.userId = :userId")
    @Cacheable(value = "userJobStatistics", key = "#userId", unless = "#result == null")
    Object[] getUserJobStatisticsOptimized(@Param("userId") Long userId);

    /**
     * Get distinct content types for user with caching
     */
    @Query("SELECT DISTINCT j.contentType FROM GenerationJob j WHERE j.userId = :userId " +
            "AND j.contentType IS NOT NULL ORDER BY j.contentType")
    @Cacheable(value = "userContentTypes", key = "#userId")
    List<String> findDistinctContentTypesByUserIdCached(@Param("userId") Long userId);

    // =============================================================================
    // OPTIMIZED QUEUE PROCESSING QUERIES
    // =============================================================================

    /**
     * Find next jobs to process with optimized index usage
     * Uses idx_generation_jobs_queue_processing partial index
     */
    @Query("SELECT j FROM GenerationJob j WHERE j.status = :status " +
            "ORDER BY j.priority ASC, j.createdAt ASC")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "50")
    })
    List<GenerationJob> findNextJobsToProcessOptimized(
            @Param("status") JobStatus status,
            Pageable pageable);

    /**
     * Find jobs ready for retry with optimized index
     * Uses idx_generation_jobs_retry_ready partial index
     */
    @Query("SELECT j FROM GenerationJob j WHERE j.status = :status " +
            "AND j.nextRetryAt <= :now AND j.retryCount < j.maxRetries " +
            "ORDER BY j.priority ASC, j.nextRetryAt ASC")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "20")
    })
    List<GenerationJob> findJobsReadyForRetryOptimized(
            @Param("status") JobStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    // =============================================================================
    // BATCH OPERATIONS FOR BETTER PERFORMANCE
    // =============================================================================

    /**
     * Find jobs in batch for processing
     * Optimized for bulk operations
     */
    @Query("SELECT j FROM GenerationJob j WHERE j.id IN :jobIds")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "100")
    })
    List<GenerationJob> findJobsByIdsBatch(@Param("jobIds") List<Long> jobIds);

    /**
     * Count active jobs by user with optimized index
     * Uses idx_generation_jobs_active_by_user partial index
     */
    @Query("SELECT COUNT(j) FROM GenerationJob j WHERE j.userId = :userId " +
            "AND j.status IN ('QUEUED', 'PROCESSING')")
    @Cacheable(value = "activeJobCount", key = "#userId")
    long countActiveJobsByUserOptimized(@Param("userId") Long userId);

    // =============================================================================
    // OPTIMIZED MAINTENANCE QUERIES
    // =============================================================================

    /**
     * Find expired jobs for cleanup with optimized index
     * Uses idx_generation_jobs_expired partial index
     */
    @Query("SELECT j FROM GenerationJob j WHERE j.expiresAt <= :now")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "100")
    })
    List<GenerationJob> findExpiredJobsOptimized(@Param("now") Instant now);

    /**
     * Find timed out jobs with optimized index
     * Uses idx_generation_jobs_timeout_detection partial index
     */
    @Query("SELECT j FROM GenerationJob j WHERE j.status = :status " +
            "AND j.startedAt <= :timeoutThreshold")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "50")
    })
    List<GenerationJob> findTimedOutJobsOptimized(
            @Param("status") JobStatus status,
            @Param("timeoutThreshold") Instant timeoutThreshold);

    /**
     * Find old completed jobs for cleanup with optimized index
     * Uses idx_generation_jobs_cleanup_age index
     */
    @Query("SELECT j.id FROM GenerationJob j WHERE j.status IN :statuses " +
            "AND j.completedAt <= :cutoffTime")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "1000")
    })
    List<Long> findOldJobIdsForCleanup(
            @Param("statuses") List<JobStatus> statuses,
            @Param("cutoffTime") Instant cutoffTime);

    // =============================================================================
    // OPTIMIZED ANALYTICS QUERIES
    // =============================================================================

    /**
     * Get hourly job statistics for analytics
     * Uses idx_generation_jobs_time_analytics index
     */
    @Query(value = "SELECT " +
            "DATE_TRUNC('hour', created_at) as hour, " +
            "status, " +
            "COUNT(*) as job_count, " +
            "AVG(processing_time_ms) as avg_processing_time, " +
            "SUM(COALESCE(tokens_used, 0)) as total_tokens " +
            "FROM generation_jobs " +
            "WHERE created_at >= :since " +
            "GROUP BY DATE_TRUNC('hour', created_at), status " +
            "ORDER BY hour DESC", nativeQuery = true)
    @Cacheable(value = "hourlyJobStats", key = "#since")
    List<Object[]> getHourlyJobStatistics(@Param("since") Instant since);

    /**
     * Get user performance metrics
     */
    @Query("SELECT " +
            "j.userId, " +
            "COUNT(j) as totalJobs, " +
            "AVG(j.processingTimeMs) as avgProcessingTime, " +
            "SUM(COALESCE(j.tokensUsed, 0)) as totalTokens, " +
            "COUNT(CASE WHEN j.status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(j) as successRate " +
            "FROM GenerationJob j " +
            "WHERE j.createdAt >= :since " +
            "GROUP BY j.userId " +
            "HAVING COUNT(j) >= :minJobs " +
            "ORDER BY totalJobs DESC")
    @Cacheable(value = "userPerformanceMetrics", key = "#since + '_' + #minJobs")
    List<Object[]> getUserPerformanceMetrics(
            @Param("since") Instant since,
            @Param("minJobs") long minJobs);

    // =============================================================================
    // COVERING INDEX OPTIMIZED QUERIES
    // =============================================================================

    /**
     * Get job list with covering index optimization
     * Uses idx_generation_jobs_list_covering index
     */
    @Query("SELECT j.id, j.jobId, j.contentType, j.status, j.createdAt, j.completedAt, " +
            "j.retryCount, j.processingTimeMs, j.errorMessage " +
            "FROM GenerationJob j WHERE j.userId = :userId " +
            "ORDER BY j.createdAt DESC")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
    Page<Object[]> findJobListProjectionByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Get job details with covering index optimization
     * Uses idx_generation_jobs_details_covering index
     */
    @Query("SELECT j.id, j.jobId, j.userId, j.status, j.contentType, j.priority, " +
            "j.aiProvider, j.aiModel, j.createdAt, j.startedAt, j.completedAt, " +
            "j.processingTimeMs, j.retryCount, j.maxRetries, j.tokensUsed, j.generationCost " +
            "FROM GenerationJob j WHERE j.id = :jobId")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
    Optional<Object[]> findJobDetailsProjection(@Param("jobId") Long jobId);
}