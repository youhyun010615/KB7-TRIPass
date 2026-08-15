package com.tripass.travel.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BudgetCheckResponseDto {
    private Long tripCountryId;
    private String countryName;
    private Long targetBudget;
    private Long preExpenseTotal;
    private Long travelExpenseTotal;
    private Long remainingFund;
    private List<CategoryBreakdownDto> categoryBreakdown;
}
