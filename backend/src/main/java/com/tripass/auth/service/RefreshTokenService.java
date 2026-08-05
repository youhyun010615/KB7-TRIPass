package com.tripass.auth.service;

import com.tripass.auth.model.RefreshToken;
//RefreshToken 저장, 검증, 폐기 기능
public interface RefreshTokenService {
    //발급한 RefreshToken 저장
    void saveRefreshToken(Long userId, String refreshToken);

    // 전달받은 RefreshToken과 DB 저장 정보를 검증
    RefreshToken validateRefreshToken(String refreshToken);

    //특정 RefreshToken 파기
    void revokeRefreshToken(String refreshToken);

    //특정 회원 RefreshToken 전부 폐기
    void revokeAllByUserId(Long userId);
}
