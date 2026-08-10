package com.tripass.bank.exception;

import com.tripass.common.exception.CustomException;

public class BankException extends CustomException {

    public BankException(BankErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage());
    }
}
