package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/setting-test")
@RequiredArgsConstructor
@Slf4j
public class SettingTestController {

    private final SecurityUtil securityUtil;

    @GetMapping("/health")
    public ResponseEntity<BaseResponse<Map<String, Object>>> healthCheck() {
        try {
            log.info("Settings health check requested");

            Map<String, Object> healthData = new HashMap<>();
            healthData.put("status", "OK");
            healthData.put("timestamp", System.currentTimeMillis());

            // Test security util
            try {
                Long userId = securityUtil.getCurrentUserId();
                healthData.put("currentUserId", userId);
                healthData.put("userAuthenticated", userId != null);
            } catch (Exception e) {
                log.warn("Error getting current user: {}", e.getMessage());
                healthData.put("currentUserId", null);
                healthData.put("userAuthenticated", false);
                healthData.put("authError", e.getMessage());
            }

            BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                    .setErrorMessage("Settings service is healthy")
                    .setData(healthData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Health check failed", e);

            Map<String, Object> errorData = new HashMap<>();
            errorData.put("status", "ERROR");
            errorData.put("error", e.getMessage());

            BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                    .setErrorMessage("Settings service health check failed")
                    .setData(errorData);

            return ResponseEntity.status(500).body(response);
        }
    }
}