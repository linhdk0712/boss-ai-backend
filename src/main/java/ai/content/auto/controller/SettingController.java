package ai.content.auto.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.VUserConfigDto;
import ai.content.auto.service.VUserConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/setting")
@RequiredArgsConstructor
@Slf4j
public class SettingController {
        private final VUserConfigService vUserConfigService;

        @GetMapping("/tone")
        public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getTone() {
                log.info("Fetching tone settings for current user");

                List<VUserConfigDto> vUserConfigDtoList = vUserConfigService
                                .findAllByCategory(ContentConstants.CATEGORY_TONE);
                BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<List<VUserConfigDto>>()
                                .setErrorMessage("Tone settings retrieved successfully")
                                .setData(vUserConfigDtoList);

                return ResponseEntity.ok(baseResponse);
        }

        @GetMapping("/industry")
        public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getIndustry() {
                log.info("Fetching industry settings for current user");

                List<VUserConfigDto> vUserConfigDtoList = vUserConfigService
                                .findAllByCategory(ContentConstants.CATEGORY_INDUSTRY);
                BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<List<VUserConfigDto>>()
                                .setErrorMessage("Industry settings retrieved successfully")
                                .setData(vUserConfigDtoList);

                return ResponseEntity.ok(baseResponse);
        }

        @GetMapping("/language")
        public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getLanguage() {
                log.info("Fetching language settings for current user");

                List<VUserConfigDto> vUserConfigDtoList = vUserConfigService
                                .findAllByCategory(ContentConstants.CATEGORY_LANGUAGE);
                BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<List<VUserConfigDto>>()
                                .setErrorMessage("Language settings retrieved successfully")
                                .setData(vUserConfigDtoList);

                return ResponseEntity.ok(baseResponse);
        }

        @GetMapping("/target-audience")
        public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getTargetAudience() {
                log.info("Fetching target audience settings for current user");

                List<VUserConfigDto> vUserConfigDtoList = vUserConfigService
                                .findAllByCategory(ContentConstants.CATEGORY_TARGET_AUDIENCE);
                BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<List<VUserConfigDto>>()
                                .setErrorMessage("Target audience settings retrieved successfully")
                                .setData(vUserConfigDtoList);

                return ResponseEntity.ok(baseResponse);
        }

        @GetMapping("/content-type")
        public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getContentType() {
                log.info("Fetching content type settings for current user");

                List<VUserConfigDto> vUserConfigDtoList = vUserConfigService
                                .findAllByCategory(ContentConstants.CATEGORY_CONTENT_TYPE);
                BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<List<VUserConfigDto>>()
                                .setErrorMessage("Content type settings retrieved successfully")
                                .setData(vUserConfigDtoList);

                return ResponseEntity.ok(baseResponse);
        }

        @PostMapping
        public ResponseEntity<BaseResponse<List<VUserConfigDto>>> update(
                        @Valid @RequestBody VUserConfigDto vUserConfigDto) {
                log.info("Updating setting: {} for category: {}", vUserConfigDto.id(), vUserConfigDto.category());

                vUserConfigService.updateConfig(vUserConfigDto);
                List<VUserConfigDto> vUserConfigDtoList = vUserConfigService
                                .findAllByCategory(vUserConfigDto.category());

                BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<List<VUserConfigDto>>()
                                .setErrorMessage("Setting updated successfully")
                                .setData(vUserConfigDtoList);

                return ResponseEntity.ok(baseResponse);
        }

}
