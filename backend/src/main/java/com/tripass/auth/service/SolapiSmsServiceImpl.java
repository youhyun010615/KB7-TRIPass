package com.tripass.auth.service;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import com.tripass.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SolapiSmsServiceImpl implements SmsService {
    private final String apiKey;
    private final String apiSecret;
    private final String senderNumber;
    // api 키 가져오기
    public SolapiSmsServiceImpl(
            @Value("${solapi.api-key:}") String apiKey,
            @Value("${solapi.api-secret:}") String apiSecret,
            @Value("${solapi.from:}") String senderNumber
    ){
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.senderNumber = senderNumber;
    }
    //인증번호를 SolAPI로 발송

    @Override
    public void sendVerificationCode(String phoneNumber, String verificationCode) {
        validateConfiguration();
        try {
            DefaultMessageService messageService =
                    SolapiClient.INSTANCE.createInstance(
                            apiKey,
                            apiSecret
                    );

            Message message = new Message();

            message.setFrom(senderNumber);
            message.setTo(phoneNumber);
            message.setText(
                    "[TRIPass] 인증번호는 "
                            + verificationCode
                            + "입니다. 3분 안에 입력해 주세요."
            );

            messageService.send(message, null);

        } catch (Exception exception) {
            //SOLAPI의 내부 오류나 API 키 정보가 클라이언트 응답으로 노출되지 않게 공통 오류로 변환한다.
            throw new CustomException(
                    HttpStatus.BAD_GATEWAY,
                    "SMS_SEND_FAILED",
                    "인증번호 문자 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."
            );
        }
    }
    //SOLAPI 필수 설정값 존재 확인
    private void validateConfiguration() {

        if (apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()
                || senderNumber == null || senderNumber.isBlank()) {

            throw new CustomException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SMS_NOT_CONFIGURED",
                    "문자 발송 설정이 완료되지 않았습니다."
            );
        }
    }
}
