package ai.content.auto.controller;

import ai.content.auto.dtos.*;
import ai.content.auto.entity.GenerationJob;
import ai.content.auto.service.JobQueueService;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for job queue management operations
 * Provides endpoints for job listing, filtering, retry, and content download
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobQueueController {

    private final JobQueueService jobQueueService;
    private final SecurityUtil securityUtil;

    /**
     * Get jobs with pagination and filtering
     * GET
     * /api/v1/jobs?page=0&size=10&status=COMPLETED&contentType=blog&search=keyword
     */
    @GetMapping
    public ResponseEntity<BaseResponse<JobListResponse>> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String createdAfter,
            @RequestParam(required = false) String createdBefore,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Getting jobs - page: {}, size: {}, status: {}, contentType: {}, search: {}",
                page, size, status, contentType, search);

        // Get current user
        Long userId = securityUtil.getCurrentUserId();

        // Create pageable
        Pageable pageable = PageRequest.of(page, size);

        // Build filter criteria
        JobFilterCriteria criteria = buildFilterCriteria(
                status, contentType, createdAfter, createdBefore, search, sortBy, sortDirection);

        // Get jobs
        JobListResponse jobList = jobQueueService.getJobsByUser(userId, pageable, criteria);

        BaseResponse<JobListResponse> response = new BaseResponse<JobListResponse>()
                .setErrorMessage("Jobs retrieved successfully")
                .setData(jobList);

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed job information
     * GET /api/v1/jobs/{id}/details
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<BaseResponse<JobDetailsResponse>> getJobDetails(@PathVariable Long id) {

        log.info("Getting job details for job: {}", id);

        Long userId = securityUtil.getCurrentUserId();
        JobDetailsResponse jobDetails = jobQueueService.getJobDetails(id, userId);

        BaseResponse<JobDetailsResponse> response = new BaseResponse<JobDetailsResponse>()
                .setErrorMessage("Job details retrieved successfully")
                .setData(jobDetails);

        return ResponseEntity.ok(response);
    }

    /**
     * Retry a failed or cancelled job
     * POST /api/v1/jobs/{id}/retry
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<BaseResponse<RetryJobResponse>> retryJob(@PathVariable Long id) {

        log.info("Retrying job: {}", id);

        Long userId = securityUtil.getCurrentUserId();
        RetryJobResponse retryResponse = jobQueueService.retryJob(id, userId);

        BaseResponse<RetryJobResponse> response = new BaseResponse<RetryJobResponse>()
                .setErrorMessage("Job retry initiated successfully")
                .setData(retryResponse);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get available content types for filtering
     * GET /api/v1/jobs/content-types
     */
    @GetMapping("/content-types")
    public ResponseEntity<BaseResponse<List<String>>> getAvailableContentTypes() {

        log.info("Getting available content types for filtering");

        Long userId = securityUtil.getCurrentUserId();
        List<String> contentTypes = jobQueueService.getAvailableContentTypes(userId);

        BaseResponse<List<String>> response = new BaseResponse<List<String>>()
                .setErrorMessage("Content types retrieved successfully")
                .setData(contentTypes);

        return ResponseEntity.ok(response);
    }

    /**
     * Download job content
     * GET /api/v1/jobs/{id}/download?format=txt
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadJobContent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "txt") String format) {

        log.info("Downloading content for job: {} in format: {}", id, format);

        Long userId = securityUtil.getCurrentUserId();
        Resource resource = jobQueueService.downloadJobContent(id, userId, format);

        // Determine content type and filename
        String contentType = getContentType(format);
        String filename = generateFilename(id, format);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    // Private helper methods

    private JobFilterCriteria buildFilterCriteria(String status, String contentType,
            String createdAfter, String createdBefore,
            String search, String sortBy, String sortDirection) {
        JobFilterCriteria.JobFilterCriteriaBuilder builder = JobFilterCriteria.builder();

        // Parse status filter
        if (status != null && !status.trim().isEmpty()) {
            try {
                List<GenerationJob.JobStatus> statuses = Arrays.stream(status.split(","))
                        .map(String::trim)
                        .map(s -> GenerationJob.JobStatus.valueOf(s.toUpperCase()))
                        .collect(Collectors.toList());
                builder.statuses(statuses);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", status);
                // Continue without status filter
            }
        }

        // Parse content type filter
        if (contentType != null && !contentType.trim().isEmpty()) {
            List<String> contentTypes = Arrays.stream(contentType.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            builder.contentTypes(contentTypes);
        }

        // Parse date filters
        if (createdAfter != null && !createdAfter.trim().isEmpty()) {
            try {
                builder.createdAfter(Instant.parse(createdAfter));
            } catch (DateTimeParseException e) {
                log.warn("Invalid createdAfter date format: {}", createdAfter);
                // Continue without date filter
            }
        }

        if (createdBefore != null && !createdBefore.trim().isEmpty()) {
            try {
                builder.createdBefore(Instant.parse(createdBefore));
            } catch (DateTimeParseException e) {
                log.warn("Invalid createdBefore date format: {}", createdBefore);
                // Continue without date filter
            }
        }

        // Set search text
        if (search != null && !search.trim().isEmpty()) {
            builder.searchText(search.trim());
        }

        // Set sorting
        builder.sortBy(sortBy);
        builder.sortDirection(sortDirection);

        return builder.build();
    }

    private String getContentType(String format) {
        switch (format.toLowerCase()) {
            case "json":
                return "application/json";
            case "pdf":
                return "application/pdf";
            case "txt":
            default:
                return "text/plain";
        }
    }

    private String generateFilename(Long jobId, String format) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
        return String.format("job_%d_content_%s.%s", jobId, timestamp, format.toLowerCase());
    }
}