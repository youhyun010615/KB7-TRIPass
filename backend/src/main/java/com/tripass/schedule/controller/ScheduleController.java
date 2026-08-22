package com.tripass.schedule.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.schedule.dto.*;
import com.tripass.schedule.service.ScheduleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

@Api(tags = "여행 일정")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips/{tripId}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @ApiOperation(
            value = "여행 일정 목록 조회",
            notes = "선택한 여행에 포함된 일정 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleListResponseDto>>> getSchedules(
            @ApiParam(value = "여행 ID", required = true, example = "1")
            @PathVariable
            @Positive(message = "여행 ID는 양수여야 합니다.")
            Long tripId,

            @AuthenticationPrincipal Long userId
    ) {

        List<ScheduleListResponseDto> data = scheduleService.getSchedules(tripId, userId);

        return ResponseEntity.ok(ApiResponse.success("여행 일정 목록 조회 성공", data));
    }


    @ApiOperation(
            value = "여행 일정 상세 조회",
            notes = "선택한 여행 일정의 상세 정보를 조회합니다."
    )
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleDetailResponseDto>> getScheduleDetail(
            @ApiParam(value = "여행 ID", required = true, example = "1")
            @PathVariable
            @Positive(message = "여행 ID는 양수여야 합니다.")
            Long tripId,
            @ApiParam(value = "여행 일정 ID", required = true, example = "1")
            @PathVariable
            @Positive(message = "여행 일정 ID는 양수여야 합니다.")
            Long scheduleId,

            @AuthenticationPrincipal Long userId
    ) {
        ScheduleDetailResponseDto data = scheduleService.getScheduleDetail(tripId, scheduleId, userId);

        return ResponseEntity.ok(ApiResponse.success("여행 일정 상세 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "여행 일정 등록",
            notes = "선택한 여행에 일정을 등록합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleCreateResponseDto>>
    createSchedule(
            @ApiParam(
                    value = "여행 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable("tripId")
            @Positive(
                    message = "여행 ID는 양수여야 합니다."
            )
            Long tripId,

            @Valid
            @RequestBody
            ScheduleCreateRequestDto request,

            @AuthenticationPrincipal Long userId
    ) {
        ScheduleCreateResponseDto data =
                scheduleService.createSchedule(
                        tripId,
                        request,
                        userId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "여행 일정 등록 성공",
                                data
                        )
                );
    }

    @ApiOperation(
            value = "여행 일정 수정",
            notes = "기존 여행 일정 정보를 수정합니다."
    )
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleUpdateResponseDto>>
    updateSchedule(
            @ApiParam(
                    value = "여행 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable("tripId")
            @Positive(
                    message = "여행 ID는 양수여야 합니다."
            )
            Long tripId,

            @ApiParam(
                    value = "여행 일정 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable("scheduleId")
            @Positive(
                    message = "여행 일정 ID는 양수여야 합니다."
            )
            Long scheduleId,

            @Valid
            @RequestBody
            ScheduleUpdateRequestDto request,

            @AuthenticationPrincipal Long userId
    ) {
        ScheduleUpdateResponseDto data =
                scheduleService.updateSchedule(
                        tripId,
                        scheduleId,
                        request,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "여행 일정 수정 성공",
                        data
                )
        );
    }

    @ApiOperation(
            value = "여행 일정 삭제",
            notes = "여행에 등록된 일정을 소프트 삭제합니다."
    )
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>>
    deleteSchedule(
            @ApiParam(
                    value = "여행 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable("tripId")
            @Positive(
                    message = "여행 ID는 양수여야 합니다."
            )
            Long tripId,

            @ApiParam(
                    value = "여행 일정 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable("scheduleId")
            @Positive(
                    message = "여행 일정 ID는 양수여야 합니다."
            )
            Long scheduleId,

            @AuthenticationPrincipal Long userId
    ) {
        scheduleService.deleteSchedule(
                tripId,
                scheduleId,
                userId
        );

        return ResponseEntity.ok(
                ApiResponse.<Void>success(
                        "여행 일정 삭제 성공",
                        null
                )
        );
    }
}
