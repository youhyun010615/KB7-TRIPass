package com.tripass.checklist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChecklistToggleResponseDto {
    private Long id;              // 변경된 체크리스트 항목 ID
    private Boolean isCompleted;  // 변경된 완료 상태
}