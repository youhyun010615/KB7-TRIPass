package com.tripass.report.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode {

    TRIP_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TRIP_NOT_FOUND",
            "여행 정보를 찾을 수 없습니다."
    ),

    TRIP_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "TRIP_ACCESS_DENIED",
            "해당 여행에 접근할 권한이 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
