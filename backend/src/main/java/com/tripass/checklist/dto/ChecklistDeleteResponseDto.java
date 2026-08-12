package com.tripass.checklist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChecklistDeleteResponseDto {
    private Long id; // 삭제된 체크리스트 항목 ID
}