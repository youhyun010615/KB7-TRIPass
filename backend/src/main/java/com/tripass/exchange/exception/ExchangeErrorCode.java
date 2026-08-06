package com.tripass.exchange.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExchangeErrorCode {

    RATE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXCHANGE_RATE_NOT_FOUND", "환율 정보를 찾을 수 없습니다."),
    API_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EXCHANGE_API_CONNECTION_ERROR", "환율 API 연결에 실패했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "EXCHANGE_INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    INVALID_DAYS_RANGE(HttpStatus.BAD_REQUEST, "EXCHANGE_INVALID_DAYS_RANGE", "조회 가능한 일수는 7, 30, 90일 중 하나여야 합니다."),
    DUPLICATE_ALERT(HttpStatus.BAD_REQUEST, "EXCHANGE_DUPLICATE_ALERT", "이미 존재하는 알림입니다."),
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "EXCHANGE_ALERT_NOT_FOUND", "해당 알림을 찾을 수 없습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "EXCHANGE_FORBIDDEN_ACCESS", "접근 권한이 없습니다."),
    UNSUPPORTED_CURRENCY(HttpStatus.BAD_REQUEST, "EXCHANGE_UNSUPPORTED_CURRENCY", "지원하지 않는 통화입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
