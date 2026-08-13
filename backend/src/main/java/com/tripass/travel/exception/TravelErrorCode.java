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
    INVALID_TRIP_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_TRIP_DATE_RANGE", "여행 국가의 도착일은 출발일보다 이전이어야 합니다."),
    DUPLICATE_TRIP_COUNTRY(HttpStatus.BAD_REQUEST, "DUPLICATE_TRIP_COUNTRY", "동일한 국가는 여행 계획에 한 번만 등록할 수 있습니다."),
    INVALID_TRIP_COUNTRY_ORDER(HttpStatus.BAD_REQUEST, "INVALID_TRIP_COUNTRY_ORDER", "여행 국가 방문 순서와 일정이 올바르지 않습니다."),
    TRIP_ALREADY_EXISTS(HttpStatus.CONFLICT, "TRIP_ALREADY_EXISTS", "진행 중인 여행 목표가 이미 있습니다. 기존 여행 계획을 수정해 주세요."),
    TRIP_NOT_EDITABLE(HttpStatus.CONFLICT, "TRIP_NOT_EDITABLE", "여행 시작 후에는 여행 목표를 수정할 수 없습니다."),
    BUDGET_NOT_READY(HttpStatus.CONFLICT, "BUDGET_NOT_READY", "AI 예산 추천을 먼저 생성해 주세요."),
    AI_BUDGET_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI_BUDGET_UNAVAILABLE", "AI 여행 예산 추천을 지금 생성할 수 없습니다."),
    INVALID_BUDGET_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_BUDGET_AMOUNT", "예산 금액은 0 이상 1억 원 이하여야 합니다."),

    // 403 Forbidden
    FORBIDDEN_TRIP_ACCESS(HttpStatus.FORBIDDEN, "FORBIDDEN_TRIP_ACCESS", "해당 여행 정보에 대한 접근 권한이 없습니다."),

    // 404 Not Found
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "존재하지 않는 여행 ID입니다."),
    COUNTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "COUNTRY_NOT_FOUND", "존재하지 않는 국가입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
