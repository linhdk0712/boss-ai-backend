package ai.content.auto.controller;

import ai.content.auto.annotation.Loggable;
import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.auth.*;
import ai.content.auto.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Loggable
  @PostMapping("/login")
  public ResponseEntity<BaseResponse<AuthResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    log.info("Login attempt for username: {}", request.getUsername());

    AuthResponse authResponse = authService.login(request);
    BaseResponse<AuthResponse> response = new BaseResponse<AuthResponse>()
        .setErrorMessage("Login successful")
        .setData(authResponse);

    return ResponseEntity.ok(response);
  }

  @Loggable
  @PostMapping("/register")
  public ResponseEntity<BaseResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
    log.info("Registration attempt for username: {}", request.getUsername());

    authService.register(request);
    BaseResponse<Void> response = new BaseResponse<Void>()
        .setErrorMessage("Registration successful. Please check your email to activate your account.");

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Loggable
  @PostMapping("/user-active")
  public ResponseEntity<BaseResponse<Void>> activateUser(
      @Valid @RequestBody UserActivationRequest request) {
    log.info("User activation attempt with token");

    authService.activateUser(request);
    BaseResponse<Void> response = new BaseResponse<Void>()
        .setErrorMessage("Account activated successfully. You can now login.");

    return ResponseEntity.ok(response);
  }

  @Loggable
  @PostMapping("/refresh")
  public ResponseEntity<BaseResponse<AuthResponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request) {
    log.info("Token refresh attempt");

    AuthResponse authResponse = authService.refreshToken(request);
    BaseResponse<AuthResponse> response = new BaseResponse<AuthResponse>()
        .setData(authResponse)
        .setErrorMessage("Token refreshed successfully");

    return ResponseEntity.ok(response);
  }
}