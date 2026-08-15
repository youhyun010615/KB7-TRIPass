package com.tripass.saving.analysis;

import com.tripass.saving.dto.ReductionRateOptionDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 카테고리 하나의 기준 지출액(baselineSpendingAmount)을 절감률(10/30/50%)에 적용해
 * 월/주간 목표 금액을 계산한다. 저장하지 않고 그때그때 계산하는 값이라 순수 계산기로 둔다.
 *
 * 원 단위는 항상 내림(DOWN)으로 처리한다. 절감 목표를 실제보다 넉넉하게 잡거나
 * 사용 한도를 실제보다 후하게 잡지 않기 위해서다.
 */
@Component
public class MissionReductionCalculator {

    public static final List<Integer> AVAILABLE_REDUCTION_RATES = List.of(10, 30, 50);

    private static final int WEEKS_PER_MONTH = 4;
    private static final int PERCENT_BASE = 100;

    /** 절감률 10/30/50%에 대한 예상 금액을 각각 계산한다. GET /mission-options에서 사용한다. */
    public List<ReductionRateOptionDto> calculateOptions(int baselineSpendingAmount) {
        return AVAILABLE_REDUCTION_RATES.stream()
                .map(rate -> calculate(baselineSpendingAmount, rate))
                .toList();
    }

    /** 절감률 하나에 대한 월/주간 목표 금액을 계산한다. PUT 저장·GET 옵션 조회 양쪽에서 쓰는 핵심 계산이다. */
    public ReductionRateOptionDto calculate(int baselineSpendingAmount, int reductionRate) {
        // int 오버플로를 피하려고 곱셈만 long으로 계산한다.
        int monthlyReductionTarget = (int) ((long) baselineSpendingAmount * reductionRate / PERCENT_BASE);
        int monthlyUsageTarget = baselineSpendingAmount - monthlyReductionTarget;
        int weeklyUsageLimit = monthlyUsageTarget / WEEKS_PER_MONTH;
        int weeklyExpectedSaving = monthlyReductionTarget / WEEKS_PER_MONTH;

        return new ReductionRateOptionDto(
                reductionRate, monthlyReductionTarget, monthlyUsageTarget, weeklyUsageLimit, weeklyExpectedSaving);
    }
}
