package com.tripass.wallet.fx.enums;

/** 월렛 환전 거래의 처리 상태를 구분하는 Enum입니다. */

public enum ExchangeStatus {
    REQUESTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED
}
