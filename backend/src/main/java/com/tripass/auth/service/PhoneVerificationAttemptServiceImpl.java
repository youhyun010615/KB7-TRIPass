package com.tripass.auth.service;

import com.tripass.auth.mapper.PhoneVerificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 인증번호 확인 실패 횟수 저장 기능 구현체
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneVerificationAttemptServiceImpl implements PhoneVerificationAttemptService{
    private final PhoneVerificationMapper phoneVerificationMapper;

    //verifyCode의 트랜잭션과 분리된 새 트랜잭션을 시작한다.
    //이후 verifyCode에서 예외가 발생해도 실패 횟수는 롤백되지 않는다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseFailedAttempt(String requestId) {

        int updatedRows =
                phoneVerificationMapper.incrementAttemptCount(requestId);

        if (updatedRows != 1) {
             //이미 5회에 도달했거나, 인증 완료·사용 완료된 요청이면
             //더 이상 실패 횟수를 증가시키지 않는다.
            return;
        }
    }
}
