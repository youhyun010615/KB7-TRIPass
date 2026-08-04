package com.tripass.auth.service;

import com.tripass.auth.dto.request.PhoneCodeSendRequest;
import com.tripass.auth.dto.request.PhoneCodeVerifyRequest;
import com.tripass.auth.dto.response.PhoneCodeSendResponse;
import com.tripass.auth.mapper.PhoneVerificationMapper;
import com.tripass.auth.model.PhoneVerification;
import com.tripass.auth.model.VerificationPurpose;
import com.tripass.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

//번호인증 기능 구현
@Service
@RequiredArgsConstructor
public class PhoneVerificationServiceImpl implements PhoneVerificationService {

    //인증번호 유효시간 3분
    private static final long CODE_EXPIRATION_MILLIS=
            3*60*1000L;
    //인증번호 재전송 제한: 60초
    private static final long RESEND_COOLDOWN_MILLIS =
            60 * 1000L;
    //인증번호 최대 확인 실패 횟수
    private static final int MAX_ATTEMPT_COUNT = 5;
    // 010으로 시작하는 숫자 11자리
    private static final Pattern PHONE_NUMBER_PATTERN =
            Pattern.compile("^010\\d{8}$");

    // 숫자 6자리 인증번호
    private static final Pattern VERIFICATION_CODE_PATTERN =
            Pattern.compile("^\\d{6}$");
    private final PhoneVerificationMapper phoneVerificationMapper;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom =
            new SecureRandom();
    //인증번호 생성 후 문자 발송, 이후 인증요청 정보 DB에 저장

