package com.tripass.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
//Refresh Token 모델
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    // Refresh Token PK
    private Long id;

    // 토큰을 발급받은 회원 PK
    private Long userId;

    // JWT에 포함된 고유 식별값(jti)
    private String tokenId;

    // Refresh Token 원문의 SHA-256 해시값
    private String tokenHash;

    // Refresh Token 만료일시
    private Date expiresAt;

    // 로그아웃이나 재발급으로 토큰이 폐기된 일시
    private Date revokedAt;

    // 생성일시
    private Date createdAt;

    // 수정일시
    private Date updatedAt;
}
