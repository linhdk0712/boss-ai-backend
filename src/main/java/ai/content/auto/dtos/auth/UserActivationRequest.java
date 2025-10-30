package ai.content.auto.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserActivationRequest {
    @NotBlank(message = "Activation token is required")
    private String token;
}