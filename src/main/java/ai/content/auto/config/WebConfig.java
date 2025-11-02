package ai.content.auto.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import ai.content.auto.security.SecurityCleanupFilter;
import ai.content.auto.util.SecurityUtil;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Create SecurityCleanupFilter bean
     */
    @Bean
    public SecurityCleanupFilter securityCleanupFilter(SecurityUtil securityUtil) {
        return new SecurityCleanupFilter(securityUtil);
    }

    /**
     * Register SecurityCleanupFilter to run after all other filters
     * This ensures ThreadLocal caches are cleaned up at the end of each request
     */
    @Bean
    public FilterRegistrationBean<SecurityCleanupFilter> securityCleanupFilterRegistration(
            SecurityCleanupFilter securityCleanupFilter) {
        FilterRegistrationBean<SecurityCleanupFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(securityCleanupFilter);
        registration.setOrder(Integer.MAX_VALUE); // Run last
        registration.addUrlPatterns("/*");
        return registration;
    }
}