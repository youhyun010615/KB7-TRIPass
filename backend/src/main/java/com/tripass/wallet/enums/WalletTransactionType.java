package com.tripass.wallet.enums;

/** 월렛 원장 거래의 충전, 출금, 카드 충전, 미션 보상, 환불, 보정 유형을 구분하는 Enum입니다. */

public enum WalletTransactionType {
    CHARGE,
    WITHDRAW,
    CARD_TOPUP,
    EXCHANGE_SELL,
    MISSION_REWARD,
    REFUND,
    ADJUST
}
