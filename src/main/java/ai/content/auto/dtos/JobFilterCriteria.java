package ai.content.auto.dtos;

import java.time.Instant;
import java.util.List;

import ai.content.auto.entity.GenerationJob.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for job search and filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobFilterCriteria {

    private List<JobStatus> statuses;
    private List<String> contentTypes;
    private Instant createdAfter;
    private Instant createdBefore;
    private String searchText;
    private String sortBy;
    private String sortDirection;
    private Long userId;

    /**
     * Check if any filters are applied
     */
    public boolean hasFilters() {
        return (statuses != null && !statuses.isEmpty()) ||
                (contentTypes != null && !contentTypes.isEmpty()) ||
                createdAfter != null ||
                createdBefore != null ||
                (searchText != null && !searchText.trim().isEmpty());
    }

    /**
     * Get search text for database query (trimmed and lowercase)
     */
    public String getSearchTextForQuery() {
        if (searchText == null) {
            return null;
        }
        String trimmed = searchText.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    /**
     * Get sort direction (default to DESC)
     */
    public String getSortDirectionOrDefault() {
        return sortDirection != null ? sortDirection.toUpperCase() : "DESC";
    }

    /**
     * Get sort field (default to createdAt)
     */
    public String getSortByOrDefault() {
        return sortBy != null ? sortBy : "createdAt";
    }

    /**
     * Validate sort field
     */
    public boolean isValidSortField() {
        if (sortBy == null) {
            return true; // Default is valid
        }

        List<String> validFields = List.of(
                "createdAt", "completedAt", "status", "contentType",
                "executionTimeMs", "retryCount");

        return validFields.contains(sortBy);
    }

    /**
     * Validate sort direction
     */
    public boolean isValidSortDirection() {
        if (sortDirection == null) {
            return true; // Default is valid
        }

        return "ASC".equalsIgnoreCase(sortDirection) || "DESC".equalsIgnoreCase(sortDirection);
    }

    /**
     * Returns true when only status filter is applied (no other filters/search).
     */
    public boolean hasOnlyStatusFilter() {
        boolean hasStatus = statuses != null && !statuses.isEmpty();
        boolean noContentType = contentTypes == null || contentTypes.isEmpty();
        boolean noCreatedAfter = createdAfter == null;
        boolean noCreatedBefore = createdBefore == null;
        boolean noSearch = searchText == null || searchText.trim().isEmpty();
        return hasStatus && noContentType && noCreatedAfter && noCreatedBefore && noSearch;
    }

    /**
     * Returns true when only content type filter is applied (no other
     * filters/search).
     */
    public boolean hasOnlyContentTypeFilter() {
        boolean hasContentType = contentTypes != null && !contentTypes.isEmpty();
        boolean noStatus = statuses == null || statuses.isEmpty();
        boolean noCreatedAfter = createdAfter == null;
        boolean noCreatedBefore = createdBefore == null;
        boolean noSearch = searchText == null || searchText.trim().isEmpty();
        return hasContentType && noStatus && noCreatedAfter && noCreatedBefore && noSearch;
    }

    /**
     * Returns true when free-text search is present.
     */
    public boolean hasTextSearch() {
        return searchText != null && !searchText.trim().isEmpty();
    }
}