package com.tripass.wallet.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 월렛 도메인에서 발생할 수 있는 예외 코드, HTTP 상태, 메시지를 정의하는 Enum입니다. */

@Getter
@RequiredArgsConstructor
public enum WalletErrorCode {

    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "월렛을 찾을 수 없습니다."),
    WALLET_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "월렛에 연동된 계좌를 찾을 수 없습니다."),
    PRIMARY_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "설정된 주계좌가 없습니다. 주계좌를 먼저 설정해 주세요."),
    WALLET_TRAVEL_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "월렛에 연동된 트래블카드를 찾을 수 없습니다."),
    TRAVEL_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "트래블카드 상품을 찾을 수 없습니다."),
    TRAVEL_CARD_CURRENCY_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "해당 트래블카드에서 지원하지 않는 통화입니다."),
    EXCHANGE_RATE_NOT_FOUND(HttpStatus.NOT_FOUND, "환율 정보를 찾을 수 없습니다."),
    INSUFFICIENT_TRAVEL_CARD_BALANCE(HttpStatus.BAD_REQUEST, "트래블카드 외화 잔액이 부족합니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "금액이 올바르지 않습니다."),
    INVALID_CURRENCY_CODE(HttpStatus.BAD_REQUEST, "통화 코드가 올바르지 않습니다."),
    INVALID_MONTH(HttpStatus.BAD_REQUEST, "조회 월 형식이 올바르지 않습니다."),
    INSUFFICIENT_ACCOUNT_BALANCE(HttpStatus.BAD_REQUEST, "계좌 출금 가능 잔액이 부족합니다."),
    INSUFFICIENT_WALLET_BALANCE(HttpStatus.BAD_REQUEST, "월렛 잔액이 부족합니다."),
    DUPLICATED_REQUEST(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    WALLET_CONFLICT(HttpStatus.CONFLICT, "월렛 잔액 변경 중 충돌이 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
