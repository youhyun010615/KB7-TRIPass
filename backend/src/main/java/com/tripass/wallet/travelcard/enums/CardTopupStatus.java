package com.tripass.wallet.travelcard.enums;

/** 트래블카드 외화 충전 요청의 처리 상태를 구분하는 Enum입니다. */

public enum CardTopupStatus {
    REQUESTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED,
    REFUNDED
}
