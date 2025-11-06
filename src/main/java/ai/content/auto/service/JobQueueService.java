package ai.content.auto.service;

import ai.content.auto.dtos.*;
import ai.content.auto.entity.GenerationJob;
import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.exception.BusinessException;

import ai.content.auto.repository.GenerationJobRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for job queue management operations
 * Handles job listing, filtering, retry, and content download
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final GenerationJobRepository jobRepository;
    private final QueueManagementService queueManagementService;
    private final SecurityUtil securityUtil;

    /**
     * Get jobs by user with filtering and pagination
     */
    public JobListResponse getJobsByUser(Long userId, Pageable pageable, JobFilterCriteria criteria) {
        try {
            // Validate user access
            Long currentUserId = securityUtil.getCurrentUserId();
            if (!userId.equals(currentUserId)) {
                throw new BusinessException("Access denied to user jobs");
            }

            // Set user ID in criteria
            criteria.setUserId(userId);

            // Create specification for filtering
            Specification<GenerationJob> spec = createJobSpecification(criteria);

            // Create pageable with sorting
            Pageable sortedPageable = createSortedPageable(pageable, criteria);

            // Get jobs with filtering
            Page<GenerationJob> jobsPage = jobRepository.findAll(spec, sortedPageable);

            // Convert to summary DTOs
            List<JobSummaryDto> jobSummaries = jobsPage.getContent().stream()
                    .map(this::convertToJobSummary)
                    .collect(Collectors.toList());

            // Create pagination metadata
            PaginatedResponse.PaginationMetadata pagination = PaginatedResponse.PaginationMetadata.builder()
                    .page(jobsPage.getNumber())
                    .size(jobsPage.getSize())
                    .totalElements(jobsPage.getTotalElements())
                    .totalPages(jobsPage.getTotalPages())
                    .first(jobsPage.isFirst())
                    .last(jobsPage.isLast())
                    .hasNext(jobsPage.hasNext())
                    .hasPrevious(jobsPage.hasPrevious())
                    .numberOfElements(jobsPage.getNumberOfElements())
                    .build();

            // Calculate statistics for the user
            JobStatistics statistics = calculateUserJobStatistics(userId);

            return JobListResponse.of(jobSummaries, pagination, statistics);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get jobs for user: {}", userId, e);
            throw new BusinessException("Failed to retrieve user jobs");
        }
    }

    /**
     * Get detailed job information
     */
    public JobDetailsResponse getJobDetails(Long jobId, Long userId) {
        try {
            // Validate user access
            Long currentUserId = securityUtil.getCurrentUserId();
            if (!userId.equals(currentUserId)) {
                throw new BusinessException("Access denied to user jobs");
            }

            // Find job by ID and user
            GenerationJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new BusinessException("Job not found: " + jobId));

            if (!job.getUserId().equals(userId)) {
                throw new BusinessException("Access denied to job: " + jobId);
            }

            // Convert to detailed response
            return convertToJobDetails(job);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get job details: {} for user: {}", jobId, userId, e);
            throw new BusinessException("Failed to retrieve job details");
        }
    }

    /**
     * Retry a failed or cancelled job
     */
    public RetryJobResponse retryJob(Long jobId, Long userId) {
        try {
            // Validate user access
            Long currentUserId = securityUtil.getCurrentUserId();
            if (!userId.equals(currentUserId)) {
                throw new BusinessException("Access denied to user jobs");
            }

            // Find and validate job
            GenerationJob originalJob = findJobForRetry(jobId, userId);

            // Create retry job request
            QueueJobRequest retryRequest = createRetryJobRequest(originalJob);

            // Queue the retry job
            QueueJobResponse queueResponse = queueManagementService.queueJob(retryRequest);

            // Update original job with retry information
            updateOriginalJobForRetry(originalJob, queueResponse.getJobId());

            log.info("Job {} retried successfully as {} for user: {}",
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
            log.error("Failed to retry job: {} for user: {}", jobId, userId, e);
            throw new BusinessException("Failed to retry job");
        }
    }

    /**
     * Get available content types for filtering
     */
    public List<String> getAvailableContentTypes(Long userId) {
        try {
            // Validate user access
            Long currentUserId = securityUtil.getCurrentUserId();
            if (!userId.equals(currentUserId)) {
                throw new BusinessException("Access denied to user data");
            }

            return jobRepository.findDistinctContentTypesByUserId(userId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get content types for user: {}", userId, e);
            throw new BusinessException("Failed to retrieve content types");
        }
    }

    /**
     * Download job content as a file
     */
    public Resource downloadJobContent(Long jobId, Long userId, String format) {
        try {
            // Validate user access
            Long currentUserId = securityUtil.getCurrentUserId();
            if (!userId.equals(currentUserId)) {
                throw new BusinessException("Access denied to user jobs");
            }

            // Find job
            GenerationJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new BusinessException("Job not found: " + jobId));

            if (!job.getUserId().equals(userId)) {
                throw new BusinessException("Access denied to job: " + jobId);
            }

            // Validate job has content
            if (job.getStatus() != JobStatus.COMPLETED ||
                    job.getResultContent() == null ||
                    job.getResultContent().trim().isEmpty()) {
                throw new BusinessException("Job does not have downloadable content");
            }

            // Generate content based on format
            String content = generateDownloadContent(job, format);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            log.info("Generated download content for job: {} in format: {} for user: {}",
                    jobId, format, userId);

            return new ByteArrayResource(contentBytes);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to download job content: {} for user: {}", jobId, userId, e);
            throw new BusinessException("Failed to download job content");
        }
    }

    // Private helper methods

    private Specification<GenerationJob> createJobSpecification(JobFilterCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User filter (always required)
            if (criteria.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), criteria.getUserId()));
            }

            // Status filter
            if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(criteria.getStatuses()));
            }

            // Content type filter
            if (criteria.getContentTypes() != null && !criteria.getContentTypes().isEmpty()) {
                predicates.add(root.get("contentType").in(criteria.getContentTypes()));
            }

            // Date range filters
            if (criteria.getCreatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), criteria.getCreatedAfter()));
            }

            if (criteria.getCreatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), criteria.getCreatedBefore()));
            }

            // Text search
            String searchText = criteria.getSearchTextForQuery();
            if (searchText != null) {
                Predicate contentSearch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("resultContent")), "%" + searchText + "%");
                Predicate errorSearch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("errorMessage")), "%" + searchText + "%");
                Predicate contentTypeSearch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("contentType")), "%" + searchText + "%");

                predicates.add(criteriaBuilder.or(contentSearch, errorSearch, contentTypeSearch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable createSortedPageable(Pageable pageable, JobFilterCriteria criteria) {
        // Validate sort parameters
        if (!criteria.isValidSortField() || !criteria.isValidSortDirection()) {
            // Use default sorting
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        String sortField = criteria.getSortByOrDefault();
        Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.getSortDirectionOrDefault())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(direction, sortField));
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
                .originalJobId(null) // Will be implemented when original_job_id column is added
                .tokensUsed(job.getTokensUsed())
                .generationCost(job.getGenerationCost())
                .metadata(job.getMetadata())
                .build();
    }

    private List<String> generateExecutionLogs(GenerationJob job) {
        List<String> logs = new ArrayList<>();

        logs.add(String.format("Job created at %s",
                formatTimestamp(job.getCreatedAt())));

        if (job.getStartedAt() != null) {
            logs.add(String.format("Processing started at %s",
                    formatTimestamp(job.getStartedAt())));
        }

        if (job.getCompletedAt() != null) {
            logs.add(String.format("Job completed at %s with status: %s",
                    formatTimestamp(job.getCompletedAt()), job.getStatus()));
        }

        if (job.getRetryCount() > 0) {
            logs.add(String.format("Job has been retried %d times", job.getRetryCount()));
        }

        if (job.getErrorMessage() != null) {
            logs.add(String.format("Error: %s", job.getErrorMessage()));
        }

        return logs;
    }

    private String formatTimestamp(Instant timestamp) {
        return DateTimeFormatter.ISO_INSTANT.format(timestamp);
    }

    private JobStatistics calculateUserJobStatistics(Long userId) {
        // Get comprehensive statistics using single query
        Object[] stats = jobRepository.getUserJobStatistics(userId);

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

        // Calculate success rate
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
                .build();
    }

    private GenerationJob findJobForRetry(Long jobId, Long userId) {
        GenerationJob job = jobRepository.findById(jobId)
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
                .expirationHours(24) // Default 24 hours
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
        // This would update the original_job_id field when the database migration is
        // applied
        // For now, we just log the retry relationship
        log.info("Job {} retried as {}", originalJob.getJobId(), newJobId);
    }

    private Long extractJobIdFromUuid(String jobUuid) {
        // This is a placeholder - in reality, you'd need to query the database
        // to get the actual ID from the UUID
        return null;
    }

    private String generateDownloadContent(GenerationJob job, String format) {
        StringBuilder content = new StringBuilder();

        switch (format.toLowerCase()) {
            case "txt":
                // Add metadata header for text files
                content.append("# Generated Content\n");
                content.append("# Job ID: ").append(job.getJobId()).append("\n");
                content.append("# Content Type: ").append(job.getContentType()).append("\n");
                content.append("# Created: ").append(job.getCreatedAt()).append("\n");
                content.append("# Completed: ").append(job.getCompletedAt()).append("\n");
                if (job.getTokensUsed() != null) {
                    content.append("# Tokens Used: ").append(job.getTokensUsed()).append("\n");
                }
                if (job.getProcessingTimeMs() != null) {
                    content.append("# Processing Time: ").append(job.getProcessingTimeMs()).append("ms\n");
                }
                content.append("\n---\n\n");
                content.append(job.getResultContent());
                break;

            case "json":
                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("jobId", job.getJobId());
                jsonData.put("contentType", job.getContentType());
                jsonData.put("status", job.getStatus().toString());
                jsonData.put("createdAt", job.getCreatedAt().toString());
                jsonData.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
                jsonData.put("processingTimeMs", job.getProcessingTimeMs());
                jsonData.put("tokensUsed", job.getTokensUsed());
                jsonData.put("generationCost", job.getGenerationCost());
                jsonData.put("retryCount", job.getRetryCount());
                jsonData.put("aiProvider", job.getAiProvider());
                jsonData.put("aiModel", job.getAiModel());
                jsonData.put("content", job.getResultContent());

                // Add parameters if available
                if (job.getRequestParams() != null) {
                    jsonData.put("parameters", job.getRequestParams());
                }

                // Add metadata if available
                if (job.getMetadata() != null) {
                    jsonData.put("metadata", job.getMetadata());
                }

                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    content.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData));
                } catch (Exception e) {
                    log.error("Failed to serialize job data to JSON", e);
                    // Fallback to simple JSON
                    content.append("{\n");
                    content.append("  \"jobId\": \"").append(job.getJobId()).append("\",\n");
                    content.append("  \"contentType\": \"").append(job.getContentType()).append("\",\n");
                    content.append("  \"createdAt\": \"").append(job.getCreatedAt()).append("\",\n");
                    content.append("  \"completedAt\": \"").append(job.getCompletedAt()).append("\",\n");
                    content.append("  \"content\": \"").append(escapeJson(job.getResultContent())).append("\"\n");
                    content.append("}");
                }
                break;

            default:
                // Default to plain text
                content.append(job.getResultContent());
                break;
        }

        return content.toString();
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}