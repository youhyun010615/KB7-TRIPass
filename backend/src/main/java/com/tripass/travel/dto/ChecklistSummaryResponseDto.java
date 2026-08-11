package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistSummaryResponseDto {
    private Integer totalCompletedCount;  // 전체 완료 항목 수
    private Integer totalItemCount;       // 전체 항목 수
    private Integer prepCompletedCount;   // 여행 준비 완료 항목 수
    private Integer prepItemCount;        // 여행 준비 전체 항목 수
    private Integer returnCompletedCount; // 귀국 완료 항목 수
    private Integer returnItemCount;      // 귀국 전체 항목 수
}