package com.tripass.report.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.report.dto.PostTripReportResponseDto;
import com.tripass.report.dto.PreTripReportResponseDto;
import com.tripass.report.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Positive;

@Api(tags = "여행 리포트")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips/{tripId}/report")
public class ReportController {

    private final ReportService reportService;

    @ApiOperation(
            value = "여행 대비 리포트 조회",
            notes = "선택한 여행의 저축 목표, 준비 현황 등 여행 전 리포트를 조회합니다."
    )
    @GetMapping("/pre-trip")
    public ResponseEntity<ApiResponse<PreTripReportResponseDto>> getPreTripReport(
            @ApiParam(value = "여행 ID", required = true, example = "1")
            @PathVariable
            @Positive(message = "여행 ID는 양수여야 합니다.")
            Long tripId,

            @AuthenticationPrincipal Long userId
    ) {
        PreTripReportResponseDto data = reportService.getPreTripReport(tripId, userId);

        return ResponseEntity.ok(ApiResponse.success("여행 대비 리포트 조회 성공", data));
    }

    @ApiOperation(
            value = "여행 후 리포트 조회",
            notes = "선택한 여행의 예산 소비 요약, 지출 분석 등 여행 후 리포트를 조회합니다."
    )
    @GetMapping("/post-trip")
    public ResponseEntity<ApiResponse<PostTripReportResponseDto>> getPostTripReport(
            @ApiParam(value = "여행 ID", required = true, example = "1")
            @PathVariable
            @Positive(message = "여행 ID는 양수여야 합니다.")
            Long tripId,

            @AuthenticationPrincipal Long userId
    ) {
        PostTripReportResponseDto data = reportService.getPostTripReport(tripId, userId);

        return ResponseEntity.ok(ApiResponse.success("여행 후 리포트 조회 성공", data));
    }
}
