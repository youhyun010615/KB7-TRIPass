package com.tripass.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 예외 — 도메인별 서비스에서 throw
 *
 * 사용 예:
 *   throw new CustomException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "여행 정보를 찾을 수 없습니다.");
 */
@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public CustomException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
