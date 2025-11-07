package ai.content.auto.controller;

import ai.content.auto.dtos.*;
import ai.content.auto.service.TemplateManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/presets")
@RequiredArgsConstructor
@Slf4j
public class PresetController {

    private final TemplateManagementService templateManagementService;

    /**
     * Get user presets
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> getUserPresets() {

        log.info("Getting user presets");

        List<UserPresetDto> presets = templateManagementService.getUserPresets();

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("User presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new user preset
     */
    @PostMapping
    public ResponseEntity<BaseResponse<UserPresetDto>> createPreset(
            @Valid @RequestBody CreatePresetRequest request) {

        log.info("Creating preset: {}", request.getName());

        UserPresetDto preset = templateManagementService.createPreset(request);

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Preset created successfully")
                .setData(preset);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing preset
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<UserPresetDto>> updatePreset(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePresetRequest request) {

        log.info("Updating preset: {}", id);

        UserPresetDto preset = templateManagementService.updatePreset(id, request);

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Preset updated successfully")
                .setData(preset);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a preset
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deletePreset(@PathVariable Long id) {

        log.info("Deleting preset: {}", id);

        templateManagementService.deletePreset(id);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Preset deleted successfully");

        return ResponseEntity.ok(response);
    }
}