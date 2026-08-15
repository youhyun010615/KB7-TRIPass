package com.tripass.wallet.travelcard.client;

import lombok.*;

/** 목 트래블카드 충전 요청의 성공 여부, 외부 거래 ID, 실패 사유를 담는 응답 객체입니다. */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelCardTopupResult {

    private boolean success;
    private String externalTransactionId;
    private String failureCode;
    private String failureMessage;

    public static TravelCardTopupResult succeeded(String externalTransactionId) {
        return TravelCardTopupResult.builder()
                .success(true)
                .externalTransactionId(externalTransactionId)
                .build();
    }

    public static TravelCardTopupResult failed(
            String failureCode,
            String failureMessage
    ) {
        return TravelCardTopupResult.builder()
                .success(false)
                .failureCode(failureCode)
                .failureMessage(failureMessage)
                .build();
    }
}
