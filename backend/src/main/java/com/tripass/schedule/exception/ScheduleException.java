package com.tripass.schedule.exception;

import com.tripass.common.exception.CustomException;

public class ScheduleException extends CustomException {

    public ScheduleException(ScheduleErrorCode errorCode) {
        super(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }
}
