package com.tripass.checklist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChecklistCreateRequestDto {
    private String itemName;      // 필수 항목명
    private String checklistType; // 필수 체크리스트 유형 (PRE_TRAVEL / RETURN)
    private String ddayStage;     // 선택 D-Day 단계 ("D30", "D7", "D1" 등, 귀국일 땐 null)
}