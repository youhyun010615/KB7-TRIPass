package com.tripass.common.response;

import lombok.Getter;

/**
 * 모든 API 응답의 공통 래퍼
 *
 * 성공: { "code": "SUCCESS", "message": "...", "data": { ... } }
 * 실패: { "code": "ERR_CODE",  "message": "...", "data": null }
 */
@Getter
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "요청이 정상적으로 처리되었습니다.", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
