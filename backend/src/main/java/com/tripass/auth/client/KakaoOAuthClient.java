package com.tripass.auth.client;

import com.tripass.auth.dto.external.kakao.KakaoTokenResponse;
import com.tripass.auth.dto.external.kakao.KakaoUserResponse;
import com.tripass.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Qualifier;

import lombok.extern.log4j.Log4j2;

import java.util.Collections;

// 카카오 OAuth 토큰 발급 및 사용자 정보 조회 Client
@Log4j2
@Component
public class KakaoOAuthClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenUri;
    private final String userInfoUri;
    private final String authorizationUri;

    public KakaoOAuthClient(
            @Qualifier("kakaoRestTemplate")
            RestTemplate restTemplate,
            @Value("${kakao.oauth.client-id}")
            String clientId,
            @Value("${kakao.oauth.client-secret:}")
            String clientSecret,
            @Value("${kakao.oauth.redirect-uri}")
            String redirectUri,
            @Value("${kakao.oauth.token-uri}")
            String tokenUri,
            @Value("${kakao.oauth.user-info-uri}")
            String userInfoUri,
            @Value("${kakao.oauth.authorization-uri}")
            String authorizationUri
    ) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
        this.authorizationUri = authorizationUri;
    }

    public String createAuthorizationUrl(
            String state
    ) {
        if (!StringUtils.hasText(state)) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "KAKAO_OAUTH_STATE_CREATE_FAILED",
                    "카카오 로그인 요청 생성에 실패했습니다."
            );
        }

        return UriComponentsBuilder
                .fromHttpUrl(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam(
                        "response_type",
                        "code"
                )
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    // 카카오 인가 코드를 Access Token으로 교환한다.
    public KakaoTokenResponse requestAccessToken(
            String authorizationCode
    ) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "KAKAO_AUTHORIZATION_CODE_REQUIRED",
                    "카카오 인가 코드가 필요합니다."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );
        headers.setAccept(
                Collections.singletonList(
                        MediaType.APPLICATION_JSON
                )
        );

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", authorizationCode.trim());

        if (StringUtils.hasText(clientSecret)) {
            body.add(
                    "client_secret",
                    clientSecret.trim()
            );
        }

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response =
                    restTemplate.exchange(
                            tokenUri,
                            HttpMethod.POST,
                            request,
                            KakaoTokenResponse.class
                    );

            KakaoTokenResponse tokenResponse =
                    response.getBody();

            if (tokenResponse == null
                    || !StringUtils.hasText(
                    tokenResponse.getAccessToken()
            )) {
                throw new CustomException(
                        HttpStatus.BAD_GATEWAY,
                        "KAKAO_TOKEN_RESPONSE_INVALID",
                        "카카오 토큰 응답을 확인할 수 없습니다."
                );
            }

            return tokenResponse;

        } catch (RestClientException exception) {
            log.warn(
                    "Kakao access token request failed: type={}, message={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            throw new CustomException(
                    HttpStatus.BAD_GATEWAY,
                    "KAKAO_TOKEN_REQUEST_FAILED",
                    "카카오 인증 처리에 실패했습니다."
            );
        }
    }

    // 카카오 Access Token으로 사용자 정보를 조회한다.
    public KakaoUserResponse requestUserInfo(
            String kakaoAccessToken
    ) {
        if (!StringUtils.hasText(kakaoAccessToken)) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "KAKAO_ACCESS_TOKEN_REQUIRED",
                    "카카오 Access Token이 필요합니다."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);
        headers.setAccept(
                Collections.singletonList(
                        MediaType.APPLICATION_JSON
                )
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoUserResponse> response =
                    restTemplate.exchange(
                            userInfoUri,
                            HttpMethod.GET,
                            request,
                            KakaoUserResponse.class
                    );

            KakaoUserResponse userResponse =
                    response.getBody();

            if (userResponse == null
                    || userResponse.getId() == null) {
                throw new CustomException(
                        HttpStatus.BAD_GATEWAY,
                        "KAKAO_USER_RESPONSE_INVALID",
                        "카카오 사용자 정보를 확인할 수 없습니다."
                );
            }

            return userResponse;

        } catch (CustomException exception) {
            throw exception;

        } catch (RestClientException exception) {
            log.warn(
                    "Kakao user info request failed: type={}, message={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            throw new CustomException(
                    HttpStatus.BAD_GATEWAY,
                    "KAKAO_USER_REQUEST_FAILED",
                    "카카오 사용자 정보 조회에 실패했습니다."
            );
        }
    }
}