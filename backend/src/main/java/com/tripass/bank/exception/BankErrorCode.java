package com.tripass.bank.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BankErrorCode {

    BANK_NOT_FOUND(HttpStatus.NOT_FOUND, "BANK_NOT_FOUND", "해당 은행 지점을 찾을 수 없습니다."),
    CURRENCY_NOT_FOUND(HttpStatus.NOT_FOUND, "BANK_CURRENCY_NOT_FOUND", "해당 통화의 환율 정보를 찾을 수 없습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "BANK_INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BANK_SYNC_FAILED", "은행 데이터 동기화에 실패했습니다."),
    API_RESPONSE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "BANK_API_RESPONSE_INVALID", "외부 API 응답 형식이 올바르지 않습니다."),
    API_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "BANK_API_SERVICE_UNAVAILABLE", "외부 은행 정보 제공 서비스에 연결할 수 없습니다."),
    INVALID_CALCULATION_INPUT(HttpStatus.BAD_REQUEST, "BANK_INVALID_CALCULATION_INPUT", "환전 계산을 위한 입력값이 올바르지 않습니다 (금액은 0보다 커야 합니다).");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
