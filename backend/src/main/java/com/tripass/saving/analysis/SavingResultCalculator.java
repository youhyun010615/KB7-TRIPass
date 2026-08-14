package com.tripass.saving.analysis;

import com.tripass.saving.dto.SavingResultResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Component
public class SavingResultCalculator {

    public SavingResultResponseDto calculate(BigDecimal targetAmount, BigDecimal actualAmount) {
        BigDecimal target = targetAmount != null ? targetAmount : BigDecimal.ZERO;
        BigDecimal actual = actualAmount != null ? actualAmount : BigDecimal.ZERO;
        BigDecimal difference = actual.subtract(target);

        String message;
        int comparison = difference.compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            message = "목표보다 " + formatAmount(difference) + "원 더 저축했어요.";
        } else if (comparison < 0) {
            message = "목표보다 " + formatAmount(difference.abs()) + "원 덜 저축했어요.";
        } else {
            message = "목표 저축 금액을 달성했어요.";
        }

        return new SavingResultResponseDto(target, actual, difference, message);
    }

    // DecimalFormat은 thread-safe하지 않으므로 싱글톤 빈에 static 필드로 두지 않고 호출마다 생성한다.
    private String formatAmount(BigDecimal amount) {
        return new DecimalFormat("#,##0").format(amount);
    }
}
