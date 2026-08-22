package com.tripass.auth.client;

import com.tripass.auth.dto.external.google.GoogleTokenResponse;
import com.tripass.auth.dto.external.google.GoogleUserResponse;
import com.tripass.common.exception.CustomException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
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

import java.util.Collections;

// Google OAuth 토큰 발급 및 사용자 정보 조회 Client
@Log4j2
@Component
public class GoogleOAuthClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizationUri;
    private final String tokenUri;
    private final String userInfoUri;
    private final String scope;

    public GoogleOAuthClient(
            @Qualifier("googleRestTemplate")
            RestTemplate restTemplate,

            @Value("${google.oauth.client-id}")
            String clientId,

            @Value("${google.oauth.client-secret}")
            String clientSecret,

            @Value("${google.oauth.redirect-uri}")
            String redirectUri,

            @Value("${google.oauth.authorization-uri}")
            String authorizationUri,

            @Value("${google.oauth.token-uri}")
            String tokenUri,

            @Value("${google.oauth.user-info-uri}")
            String userInfoUri,

            @Value("${google.oauth.scope}")
            String scope
    ) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
        this.scope = scope;
    }

    // Google 로그인 인가 URL을 생성한다.
    public String createAuthorizationUrl(
            String state
    ) {
        if (!StringUtils.hasText(state)) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "GOOGLE_OAUTH_STATE_CREATE_FAILED",
                    "Google 로그인 요청 생성에 실패했습니다."
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
                .queryParam("scope", scope)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    // Google 인가 코드를 Access Token으로 교환한다.
    public GoogleTokenResponse requestAccessToken(
            String authorizationCode
    ) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "GOOGLE_AUTHORIZATION_CODE_REQUIRED",
                    "Google 인가 코드가 필요합니다."
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

        body.add(
                "grant_type",
                "authorization_code"
        );
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add(
                "code",
                authorizationCode.trim()
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GoogleTokenResponse> response =
                    restTemplate.exchange(
                            tokenUri,
                            HttpMethod.POST,
                            request,
                            GoogleTokenResponse.class
                    );

            GoogleTokenResponse tokenResponse =
                    response.getBody();

            if (
                    tokenResponse == null ||
                            !StringUtils.hasText(
                                    tokenResponse.getAccessToken()
                            )
            ) {
                throw new CustomException(
                        HttpStatus.BAD_GATEWAY,
                        "GOOGLE_TOKEN_RESPONSE_INVALID",
                        "Google 토큰 응답을 확인할 수 없습니다."
                );
            }

            return tokenResponse;

        } catch (CustomException exception) {
            throw exception;

        } catch (RestClientException exception) {
            log.warn(
                    "Google access token request failed: type={}, message={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            throw new CustomException(
                    HttpStatus.BAD_GATEWAY,
                    "GOOGLE_TOKEN_REQUEST_FAILED",
                    "Google 인증 처리에 실패했습니다."
            );
        }
    }

    // Google Access Token으로 사용자 정보를 조회한다.
    public GoogleUserResponse requestUserInfo(
            String googleAccessToken
    ) {
        if (!StringUtils.hasText(googleAccessToken)) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "GOOGLE_ACCESS_TOKEN_REQUIRED",
                    "Google Access Token이 필요합니다."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(
                googleAccessToken
        );
        headers.setAccept(
                Collections.singletonList(
                        MediaType.APPLICATION_JSON
                )
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUserResponse> response =
                    restTemplate.exchange(
                            userInfoUri,
                            HttpMethod.GET,
                            request,
                            GoogleUserResponse.class
                    );

            GoogleUserResponse userResponse =
                    response.getBody();

            if (
                    userResponse == null ||
                            !StringUtils.hasText(
                                    userResponse.getSub()
                            )
            ) {
                throw new CustomException(
                        HttpStatus.BAD_GATEWAY,
                        "GOOGLE_USER_RESPONSE_INVALID",
                        "Google 사용자 정보를 확인할 수 없습니다."
                );
            }

            return userResponse;

        } catch (CustomException exception) {
            throw exception;

        } catch (RestClientException exception) {
            log.warn(
                    "Google user info request failed: type={}, message={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            throw new CustomException(
                    HttpStatus.BAD_GATEWAY,
                    "GOOGLE_USER_REQUEST_FAILED",
                    "Google 사용자 정보 조회에 실패했습니다."
            );
        }
    }
}