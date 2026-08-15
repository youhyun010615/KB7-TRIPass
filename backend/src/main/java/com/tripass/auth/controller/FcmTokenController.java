package com.tripass.auth.controller;

import com.tripass.auth.dto.request.FcmTokenRequestDto;
import com.tripass.auth.service.FcmTokenService;
import com.tripass.common.response.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "AUTH - FCM 토큰")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @ApiOperation(
            value = "FCM 토큰 등록/갱신",
            notes = "로그인한 사용자의 FCM 디바이스 토큰을 등록하거나 갱신합니다."
    )
    @PostMapping("/tokens")
    public ApiResponse<Void> registerToken(
            @ApiIgnore Authentication authentication,
            @ApiParam(value = "FCM 토큰 등록 정보", required = true)
            @RequestBody FcmTokenRequestDto requestDto
    ) {
        Long userId = (Long) authentication.getPrincipal();
        fcmTokenService.registerToken(userId, requestDto);
        return ApiResponse.success("FCM 토큰이 등록되었습니다.", null);
    }
}
