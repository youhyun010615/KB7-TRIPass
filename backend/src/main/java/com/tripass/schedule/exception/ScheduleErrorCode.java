package com.tripass.schedule.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode {

    INVALID_TRIP_ID(
            HttpStatus.BAD_REQUEST,
            "INVALID_TRIP_ID",
            "잘못된 여행 ID입니다."
    ),

    TRIP_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TRIP_NOT_FOUND",
            "여행 정보를 찾을 수 없습니다."
    ),

    TRIP_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "TRIP_ACCESS_DENIED",
            "해당 여행에 접근할 권한이 없습니다."
    ),

    SCHEDULE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SCHEDULE_NOT_FOUND",
            "여행 일정을 찾을 수 없습니다."
    ),

    INVALID_SCHEDULE_ID(
            HttpStatus.BAD_REQUEST,
            "INVALID_SCHEDULE_ID",
            "잘못된 여행 일정 ID입니다."
    ),
    TRIP_COUNTRY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TRIP_COUNTRY_NOT_FOUND",
            "선택한 여행 국가 정보를 찾을 수 없습니다."
    ),

    CURRENCY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CURRENCY_NOT_FOUND",
            "선택한 통화 정보를 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}