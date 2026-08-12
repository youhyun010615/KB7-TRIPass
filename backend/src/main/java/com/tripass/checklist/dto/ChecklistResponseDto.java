package com.tripass.checklist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistResponseDto {
    private Long id;              // 항목 식별 ID
    private String itemName;      // 항목명
    private Boolean isCompleted;  // 완료 여부
    private Boolean isCustom;     // 사용자 추가 항목 여부
    private Boolean isCarriedOver;// 미완료 이월 항목 여부 (귀국 항목은 항상 false)
    private String ddayStage;     // D-Day 단계 (D30, D7, D1 / 귀국 항목은 null)
}