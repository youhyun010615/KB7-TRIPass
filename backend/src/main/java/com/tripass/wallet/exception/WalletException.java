package com.tripass.wallet.exception;

import com.tripass.common.exception.CustomException;

/** 월렛 도메인 예외를 공통 예외 응답 형식으로 전달하기 위한 커스텀 예외 클래스입니다. */

public class WalletException extends CustomException {

    public WalletException(WalletErrorCode errorCode) {
        super(
                errorCode.getStatus(),
                errorCode.name(),
                errorCode.getMessage()
        );
    }
}