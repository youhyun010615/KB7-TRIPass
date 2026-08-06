package com.tripass.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

//급여 목록과 월 급여 합계 응답 DTO
@Getter
@AllArgsConstructor
public class IncomeSourceListResponse {
    // 등록된 모든 활성 급여의 월 급여 합계
    private BigDecimal totalMonthlyIncome;

    // 등록된 급여 목록
    private List<IncomeSourceResponse> incomeSources;
}
