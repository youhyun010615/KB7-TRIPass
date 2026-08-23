package com.tripass.dev.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.dev.mapper.DevDateMapper;
import com.tripass.dev.service.DevResetService;
import com.tripass.dev.util.DevDateUtil;
import com.tripass.travel.mapper.TravelMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Api(tags = "dev-개발용")
@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevController {

    private final DevResetService devResetService;
    private final DevDateMapper devDateMapper;
    private final DevDateUtil devDateUtil;
    private final TravelMapper travelMapper;

    @ApiOperation(value = "계정 데이터 초기화")
    @PostMapping("/reset-account")
    public ApiResponse<Void> resetAccount(
            @ApiIgnore Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        devResetService.resetAccount(userId);
        return ApiResponse.success("계정 데이터가 초기화되었습니다.", null);
    }

    @ApiOperation(value = "가상 날짜 설정", notes = "date 파라미터로 가상 날짜를 설정합니다. 비즈니스 로직에만 영향을 주고, 외부 API(환율·Codef·JWT)에는 영향 없습니다.")
    @PostMapping("/override-date")
    public ApiResponse<Map<String, Object>> overrideDate(
            @ApiIgnore Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();
        devDateMapper.updateOverrideDate(userId, date);

        if (!devDateMapper.selectDemoRecordingMode(userId)) {
            devResetService.resetDemoState(userId, date);
        }

        travelMapper.syncTripRevertToPlanning(userId, date);
        travelMapper.syncTripRevertToTraveling(userId, date);
        travelMapper.syncTripStatusForUser(userId, date);
        travelMapper.syncTripEndedForUser(userId, date);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overrideDate", date.toString());
        result.put("realDate", LocalDate.now().toString());
        return ApiResponse.success("가상 날짜가 설정되었습니다.", result);
    }

    @ApiOperation(value = "가상 날짜 해제")
    @DeleteMapping("/override-date")
    public ApiResponse<Void> clearOverrideDate(
            @ApiIgnore Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        devDateMapper.clearOverrideDate(userId);
        return ApiResponse.success("가상 날짜가 해제되었습니다.", null);
    }

    @ApiOperation(value = "현재 적용 중인 날짜 조회")
    @GetMapping("/current-date")
    public ApiResponse<Map<String, Object>> getCurrentDate(
            @ApiIgnore Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        LocalDate effectiveDate = devDateUtil.today(userId);
        LocalDate overrideDate = devDateMapper.selectOverrideDate(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("effectiveDate", effectiveDate.toString());
        result.put("realDate", LocalDate.now().toString());
        result.put("overrideDate", overrideDate != null ? overrideDate.toString() : null);
        result.put("isOverridden", overrideDate != null);
        result.put("demoRecordingMode", devDateMapper.selectDemoRecordingMode(userId));
        return ApiResponse.success(null, result);
    }

    @ApiOperation(value = "데모 녹화 모드 설정", notes = "켜져 있으면 가상 날짜를 전환해도 자동 리셋을 하지 않는다. " +
            "라이브로 넣은 저축/충전 기록이 이후 날짜에서도 계속 누적되어 보이므로, 끊김 없는 시연 녹화에 사용한다. " +
            "QA처럼 여러 명이 같은 계정을 쓸 때는 꺼두는 것(기본값)을 권장한다.")
    @PostMapping("/demo-mode")
    public ApiResponse<Map<String, Object>> setDemoRecordingMode(
            @ApiIgnore Authentication authentication,
            @RequestParam boolean enabled
    ) {
        Long userId = (Long) authentication.getPrincipal();
        devDateMapper.updateDemoRecordingMode(userId, enabled);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("demoRecordingMode", enabled);
        return ApiResponse.success(enabled ? "데모 녹화 모드를 켰습니다." : "데모 녹화 모드를 껐습니다.", result);
    }

    @ApiOperation(value = "데모 상태 수동 리셋", notes = "데모 녹화 모드에서 라이브로 넣은 기록을 지금 시점에 수동으로 정리하고, " +
            "시드(DEMO-SEED-*) 데이터 기준으로 되돌린다. 자동 리셋이 꺼져 있을 때 촬영 중 실수를 되돌리는 용도.")
    @PostMapping("/reset-demo-state")
    public ApiResponse<Void> resetDemoStateManually(
            @ApiIgnore Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        LocalDate virtualDate = devDateUtil.today(userId);
        devResetService.resetDemoState(userId, virtualDate);
        return ApiResponse.success("데모 상태를 시드 기준으로 리셋했습니다.", null);
    }
}
