package com.tripass.schedule.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.schedule.dto.ScheduleListResponseDto;
import com.tripass.schedule.service.ScheduleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "여행 일정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips/{tripId}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @ApiOperation(
            value = "여행 일정 목록 조회",
            notes = "선택한 여행의 삭제되지 않은 일정을 시작 일시 기준으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleListResponseDto>>> getSchedules(
            @ApiParam(value = "여행 ID", required = true, example = "1")
            @PathVariable Long tripId
    ) {

        List<ScheduleListResponseDto> data = scheduleService.getSchedules(tripId);

        return ResponseEntity.ok(ApiResponse.success("여행 일정 목록 조회 성공", data));
    }
}
