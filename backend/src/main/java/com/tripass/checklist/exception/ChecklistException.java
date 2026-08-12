package com.tripass.checklist.exception;

import com.tripass.common.exception.CustomException;
import lombok.Getter;

@Getter
public class ChecklistException extends CustomException {

    // 1. 기본 ErrorCode 메시지 사용
    public ChecklistException(ChecklistErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    // 2. 동적 상세 메시지가 필요한 경우 사용
    public ChecklistException(ChecklistErrorCode errorCode, String customMessage) {
        super(errorCode.getStatus(), errorCode.getCode(), customMessage);
    }
}