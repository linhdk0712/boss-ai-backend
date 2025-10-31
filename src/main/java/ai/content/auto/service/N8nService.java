package ai.content.auto.service;

import ai.content.auto.dtos.ContentWorkflowRequest;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.util.StringUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class N8nService {

    private final RestTemplate restTemplate;

    private static final String N8N_WEBHOOK_URL = "https://bossai.com.vn/webhook-test/a1fb6ce9-8106-437a-8565-bd40b3d06b9e";

    public Map<String, Object> triggerWorkflow(ContentWorkflowRequest request, Long userId) {
        try {
            // 1. Validate input
            validateWorkflowRequest(request, userId);

            log.info("Triggering N8N workflow for user: {}", userId);

            // 2. Build workflow request
            Map<String, Object> workflowRequest = buildWorkflowRequest(request, userId);

            // 3. Call N8N webhook
            Map<String, Object> webhookResponse = callN8nWebhook(workflowRequest);

            // 4. Build result
            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("message", "Workflow triggered successfully");
            result.put("workflowResponse", webhookResponse);

            log.info("N8N workflow triggered successfully for user: {}", userId);
            return result;

        } catch (BusinessException e) {
            log.error("Business error triggering N8N workflow for user: {}", userId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error triggering N8N workflow for user: {}", userId, e);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "FAILED");
            result.put("message", "Failed to trigger workflow: " + e.getMessage());

            return result;
        }
    }

    private void validateWorkflowRequest(ContentWorkflowRequest request, Long userId) {
        if (request == null) {
            throw new BusinessException("Workflow request is required");
        }
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }
        if (StringUtil.isBlank(request.getGeneratedContent())) {
            throw new BusinessException("Generated content is required");
        }
    }

    private Map<String, Object> callN8nWebhook(Map<String, Object> workflowRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                N8N_WEBHOOK_URL,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            responseBody = new HashMap<>();
            responseBody.put("status", "success");
            responseBody.put("message", "Webhook called successfully");
        }

        return responseBody;
    }

    private Map<String, Object> buildWorkflowRequest(ContentWorkflowRequest request, Long userId) {
        Map<String, Object> workflowRequest = new HashMap<>();
        workflowRequest.put("userId", userId);
        workflowRequest.put("generatedContent", request.getGeneratedContent());
        workflowRequest.put("title", request.getTitle());
        workflowRequest.put("contentType", request.getContentType());
        workflowRequest.put("industry", request.getIndustry());
        workflowRequest.put("targetAudience", request.getTargetAudience());
        workflowRequest.put("tone", request.getTone());
        workflowRequest.put("language", request.getLanguage());
        workflowRequest.put("prompt", request.getPrompt());

        return workflowRequest;
    }
}