package com.tripass.checklist.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChecklistErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "유효하지 않은 입력값입니다."),
    INVALID_PATH_VARIABLE(HttpStatus.BAD_REQUEST, "INVALID_PATH_VARIABLE", "유효하지 않은 ID 형식입니다."),
    DEFAULT_ITEM_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "DEFAULT_ITEM_CANNOT_BE_DELETED", "기본 제공 체크리스트 항목은 삭제할 수 없습니다."), // 👈 추가

    // 403 Forbidden
    FORBIDDEN_TRIP_ACCESS(HttpStatus.FORBIDDEN, "FORBIDDEN_TRIP_ACCESS", "해당 여행 정보에 대한 접근 권한이 없습니다."),

    // 404 Not Found
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "존재하지 않는 여행 ID입니다."),
    CHECKLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHECKLIST_ITEM_NOT_FOUND", "해당 체크리스트 항목을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}