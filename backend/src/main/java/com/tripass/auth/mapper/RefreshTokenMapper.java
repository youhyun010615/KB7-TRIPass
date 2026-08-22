package com.tripass.auth.mapper;

import com.tripass.auth.model.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
//refresh_tokens 테이블에 접근하는 Mapper
@Mapper
public interface RefreshTokenMapper {
    //로그인 시 발급한 RefreshToken정보를 저장
    int insertRefreshToken(RefreshToken refreshToken);

    //JWT의 고유 식별값(jti)로 RefreshToken 조회
    RefreshToken findByTokenId(@Param("tokenId") String tokenId);

    //재발급 또는 로그아웃 시 RefreshToken 폐기
    int revokeByTokenId(@Param("tokenId") String tokenId);

    //특정 회원의 RefreshToken 전부 폐기
    int revokeAllByUserId( @Param("userId") Long userId);

    //만료된 RefreshToken 삭제
    int deleteExpiredTokens(@Param("now") Date now);
}
