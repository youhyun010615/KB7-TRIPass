package com.tripass.travel.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TravelErrorCode {

    // 400 Bad Request
    INVALID_PATH_VARIABLE(HttpStatus.BAD_REQUEST, "INVALID_PATH_VARIABLE", "유효하지 않은 여행 ID 형식입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 유효하지 않습니다."),
    MISSING_COUNTRY_ID(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "scope가 COUNTRY인 경우 countryId는 필수입니다."),

    // 403 Forbidden
    FORBIDDEN_TRIP_ACCESS(HttpStatus.FORBIDDEN, "FORBIDDEN_TRIP_ACCESS", "해당 여행 정보에 대한 접근 권한이 없습니다."),

    // 404 Not Found
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "존재하지 않는 여행 ID입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}