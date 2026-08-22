package com.tripass.auth.service;

import com.tripass.auth.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Refresh Token 보안 폐기 기능 구현체
@Service
@RequiredArgsConstructor
public class RefreshTokenRevocationServiceImpl
        implements RefreshTokenRevocationService{

    private final RefreshTokenMapper refreshTokenMapper;

//    현재 토큰 검증 트랜잭션과 분리된 새 트랜잭션에서 처리한다.
//    이후 Refresh Token 검증 예외가 발생하더라도
//    전체 토큰 폐기 결과가 롤백되지 않도록 한다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllByUserIdRequiresNew(Long userId) {

        if (userId == null) {
            return;
        }

        refreshTokenMapper.revokeAllByUserId(userId);
    }
}
