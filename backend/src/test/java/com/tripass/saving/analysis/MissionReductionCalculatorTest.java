package com.tripass.saving.analysis;

import com.tripass.saving.dto.ReductionRateOptionDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionReductionCalculatorTest {

    private final MissionReductionCalculator calculator = new MissionReductionCalculator();

    @Test
    void 십퍼센트_절감률을_계산한다() {
        ReductionRateOptionDto option = calculator.calculate(100000, 10);

        assertEquals(10, option.reductionRate());
        assertEquals(10000, option.monthlyReductionTarget());
        assertEquals(90000, option.monthlyUsageTarget());
        assertEquals(22500, option.weeklyUsageLimit());
        assertEquals(2500, option.weeklyExpectedSaving());
    }

    @Test
    void 삼십퍼센트_절감률을_계산한다() {
        ReductionRateOptionDto option = calculator.calculate(100000, 30);

        assertEquals(30000, option.monthlyReductionTarget());
        assertEquals(70000, option.monthlyUsageTarget());
    }

    @Test
    void 오십퍼센트_절감률을_계산한다() {
        ReductionRateOptionDto option = calculator.calculate(100000, 50);

        assertEquals(50000, option.monthlyReductionTarget());
        assertEquals(50000, option.monthlyUsageTarget());
    }

    @Test
    void 월_절감_목표는_원_단위_내림으로_계산한다() {
        // 100001 * 10 / 100 = 10000.1 -> 내림이면 10000, HALF_UP이었다면 10000으로 같이 나와 구분이 안 되므로
        // 정확히 .5 초과가 반올림되는 값을 골라 내림 동작을 검증한다.
        ReductionRateOptionDto option = calculator.calculate(100009, 10);

        // 100009 * 10 / 100 = 10000.9 -> HALF_UP이면 10001, DOWN(내림)이면 10000이어야 한다.
        assertEquals(10000, option.monthlyReductionTarget());
    }

    @Test
    void 주간_금액도_원_단위_내림으로_계산한다() {
        ReductionRateOptionDto option = calculator.calculate(100000, 10);
        // monthlyUsageTarget=90000, monthlyReductionTarget=10000 모두 4로 나누어떨어지지 않는 값으로 확인
        ReductionRateOptionDto uneven = calculator.calculate(100003, 10);

        // monthlyReductionTarget = 100003*10/100 = 10000(내림), monthlyUsageTarget = 90003
        // weeklyUsageLimit = 90003/4 = 22500.75 -> 내림이면 22500
        // weeklyExpectedSaving = 10000/4 = 2500(정확히 나누어떨어짐)
        assertEquals(22500, uneven.weeklyUsageLimit());
        assertEquals(2500, uneven.weeklyExpectedSaving());
        assertEquals(22500, option.weeklyUsageLimit());
    }

    @Test
    void 세_절감률_옵션을_10_30_50_순서로_반환한다() {
        List<ReductionRateOptionDto> options = calculator.calculateOptions(100000);

        assertEquals(3, options.size());
        assertEquals(10, options.get(0).reductionRate());
        assertEquals(30, options.get(1).reductionRate());
        assertEquals(50, options.get(2).reductionRate());
    }
}
