package com.tripass.checklist.controller;

import com.tripass.checklist.dto.*;
import com.tripass.checklist.service.ChecklistService;
import com.tripass.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /**
     * 1. 여행 체크리스트 전체 현황 요약 조회
     */
    @GetMapping("/{id}/checklists/summary")
    public ResponseEntity<ApiResponse<ChecklistSummaryResponseDto>> getChecklistSummary(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        ChecklistSummaryResponseDto data = checklistService.getChecklistSummary(id, userId);
        return ResponseEntity.ok(ApiResponse.success("체크리스트 요약 조회 성공", data));
    }

    /**
     * 2. 단계별 체크리스트 상세 목록 조회
     */
    @GetMapping("/{id}/checklists")
    public ResponseEntity<ApiResponse<ChecklistGroupResponseDto>> getChecklists(
            @PathVariable Long id,
            @RequestParam String type,
            @RequestParam(required = false) String ddayStage,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        ChecklistGroupResponseDto data = checklistService.getChecklists(id, type, ddayStage, userId);
        return ResponseEntity.ok(ApiResponse.success("체크리스트 상세 목록 조회 성공", data));
    }

    /**
     * 3. 체크리스트 항목 완료 상태 토글/변경
     */
    @PatchMapping("/{id}/checklists/{itemId}")
    public ResponseEntity<ApiResponse<ChecklistToggleResponseDto>> toggleChecklistItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody ChecklistToggleRequestDto request,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        ChecklistToggleResponseDto data = checklistService.toggleChecklistItem(id, itemId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("체크리스트 항목이 완료 처리되었습니다.", data));
    }

    /**
     * 체크리스트 항목 추가 (사용자 커스텀 항목 생성)
     */
    @PostMapping("/{id}/checklists")
    public ResponseEntity<ApiResponse<ChecklistCreateResponseDto>> createChecklistItem(
            @PathVariable Long id,
            @RequestBody ChecklistCreateRequestDto request,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        ChecklistCreateResponseDto data = checklistService.createChecklistItem(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("체크리스트 항목이 추가되었습니다.", data));
    }

    /**
     * 체크리스트 항목 삭제
     */
    @DeleteMapping("/{id}/checklists/{itemId}")
    public ResponseEntity<ApiResponse<ChecklistDeleteResponseDto>> deleteChecklistItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        ChecklistDeleteResponseDto data = checklistService.deleteChecklistItem(id, itemId, userId);
        return ResponseEntity.ok(ApiResponse.success("체크리스트 항목이 삭제되었습니다.", data));
    }

}