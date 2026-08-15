package com.tripass.saving.analysis;

import com.tripass.saving.dto.SavingResultResponseDto;
import com.tripass.saving.dto.SavingResultStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingResultCalculatorTest {

    private final SavingResultCalculator calculator = new SavingResultCalculator();

    @Test
    void 목표보다_더_저축했으면_초과_문구를_반환한다() {
        SavingResultResponseDto result = calculator.calculate(
                new BigDecimal("700000"), new BigDecimal("850000"));

        assertEquals(SavingResultStatus.AVAILABLE, result.status());
        assertEquals(new BigDecimal("150000"), result.differenceAmount());
        assertTrue(result.resultMessage().contains("더 저축했어요"));
        assertTrue(result.resultMessage().contains("150,000"));
    }

    @Test
    void 목표보다_덜_저축했으면_부족_문구를_반환한다() {
        SavingResultResponseDto result = calculator.calculate(
                new BigDecimal("700000"), new BigDecimal("620000"));

        assertEquals(SavingResultStatus.AVAILABLE, result.status());
        assertEquals(new BigDecimal("-80000"), result.differenceAmount());
        assertTrue(result.resultMessage().contains("덜 저축했어요"));
        assertTrue(result.resultMessage().contains("80,000"));
    }

    @Test
    void 목표와_정확히_같으면_달성_문구를_반환한다() {
        SavingResultResponseDto result = calculator.calculate(
                new BigDecimal("700000"), new BigDecimal("700000"));

        assertEquals(SavingResultStatus.AVAILABLE, result.status());
        assertEquals(BigDecimal.ZERO, result.differenceAmount());
        assertEquals("목표 저축 금액을 달성했어요.", result.resultMessage());
    }

    @Test
    void 실제_저축액이_0원이면_집계_불가가_아니라_정상_차액을_계산한다() {
        SavingResultResponseDto result = calculator.calculate(
                new BigDecimal("700000"), BigDecimal.ZERO);

        assertEquals(SavingResultStatus.AVAILABLE, result.status());
        assertEquals(new BigDecimal("-700000"), result.differenceAmount());
        assertTrue(result.resultMessage().contains("덜 저축했어요"));
    }

    @Test
    void 실제_저축액이_없으면_집계_불가를_반환하고_0으로_취급하지_않는다() {
        SavingResultResponseDto result = calculator.calculate(new BigDecimal("700000"), null);

        assertEquals(SavingResultStatus.UNAVAILABLE, result.status());
        assertNull(result.differenceAmount());
        assertNull(result.actualAmount());
    }

    @Test
    void 목표_저축액이_없으면_집계_불가를_반환한다() {
        SavingResultResponseDto result = calculator.calculate(null, new BigDecimal("50000"));

        assertEquals(SavingResultStatus.UNAVAILABLE, result.status());
        assertNull(result.differenceAmount());
    }

    @Test
    void 차액이_50전_경계값이면_문구도_HALF_UP으로_반올림한다() {
        // 150000.50원은 HALF_UP이면 150001원, DecimalFormat 기본값인 HALF_EVEN이면 150000원으로 갈린다.
        // Service.roundToWon()이 저장하는 DB 값과 문구가 어긋나면 안 되므로 HALF_UP으로 통일돼야 한다.
        SavingResultResponseDto result = calculator.calculate(
                new BigDecimal("700000"), new BigDecimal("850000.50"));

        assertTrue(result.resultMessage().contains("150,001"));
    }
}
