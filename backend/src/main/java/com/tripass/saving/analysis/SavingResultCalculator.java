package com.tripass.saving.analysis;

import com.tripass.saving.dto.SavingResultResponseDto;
import com.tripass.saving.dto.SavingResultStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Component
public class SavingResultCalculator {

    private static final String UNAVAILABLE_MESSAGE = "아직 집계할 수 있는 저축 내역이 없어요.";

    /**
     * targetAmount 또는 actualAmount 중 하나라도 없으면(실제 저축 데이터가 아직 없는 경우 등)
     * 0원으로 취급해 차액을 계산하지 않고 UNAVAILABLE을 반환한다.
     * actualAmount가 0원인 경우는 "집계 불가"가 아니라 "실제로 0원 저축함"이므로 AVAILABLE로 처리한다.
     */
    public SavingResultResponseDto calculate(BigDecimal targetAmount, BigDecimal actualAmount) {
        if (targetAmount == null || actualAmount == null) {
            return new SavingResultResponseDto(
                    SavingResultStatus.UNAVAILABLE, targetAmount, null, null, UNAVAILABLE_MESSAGE);
        }

        BigDecimal difference = actualAmount.subtract(targetAmount);

        String message;
        int comparison = difference.compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            message = "목표보다 " + formatAmount(difference) + "원 더 저축했어요.";
        } else if (comparison < 0) {
            message = "목표보다 " + formatAmount(difference.abs()) + "원 덜 저축했어요.";
        } else {
            message = "목표 저축 금액을 달성했어요.";
        }

        return new SavingResultResponseDto(SavingResultStatus.AVAILABLE, targetAmount, actualAmount, difference, message);
    }

    // DecimalFormat은 thread-safe하지 않으므로 싱글톤 빈에 static 필드로 두지 않고 호출마다 생성한다.
    private String formatAmount(BigDecimal amount) {
        return new DecimalFormat("#,##0").format(amount);
    }
}
