package com.tripass.auth.service;

import com.tripass.auth.mapper.RefreshTokenMapper;
import com.tripass.auth.model.RefreshToken;
import com.tripass.auth.security.JwtTokenProvider;
import com.tripass.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

//RefreshToken 저장, 검증, 폐기 기능 구현
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService{
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRevocationService refreshTokenRevocationService;

    //발급한 Refresh Token을 해시하여 DB에 저장

    @Override
    @Transactional
    public void saveRefreshToken(Long userId, String refreshToken) {
        if(userId == null){
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AUTH_USER_CONTEXT_MISSING",
                    "회원 정보가 필요합니다."
            );
        }
        String requiredRefreshToken =requireRefreshToken(refreshToken);

        if (!jwtTokenProvider.validateRefreshToken(
                requiredRefreshToken
        )) {
            throw invalidRefreshToken();
        }

// JWT에 저장된 회원 PK를 조회한다.
        Long tokenUserId =
                jwtTokenProvider.getUserId(
                        requiredRefreshToken
                );

// 메서드로 전달된 회원과 JWT의 회원이 같은지 확인한다.
        if (!userId.equals(tokenUserId)) {
            throw invalidRefreshToken();
        }

// JWT에 들어 있는 고유 식별값(jti)을 조회한다.
        String tokenId =
                jwtTokenProvider.getTokenId(
                        requiredRefreshToken
                );

        if (tokenId == null || tokenId.isBlank()) {
            throw invalidRefreshToken();
        }

        RefreshToken tokenEntity = new RefreshToken();

        tokenEntity.setUserId(userId);
        tokenEntity.setTokenId(tokenId);
        tokenEntity.setTokenHash(
                hashToken(requiredRefreshToken)
        );
        tokenEntity.setExpiresAt(
                jwtTokenProvider.getExpirationDate(
                        requiredRefreshToken
                )
        );

        int insertRows = refreshTokenMapper.insertRefreshToken(tokenEntity);
        if(insertRows !=1){
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AUTH_REFRESH_TOKEN_SAVE_FAILED",
                    "로그인 정보 저장에 실패했습니다."
            );
        }
    }

    //Refresh Token의 JWT 정보와 DB 저장 정보를 검증
    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String refreshToken) {
        String requiredRefreshToken = requireRefreshToken(refreshToken);
        // JWT 서명, 만료시간 및 tokenType을 검증
        if (!jwtTokenProvider.validateRefreshToken(requiredRefreshToken)) {
            throw invalidRefreshToken();
        }

        String tokenId = jwtTokenProvider.getTokenId(requiredRefreshToken);
        if (tokenId == null || tokenId.isBlank()) {
            throw invalidRefreshToken();
        }

        RefreshToken savedToken =
                refreshTokenMapper.findByTokenId(tokenId);

        // DB에서 찾을 수 없는 토큰
        if (savedToken == null) {
            throw invalidRefreshToken();
        }
        //이미 폐기된 Refresh Token이 다시 사용됐다면
        //토큰 탈취 또는 재사용 가능성이 있다고 판단한다.
        if (savedToken.getRevokedAt() != null) {

//            별도 트랜잭션에서 해당 회원의 모든 유효한
//            Refresh Token을 폐기한다.
            refreshTokenRevocationService
                    .revokeAllByUserIdRequiresNew(
                            savedToken.getUserId()
                    );

            throw invalidRefreshToken();
        }
        // JWT의 회원 PK와 DB에 저장된 회원 PK가 같은지 확인한다.
        Long tokenUserId =
                jwtTokenProvider.getUserId(
                        requiredRefreshToken
                );

        if (savedToken.getUserId() == null
                || !savedToken.getUserId()
                .equals(tokenUserId)) {

            throw invalidRefreshToken();
        }

        // DB에 저장된 만료일시도 별도로 확인
        if (savedToken.getExpiresAt() == null
                || !savedToken.getExpiresAt()
                .after(new Date())) {

            throw invalidRefreshToken();
        }
        String receivedTokenHash = hashToken(requiredRefreshToken);

        // DB에 저장된 해시와 전달받은 토큰의 해시를 비교한다.
        if(!matchesTokenHash(savedToken.getTokenHash(), receivedTokenHash)){
            throw invalidRefreshToken();
        }
        return savedToken;
    }

    //재발급 또는 로그아웃에 사용된 Refresh Token을 폐기한다.

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        RefreshToken savedToken = validateRefreshToken(refreshToken);

        int updatedRows = refreshTokenMapper.revokeByTokenId(savedToken.getTokenId());
        //동일 토큰 재발급 요청 시 먼저 처리된 요청만 성공
        if (updatedRows != 1) {throw invalidRefreshToken();}
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId) {
        if(userId == null){
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AUTH_USER_CONTEXT_MISSING",
                    "회원 정보가 필요합니다."
            );
        }
        refreshTokenMapper.revokeAllByUserId(userId);
    }

    private String hashToken(String refreshToken){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(
                    refreshToken.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hashBuilder = new StringBuilder();

            for(byte hashByte : hashBytes){
                hashBuilder.append(String.format("%02x",hashByte & 0xff));
            }
            return hashBuilder.toString();
        }catch (NoSuchAlgorithmException exception){
            throw new IllegalStateException("Refresh Token 해시 생성에 실패했습니다.",
                    exception);
        }
    }
    //두 토큰의 해시를 비교
    private boolean matchesTokenHash(String savedTokenHash, String receivedTokenHash){
        if(savedTokenHash == null
                ||receivedTokenHash == null){
            return false;
        }
        return MessageDigest.isEqual(
                savedTokenHash.getBytes(
                        StandardCharsets.UTF_8
                ),
                receivedTokenHash.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
    //RefreshToken의 null과 공백 여부 검사
    private String requireRefreshToken(String refreshToken){
        if(refreshToken == null || refreshToken.isBlank()){
            throw invalidRefreshToken();
        }
        return refreshToken.trim();
    }
    //Refresh Token 검증 실패 응답을 통일
    private CustomException invalidRefreshToken() {
        return new CustomException(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_REFRESH_TOKEN",
                "로그인 정보가 만료되었거나 유효하지 않습니다."
        );
    }
}
