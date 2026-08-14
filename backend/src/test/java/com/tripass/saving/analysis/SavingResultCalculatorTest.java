package com.tripass.saving.analysis;

import com.tripass.saving.dto.SavingResultResponseDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingResultCalculatorTest {

    private final SavingResultCalculator calculator = new SavingResultCalculator();

    @Test
    void 목표보다_더_저축했으면_초과_문구를_반환한다() {
        SavingResultResponseDto result = calculator.calculate(
                new BigDecimal("700000"), new BigDecimal("850000"));

        assertEquals(new BigDecimal("150000"), result.differenceAmount());
        assertTrue(result.resultMessage().contains("더 저축했어요"));
        assertTrue(result.resultMessage().contains("150,000"));
    }

    @Test
    void 목표보다_덜_저축했으면_부족_문구를_반환한다() {
        SavingResultResponseDto result = calculator.calculate(
                new BigDecimal("700000"), new BigDecimal("620000"));

        assertEquals(new BigDecimal("-80000"), result.differenceAmount());
        assertTrue(result.resultMessage().contains("덜 저축했어요"));
        assertTrue(result.resultMessage().contains("80,000"));
    }

    @Test
    void 목표와_정확히_같으면_달성_문구를_반환한다() {
        SavingResultResponseDto result = calculator.calculate(
                new BigDecimal("700000"), new BigDecimal("700000"));

        assertEquals(BigDecimal.ZERO, result.differenceAmount());
        assertEquals("목표 저축 금액을 달성했어요.", result.resultMessage());
    }

    @Test
    void 목표나_실제값이_null이면_0으로_취급한다() {
        SavingResultResponseDto result = calculator.calculate(null, new BigDecimal("50000"));

        assertEquals(new BigDecimal("50000"), result.differenceAmount());
    }
}
