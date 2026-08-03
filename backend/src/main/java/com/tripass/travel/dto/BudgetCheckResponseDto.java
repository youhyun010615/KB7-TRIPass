package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BudgetCheckResponseDto {
    private Long targetBudget;
    private Long preExpenseTotal;
    private Long travelExpenseTotal;
    private Long remainingFund;
    private List<CategoryBreakdownDto> categoryBreakdown;
}
