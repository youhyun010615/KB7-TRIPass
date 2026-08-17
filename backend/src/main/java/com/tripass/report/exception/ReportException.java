package com.tripass.report.exception;

import com.tripass.common.exception.CustomException;

public class ReportException extends CustomException {

    public ReportException(ReportErrorCode errorCode) {
        super(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }
}
