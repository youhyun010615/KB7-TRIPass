package com.tripass.travel.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BudgetCheckResponseDto {
    // 테스트용 주석 추가
    private Long targetBudget;
    private Long preExpenseTotal;
    private Long travelExpenseTotal;
    private Long remainingFund;
    private List<CategoryBreakdownDto> categoryBreakdown;
}
