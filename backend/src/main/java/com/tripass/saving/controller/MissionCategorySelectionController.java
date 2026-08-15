package com.tripass.saving.controller;

import com.tripass.common.exception.CustomException;
import com.tripass.common.response.ApiResponse;
import com.tripass.saving.dto.MissionOptionResponseDto;
import com.tripass.saving.dto.MissionSelectionRequestDto;
import com.tripass.saving.dto.MissionSelectionsResponseDto;
import com.tripass.saving.service.MissionCategorySelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/saving/analyses")
@RequiredArgsConstructor
public class MissionCategorySelectionController {

    private final MissionCategorySelectionService missionCategorySelectionService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_YEAR_MONTH",
                    "yearMonth 형식이 올바르지 않습니다. 예: 2026-07");
        }
    }

    /** 절감 추천 TOP 3 카테고리별 10/30/50% 절감률 옵션과 예상 금액을 조회한다(저장하지 않는다). */
    @GetMapping("/{yearMonth}/mission-options")
    public ResponseEntity<ApiResponse<List<MissionOptionResponseDto>>> getMissionOptions(
            @PathVariable String yearMonth,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        List<MissionOptionResponseDto> data =
                missionCategorySelectionService.getMissionOptions(userId, parseYearMonth(yearMonth));
        return ResponseEntity.ok(ApiResponse.success("절감률 옵션 조회 성공", data));
    }

    /** 카테고리별 절감률 선택을 전체 교체 방식으로 저장한다. 빈 selections는 전체 선택 해제를 의미한다. */
    @PutMapping("/{yearMonth}/mission-selections")
    public ResponseEntity<ApiResponse<MissionSelectionsResponseDto>> saveMissionSelections(
            @PathVariable String yearMonth,
            @Valid @RequestBody MissionSelectionRequestDto request,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        MissionSelectionsResponseDto data = missionCategorySelectionService.saveMissionSelections(
                userId, parseYearMonth(yearMonth), request);
        return ResponseEntity.ok(ApiResponse.success("절감률 선택 저장 성공", data));
    }

    /** 저장된 카테고리별 절감률 선택 결과와 합계를 조회한다. */
    @GetMapping("/{yearMonth}/mission-selections")
    public ResponseEntity<ApiResponse<MissionSelectionsResponseDto>> getMissionSelections(
            @PathVariable String yearMonth,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        MissionSelectionsResponseDto data =
                missionCategorySelectionService.getMissionSelections(userId, parseYearMonth(yearMonth));
        return ResponseEntity.ok(ApiResponse.success("절감률 선택 조회 성공", data));
    }
}
