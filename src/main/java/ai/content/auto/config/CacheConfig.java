package ai.content.auto.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache configuration for job queue management performance optimization
 * Uses Caffeine cache for high-performance in-memory caching
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

        /**
         * Caffeine cache manager for application-wide caching
         * Used as fallback when Redis is not available
         */
        @Bean("caffeineCacheManager")
        public CacheManager caffeineCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager();

                // Configure default cache settings
                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .maximumSize(10_000)
                                .expireAfterWrite(Duration.ofMinutes(30))
                                .expireAfterAccess(Duration.ofMinutes(15))
                                .recordStats()
                                .removalListener(
                                                (key, value, cause) -> log.debug(
                                                                "Cache entry removed: key={}, cause={}", key, cause)));

                // Pre-configure specific caches with optimized settings
                cacheManager.setCacheNames(java.util.Arrays.asList(
                                "userJobs",
                                "userJobsByStatus",
                                "userJobsByContentType",
                                "userJobStatistics",
                                "userContentTypes",
                                "activeJobCount",
                                "hourlyJobStats",
                                "userPerformanceMetrics",
                                "systemStats"));

                log.info("Caffeine cache manager configured with {} caches",
                                cacheManager.getCacheNames().size());

                return cacheManager;
        }

        /**
         * Specialized cache manager for user job data
         * Optimized for frequent access patterns
         */
        @Bean("userJobCacheManager")
        public CacheManager userJobCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager();

                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .maximumSize(5_000) // Smaller size for user-specific data
                                .expireAfterWrite(Duration.ofMinutes(10)) // Shorter expiry for real-time data
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .recordStats());

                cacheManager.setCacheNames(
                                java.util.Arrays.asList("userJobs", "userJobsByStatus", "userJobsByContentType"));

                return cacheManager;
        }

        /**
         * Long-term cache manager for statistics and analytics
         * Optimized for data that changes less frequently
         */
        @Bean("statisticsCacheManager")
        public CacheManager statisticsCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager();

                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .maximumSize(1_000) // Smaller size for statistics
                                .expireAfterWrite(Duration.ofHours(1)) // Longer expiry for statistics
                                .expireAfterAccess(Duration.ofMinutes(30))
                                .recordStats());

                cacheManager.setCacheNames(java.util.Arrays.asList(
                                "userJobStatistics",
                                "userContentTypes",
                                "hourlyJobStats",
                                "userPerformanceMetrics",
                                "systemStats"));

                return cacheManager;
        }

        /**
         * Short-term cache manager for frequently changing data
         * Optimized for real-time operations
         */
        @Bean("realtimeCacheManager")
        public CacheManager realtimeCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager();

                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .maximumSize(2_000)
                                .expireAfterWrite(Duration.ofMinutes(2)) // Very short expiry
                                .expireAfterAccess(Duration.ofMinutes(1))
                                .recordStats());

                cacheManager.setCacheNames(java.util.Arrays.asList("activeJobCount", "queueStatus", "processingJobs"));

                return cacheManager;
        }

        /**
         * Cache configuration for search results
         * Optimized for text search caching
         */
        @Bean("searchCacheManager")
        public CacheManager searchCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager();

                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .maximumSize(1_000) // Moderate size for search results
                                .expireAfterWrite(Duration.ofMinutes(15)) // Medium expiry for search
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .recordStats());

                cacheManager.setCacheNames(java.util.Arrays.asList("searchResults", "contentSearch", "errorSearch"));

                return cacheManager;
        }
}