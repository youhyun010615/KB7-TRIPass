package com.tripass.mypage.controller;
import com.tripass.common.response.ApiResponse;
import com.tripass.common.util.FcmService;
import com.tripass.mypage.domain.NotificationSetting;
import com.tripass.mypage.dto.response.NotificationResponseDto;
import com.tripass.mypage.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@Api(tags = "MYPAGE - 알림")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final FcmService fcmService;

    @ApiOperation(value = "알림 목록 조회", notes = "사용자의 알림 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<NotificationResponseDto>> getNotifications(@ApiIgnore Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(notificationService.getNotifications(userId));
    }

    @ApiOperation(value = "알림 단건 읽음 처리", notes = "알림을 읽음 처리합니다.")
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@ApiIgnore Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(userId, id);
        return ApiResponse.success("읽음 처리되었습니다.", null);
    }

    @ApiOperation(value = "알림 전체 읽음 처리", notes = "모든 알림을 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@ApiIgnore Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAllAsRead(userId);
        return ApiResponse.success("모든 알림이 읽음 처리되었습니다.", null);
    }

    @ApiOperation(value = "알림 설정 조회", notes = "사용자의 알림 설정을 조회합니다.")
    @GetMapping("/settings")
    public ApiResponse<NotificationSetting> getSettings(@ApiIgnore Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(notificationService.getSettings(userId));
    }

    @ApiOperation(value = "알림 설정 수정", notes = "사용자의 알림 설정을 수정합니다.")
    @PutMapping("/settings")
    public ApiResponse<Void> updateSettings(@ApiIgnore Authentication authentication, @RequestBody NotificationSetting setting) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.updateSettings(userId, setting);
        return ApiResponse.success("알림 설정이 수정되었습니다.", null);
    }

    @ApiOperation(value = "테스트 알림 발송 (데이터 저장 포함)", notes = "로그인한 사용자에게 즉시 테스트 알림 발송 및 DB 저장")
    @PostMapping("/test-send-persistent")
    public ApiResponse<Void> testSendPersistent(@ApiIgnore Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String title = "테스트 알림";
        String body = "알림 설정이 정상입니다! (상세 일정으로 이동)";
        String url = "/schedule"; // 테스트용 URL 추가
        
        // 1. DB에 알림 저장
        notificationService.insertNotification(userId, "TEST", title, body, url);
        
        // 2. FCM 발송
        fcmService.sendNotification(userId, title, body);
        
        return ApiResponse.success("알림 발송 및 DB 저장 완료 (URL: /schedule)", null);
    }
}
