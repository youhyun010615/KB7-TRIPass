package com.tripass.exchange.exception;

import com.tripass.common.exception.CustomException;

public class ExchangeException extends CustomException {

    public ExchangeException(ExchangeErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage());
    }
}