    @Override
    public PhoneCodeSendResponse sendVerificationCode(PhoneCodeSendRequest request) {
        if(request == null){
            throw new IllegalArgumentException(
                    "인증번호 요청 정보를 입력해 주세요."
            );
        }
        String phoneNumber =
                normalizePhoneNumber(request.getPhoneNumber());

        VerificationPurpose purpose =
                request.getPurpose();
        if (purpose == null) {
            throw new IllegalArgumentException(
                    "휴대전화 인증 목적을 입력해 주세요."
            );
        }
        validateResendCooldown(
                phoneNumber,
                purpose
        );
        String verificationCode =generateVerificationCode();
        //인증번호 암호화해서 DB에 저장
        String verificationCodeHash = passwordEncoder.encode(verificationCode);
        //문자 발송 실패시 DB 저장 안함
        smsService.sendVerificationCode(phoneNumber, verificationCode);
        String requestId = UUID.randomUUID().toString();

        Date expiresAt = new Date(System.currentTimeMillis()+ CODE_EXPIRATION_MILLIS);

        PhoneVerification phoneVerification = new PhoneVerification();
        phoneVerification.setRequestId(requestId);
        phoneVerification.setPhoneNumber(phoneNumber);
        phoneVerification.setVerificationPurpose(purpose);
        phoneVerification.setVerificationCodeHash(verificationCodeHash);
        phoneVerification.setExpiresAt(expiresAt);
        int insertedRows = phoneVerificationMapper.insertPhoneVerification(phoneVerification);
        if (insertedRows != 1) {throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PHONE_VERIFICATION_SAVE_FAILED",
                    "휴대전화 인증 요청 저장에 실패했습니다."
            );
        }
        return new PhoneCodeSendResponse(
                requestId,
                (int) (CODE_EXPIRATION_MILLIS / 1000)
        );
    }
    //사용자가 입력한 인증번호 확인

    @Override
    public void verifyCode(PhoneCodeVerifyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "인증번호 확인 정보를 입력해 주세요."
            );
        }

        String requestId =requireText(request.getRequestId(),"인증 요청 식별값을 입력해 주세요.");
        String phoneNumber =normalizePhoneNumber(request.getPhoneNumber());
        String verificationCode =requireText(request.getCode(),"인증번호를 입력해 주세요.");
        if (!VERIFICATION_CODE_PATTERN
                .matcher(verificationCode)
                .matches()) {
            throw new IllegalArgumentException(
                    "인증번호는 숫자 6자리로 입력해 주세요."
            );
        }
        PhoneVerification phoneVerification = findVerification(requestId);

        validatePhoneNumber(
                phoneVerification,
                phoneNumber
        );
        validateVerificationUsable(phoneVerification);
        //이미 인증에 성공한 요청은 같은 요청에 대해 성공처리
        if (phoneVerification.getVerifiedAt() != null) {return;}


        boolean matches = passwordEncoder.matches(verificationCode, phoneVerification.getVerificationCodeHash());

        if (!matches) {phoneVerificationMapper.incrementAttemptCount(requestId);
            throw new IllegalArgumentException(
                    "인증번호가 일치하지 않습니다."
            );
        }
        int updatedRows = phoneVerificationMapper.markVerified(requestId);

        if (updatedRows != 1) {throw new IllegalArgumentException("인증번호가 만료되었거나 인증할 수 없는 상태입니다.");}
    }
    //회원가입 인증 완료 확인
    @Override
    public void validateSignupVerification(String requestId, String phoneNumber) {
        String normalizedRequestId = requireText(requestId, "휴대전화 인증 요청 식별값이 필요합니다.");

        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);

        PhoneVerification phoneVerification = findVerification(normalizedRequestId);

        validatePhoneNumber(phoneVerification, normalizedPhoneNumber);

        if (phoneVerification.getVerificationPurpose() != VerificationPurpose.SIGNUP) {
            throw new IllegalArgumentException("회원가입용 휴대전화 인증이 아닙니다.");
        }

        validateVerificationUsable(phoneVerification);

        if (phoneVerification.getVerifiedAt() == null) {
            throw new IllegalArgumentException(
                    "휴대전화 인증을 완료해 주세요."
            );
        }
    }
    //인증 결과 사용 완료 처리

    @Override
    public void markVerificationAsUsed(String requestId) {
        String normalizedRequestId = requireText(requestId, "휴대전화 인증 요청 식별값이 필요합니다.");

        int updatedRows = phoneVerificationMapper.markUsed(normalizedRequestId);
        if (updatedRows != 1) {
            throw new IllegalArgumentException(
                    "이미 사용됐거나 사용할 수 없는 휴대전화 인증입니다."
            );
        }
    }


    //SecureRandom으로 숫자 6자리 인증번호를 생성한다.
    private String generateVerificationCode() {
        return String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
    }
    //동일 전화번호와 인증 목적의 최근 발송시긴 조회
    private void validateResendCooldown(String phoneNumber, VerificationPurpose purpose) {
        Date latestCreatedAt = phoneVerificationMapper.findLatestCreatedAt(phoneNumber, purpose);

        if (latestCreatedAt == null) {
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - latestCreatedAt.getTime();
        if (elapsedMillis < RESEND_COOLDOWN_MILLIS) {
            throw new CustomException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "SMS_RESEND_TOO_SOON",
                    "인증번호는 60초 후 다시 요청할 수 있습니다."
            );
        }
    }
    //requestId에 해당하는 인증 요청을 조회한다.
    private PhoneVerification findVerification(String requestId){
        PhoneVerification phoneVerification = phoneVerificationMapper.findByRequestId(requestId);

        if (phoneVerification == null) {
            throw new IllegalArgumentException("유효한 휴대전화 인증 요청을 찾을 수 없습니다.");
        }
        return phoneVerification;
    }
    //인증 요청 전화번호와 입력 전화번호 비교확인
    private void validatePhoneNumber(PhoneVerification phoneVerification, String phoneNumber
    ) {if (!phoneVerification
            .getPhoneNumber()
            .equals(phoneNumber)) {
        throw new IllegalArgumentException("인증을 요청한 휴대전화번호와 일치하지 않습니다.");
        }
    }
    //인증 만료 혹은 사용상태 확인
    private void validateVerificationUsable(PhoneVerification phoneVerification){
        if(phoneVerification.getUsedAt() !=null){
            throw new IllegalArgumentException("이미 사용된 휴대전화 인증입니다.");
        }
        Date now = new Date();
        if(!phoneVerification
                .getExpiresAt()
                .after(now)){
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }
        if(phoneVerification.getAttemptCount() >= MAX_ATTEMPT_COUNT){
            throw new CustomException(HttpStatus.TOO_MANY_REQUESTS,
                    "VERIFICATION_ATTEMPTS_EXCEEDED",
                    "인증번호 확인 횟수를 초과했습니다. 다시 요청해 주세요."
            );
        }
    }
    //전화번호 검사
    private String normalizePhoneNumber(String phoneNumber){
        if(phoneNumber == null){
            throw new IllegalArgumentException("휴대전화번호를 입력해주세요");
        }
        String normalizedPhoneNumber = phoneNumber.replaceAll("[^0-9]", "");
        if(!PHONE_NUMBER_PATTERN
                .matcher(normalizedPhoneNumber)
                .matches()){
            throw new IllegalArgumentException("휴대전화번호는 010으로 시작하는 11자리 번호로 입력해 주세요.");
        }
        return normalizedPhoneNumber;
    }
    //문자열 공백 여부 검사
    private String requireText(String value, String errorMessage){
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value.trim();
    }
}
