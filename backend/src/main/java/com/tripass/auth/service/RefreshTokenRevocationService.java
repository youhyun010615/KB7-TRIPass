package com.tripass.auth.service;

// Refresh Token 보안 폐기 기능을 정의하는 Service
public interface RefreshTokenRevocationService {
//    폐기된 Refresh Token 재사용이 탐지되면
//    해당 회원의 모든 Refresh Token을 별도 트랜잭션으로 폐기한다.
    void revokeAllByUserIdRequiresNew(Long userId);
}
