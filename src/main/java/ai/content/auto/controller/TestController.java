package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/public")
    public ResponseEntity<BaseResponse<String>> publicEndpoint() {
        BaseResponse<String> response = new BaseResponse<String>()
                .setData("This is a public endpoint")
                .setErrorMessage("Success");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/protected")
    public ResponseEntity<BaseResponse<Map<String, Object>>> protectedEndpoint(Authentication authentication) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "This is a protected endpoint");
        data.put("user", authentication.getName());
        data.put("authorities", authentication.getAuthorities());

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setData(data)
                .setErrorMessage("Success");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<String>> adminEndpoint() {
        BaseResponse<String> response = new BaseResponse<String>()
                .setData("This is an admin-only endpoint")
                .setErrorMessage("Success");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BaseResponse<String>> userEndpoint() {
        BaseResponse<String> response = new BaseResponse<String>()
                .setData("This is a user endpoint")
                .setErrorMessage("Success");

        return ResponseEntity.ok(response);
    }
}