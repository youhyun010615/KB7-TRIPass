package com.tripass.checklist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistGroupResponseDto {
    private List<ChecklistResponseDto> carriedOverChecklists; // 이월된 미완료 항목 목록
    private List<ChecklistResponseDto> currentChecklists;     // 현재 단계 본래 항목 목록
}