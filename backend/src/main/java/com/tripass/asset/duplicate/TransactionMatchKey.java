package com.tripass.asset.duplicate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * (거래일, 금액) 기준 중복 후보 그룹화 키.
 *
 * amount는 호출자마다 BigDecimal 스케일이 다를 수 있으므로(예: "10000" vs "10000.00")
 * 정규화된 스케일로 저장해 equals/hashCode가 값 기준으로 정확히 동작하도록 한다.
 */
public record TransactionMatchKey(LocalDate transactionDate, BigDecimal amount) {

    public TransactionMatchKey {
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
}
