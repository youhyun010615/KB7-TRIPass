package com.tripass.checklist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChecklistCreateResponseDto {
    private Long id; // 생성된 체크리스트 항목 PK
}