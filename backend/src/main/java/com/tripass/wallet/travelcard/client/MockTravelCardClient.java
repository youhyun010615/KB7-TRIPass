package com.tripass.wallet.travelcard.client;

import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 실제 카드사 API 대신 규칙 기반 목 응답으로 트래블카드 충전 성공과 실패를 시뮬레이션하는 클라이언트입니다. */

@Slf4j
@Component
public class MockTravelCardClient {

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("1000000");

    public TravelCardTopupResult requestTopup(
            WalletCardTopup topup,
            String idempotencyKey
    ) {
        if (idempotencyKey != null && idempotencyKey.toUpperCase().contains("FAIL")) {
            return TravelCardTopupResult.failed(
                    "MOCK_FORCED_FAIL",
                    "멱등키에 FAIL이 포함되어 목 충전 실패로 처리되었습니다."
            );
        }

        if (topup.getKrwAmount().compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
            return TravelCardTopupResult.failed(
                    "MOCK_LIMIT_EXCEEDED",
                    "목 카드사 1회 충전 한도를 초과했습니다."
            );
        }

        if ("CHF".equals(topup.getCurrencyCode()) && isFirstAttempt(topup.getFailureReason())) {
            return TravelCardTopupResult.failed(
                    "MOCK_TEMPORARY_ERROR",
                    "CHF 충전은 목 카드사 일시 오류로 1회 실패 처리됩니다."
            );
        }

        return TravelCardTopupResult.succeeded(
                "MOCK-TOPUP-" + topup.getId()
        );
    }

    private boolean isFirstAttempt(String failureReason) {
        return failureReason == null || failureReason.trim().isEmpty();
    }
}
