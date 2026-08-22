package com.tripass.travel.exception;

import com.tripass.common.exception.CustomException;

public class TravelException extends CustomException {

    // 기본 ErrorCode 메시지 사용
    public TravelException(TravelErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    // 상황에 따라 동적 상세 메시지가 필요한 경우 사용
    public TravelException(TravelErrorCode errorCode, String detailMessage) {
        super(errorCode.getStatus(), errorCode.getCode(), detailMessage);
    }
}