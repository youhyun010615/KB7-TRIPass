package com.tripass.auth.controller;

import com.tripass.auth.dto.response.CheckLoginIdResponse;
import com.tripass.auth.service.AuthService;
import com.tripass.common.response.ApiResponse;
import com.tripass.auth.dto.request.PhoneCodeSendRequest;
import com.tripass.auth.dto.request.PhoneCodeVerifyRequest;
import com.tripass.auth.dto.response.PhoneCodeSendResponse;
import com.tripass.auth.dto.request.SignupRequest;
import com.tripass.auth.dto.response.SignupResponse;
import com.tripass.auth.dto.request.LoginRequest;
import com.tripass.auth.dto.response.LoginResponse;
import com.tripass.auth.dto.internal.LoginResult;
import com.tripass.auth.dto.internal.TokenRefreshResult;
import com.tripass.auth.dto.response.TokenRefreshResponse;
import com.tripass.auth.dto.request.FindIdRequest;
import com.tripass.auth.dto.response.FindIdResponse;
import com.tripass.auth.dto.request.ResetPasswordRequest;
import com.tripass.auth.dto.request.ChangePasswordRequest;
import com.tripass.auth.dto.request.KakaoLoginRequest;
import com.tripass.auth.dto.response.KakaoAuthorizationUrlResponse;
import com.tripass.auth.dto.request.GoogleLoginRequest;
import com.tripass.auth.dto.response.GoogleAuthorizationUrlResponse;
import com.tripass.auth.security.OAuthStateCookieProvider;
import com.tripass.auth.security.OAuthProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import com.tripass.auth.security.RefreshTokenCookieProvider;
import com.tripass.auth.service.PhoneVerificationService;
import com.tripass.common.exception.CustomException;
import lombok.extern.log4j.Log4j2;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

//일반 회원가입 및 로그인 컨트롤러
@Api(tags = "AUTH - 인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;
    private final OAuthStateCookieProvider oauthStateCookieProvider;

    //아이디 중복 확인
    @ApiOperation(
            value = "아이디 중복 확인",
            notes = "일반 회원가입에 사용할 로그인 아이디의 중복 여부를 확인합니다."
    )
    @GetMapping("/check-id")
    public ApiResponse<CheckLoginIdResponse> checkLoginId(
            @ApiParam(
                    value = "중복 확인할 로그인 아이디",
                    required = true,
                    example = "tripass01"
            )
            @RequestParam("loginId") String loginId
    ){
        CheckLoginIdResponse response =
                authService.checkLoginId(loginId);
        return ApiResponse.success(response);
    }
    //인증번호 발송
    @ApiOperation(
            value = "휴대전화 인증번호 발송",
            notes = "휴대전화번호와 인증 목적을 받아 숫자 6자리 인증번호를 발송합니다."
    )
    @PostMapping("/phone/send")
    public ApiResponse<PhoneCodeSendResponse> sendPhoneCode(
            @ApiParam(
                    value = "휴대전화 인증번호 발송 정보",
                    required = true
            )
            @RequestBody PhoneCodeSendRequest request
    ) {
        PhoneCodeSendResponse response =
                phoneVerificationService
                        .sendVerificationCode(request);

        return ApiResponse.success(
                "인증번호를 발송했습니다.",
                response
        );
    }
    //인증번호 확인
    @ApiOperation(
            value = "휴대전화 인증번호 확인",
            notes = "인증 요청 식별값, 휴대전화번호, 숫자 6자리 인증번호를 확인합니다."
    )
    @PostMapping("/phone/verify")
    public ApiResponse<Void> verifyPhoneCode(
            @ApiParam(
                    value = "휴대전화 인증번호 확인 정보",
                    required = true
            )
            @RequestBody PhoneCodeVerifyRequest request
    ) {
        phoneVerificationService.verifyCode(request);

        return ApiResponse.success(
                "휴대전화 인증이 완료되었습니다.",
                null
        );
    }
    //번호 인증 후 회원가입 처리
    @ApiOperation(
            value = "일반 회원가입",
            notes = "아이디 중복 여부와 휴대전화 인증 상태를 확인한 후 일반 회원가입을 처리합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @ApiParam(
                    value = "일반 회원가입 정보",
                    required = true
            )
            @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "회원가입이 완료되었습니다.",
                                response
                        )
                );
    }

    //일반 로그인
    @ApiOperation(
            value = "일반 로그인",
            notes = "회원의 아이디와 비밀번호를 확인하고 JWT를 발급합니다."
    )
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @ApiParam(
                    value = "일반 로그인 정보",
                    required = true
            )
            @RequestBody LoginRequest request,
            HttpServletResponse servletResponse
    ){
        LoginResult result = authService.login(request);
        // Refresh Token은 HttpOnly 쿠키로 전달한다.
        refreshTokenCookieProvider
                .addRefreshTokenCookie(
                        servletResponse,
                        result.getRefreshToken()
                );
        // Access Token과 회원 정보만 JSON으로 반환한다.
        return ApiResponse.success(
                "로그인이 완료되었습니다.",
                result.getLoginResponse()
        );
    }

    @ApiOperation(
            value = "카카오 로그인 인가 URL 생성",
            notes = "카카오 OAuth state를 생성하여 "
                    + "HttpOnly 쿠키에 저장하고 "
                    + "카카오 인가 URL을 반환합니다."
    )
    @GetMapping("/social/kakao/authorization-url")
    public ApiResponse<KakaoAuthorizationUrlResponse>
    createKakaoAuthorizationUrl(
            HttpServletResponse servletResponse
    ) {
        String state =
                oauthStateCookieProvider.generateState();

        String authorizationUrl =
                authService.createKakaoAuthorizationUrl(state);

        oauthStateCookieProvider.addStateCookie(
                servletResponse,
                OAuthProvider.KAKAO,
                state
        );

        return ApiResponse.success(
                new KakaoAuthorizationUrlResponse(
                        authorizationUrl
                )
        );
    }

    @ApiOperation(
            value = "Google 로그인 인가 URL 생성",
            notes = "Google OAuth state를 생성하여 "
                    + "HttpOnly 쿠키에 저장하고 "
                    + "Google 인가 URL을 반환합니다."
    )
    @GetMapping("/social/google/authorization-url")
    public ApiResponse<GoogleAuthorizationUrlResponse>
    createGoogleAuthorizationUrl(
            HttpServletResponse servletResponse
    ) {
        String state =
                oauthStateCookieProvider
                        .generateState();

        String authorizationUrl =
                authService
                        .createGoogleAuthorizationUrl(
                                state
                        );

        oauthStateCookieProvider
                .addStateCookie(
                        servletResponse,
                        OAuthProvider.GOOGLE,
                        state
                );

        return ApiResponse.success(
                new GoogleAuthorizationUrlResponse(
                        authorizationUrl
                )
        );
    }

    // 카카오 소셜 로그인
    @ApiOperation(
            value = "카카오 소셜 로그인",
            notes = "카카오 인가 코드로 사용자 정보를 조회하고 "
                    + "기존 카카오 회원 로그인 또는 신규 회원가입 후 "
                    + "TRIPass JWT를 발급합니다."
    )
    @PostMapping("/social/kakao")
    public ApiResponse<LoginResponse> kakaoLogin(
            @ApiParam(
                    value = "카카오 로그인 인가 코드",
                    required = true
            )
            @Valid
            @RequestBody
            KakaoLoginRequest request,

            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {

        String expectedState =
                oauthStateCookieProvider
                        .getState(
                                servletRequest,
                                OAuthProvider.KAKAO
                        );

        try {
            oauthStateCookieProvider
                    .validateState(
                            expectedState,
                            request.getState(),
                            OAuthProvider.KAKAO
                    );
        } finally {
            oauthStateCookieProvider
                    .deleteStateCookie(
                            servletResponse,
                            OAuthProvider.KAKAO
                    );
        }

        /*
         * state 검증에 성공한 경우에만
         * 카카오 인가 코드를 교환한다.
         */
        LoginResult result =
                authService.kakaoLogin(request);

        // Refresh Token은 HttpOnly 쿠키로 전달한다.
        refreshTokenCookieProvider
                .addRefreshTokenCookie(
                        servletResponse,
                        result.getRefreshToken()
                );

        // Access Token과 회원정보만 응답 본문으로 반환한다.
        return ApiResponse.success(
                "카카오 로그인이 완료되었습니다.",
                result.getLoginResponse()
        );
    }

    // Google 소셜 로그인
    @ApiOperation(
            value = "Google 소셜 로그인",
            notes = "Google 인가 코드로 사용자 정보를 조회하고 "
                    + "기존 Google 회원 로그인 또는 신규 회원가입 후 "
                    + "TRIPass JWT를 발급합니다."
    )
    @PostMapping("/social/google")
    public ApiResponse<LoginResponse> googleLogin(
            @ApiParam(
                    value = "Google 로그인 인가 코드와 OAuth state",
                    required = true
            )
            @Valid
            @RequestBody
            GoogleLoginRequest request,

            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String expectedState =
                oauthStateCookieProvider
                        .getState(
                                servletRequest,
                                OAuthProvider.GOOGLE
                        );

        try {
            oauthStateCookieProvider
                    .validateState(
                            expectedState,
                            request.getState(),
                            OAuthProvider.GOOGLE
                    );
        } finally {
            oauthStateCookieProvider
                    .deleteStateCookie(
                            servletResponse,
                            OAuthProvider.GOOGLE
                    );
        }

        /*
         * state 검증에 성공한 경우에만
         * Google 인가 코드를 교환한다.
         */
        LoginResult result =
                authService.googleLogin(
                        request
                );

        // TRIPass Refresh Token은 HttpOnly 쿠키로 전달한다.
        refreshTokenCookieProvider
                .addRefreshTokenCookie(
                        servletResponse,
                        result.getRefreshToken()
                );

        // Access Token과 회원정보만 응답 본문으로 반환한다.
        return ApiResponse.success(
                "Google 로그인이 완료되었습니다.",
                result.getLoginResponse()
        );
    }

    // Access Token 및 Refresh Token 재발급
    @ApiOperation(
            value = "토큰 재발급",
            notes = "HttpOnly 쿠키의 Refresh Token을 검증하고 새로운 토큰을 발급합니다."
    )
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        // 요청 쿠키에서 기존 Refresh Token을 조회한다.
        String refreshToken =
                refreshTokenCookieProvider
                        .getRefreshToken(
                                servletRequest
                        );

        // 기존 토큰을 검증·폐기하고 새로운 토큰을 발급한다.
        TokenRefreshResult result =
                authService.refreshToken(
                        refreshToken
                );

        // 새 Refresh Token으로 HttpOnly 쿠키를 교체한다.
        refreshTokenCookieProvider
                .addRefreshTokenCookie(
                        servletResponse,
                        result.getRefreshToken()
                );

        // 새 Access Token 정보만 JSON 응답으로 반환한다.
        return ApiResponse.success(
                "토큰이 재발급되었습니다.",
                result.getTokenRefreshResponse()
        );
    }

    // 로그아웃
    @ApiOperation(
            value = "로그아웃",
            notes = "Refresh Token을 폐기하고 HttpOnly 쿠키를 삭제합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String refreshToken =
                refreshTokenCookieProvider
                        .getRefreshToken(servletRequest);

        try {
            // 쿠키가 존재하면 DB에 저장된 Refresh Token을 폐기한다.
            authService.logout(refreshToken);

        } catch (CustomException exception) {
             //만료되거나 이미 폐기된 토큰의 로그아웃 요청은
             //사용자 관점에서 이미 로그아웃된 상태이므로 성공 처리한다.

            if (!"AUTH_INVALID_REFRESH_TOKEN"
                    .equals(exception.getErrorCode())) {

                // DB 저장 실패 등 다른 예외는 숨기지 않는다.
                throw exception;
            }

            log.debug(
                    "이미 만료되거나 폐기된 Refresh Token 로그아웃 요청"
            );

        } finally {
            //토큰이 없거나 만료됐거나 DB 폐기에 실패하더라도
            // 브라우저의 Refresh Token 쿠키는 반드시 삭제한다.
            refreshTokenCookieProvider
                    .deleteRefreshTokenCookie(
                            servletResponse
                    );
        }

        return ApiResponse.success(
                "로그아웃이 완료되었습니다.",
                null
        );
    }

    // 휴대전화 인증 기반 아이디 찾기
    @ApiOperation(
            value = "아이디 찾기",
            notes = "FIND_ID 목적으로 휴대전화 인증을 완료한 후 "
                    + "이름과 전화번호가 일치하는 LOCAL 계정의 마스킹 아이디를 조회합니다."
    )
    @PostMapping("/find-id")
    public ApiResponse<FindIdResponse> findId(
            @ApiParam(
                    value = "아이디 찾기 요청 정보",
                    required = true
            )
            @RequestBody FindIdRequest request
    ) {
        FindIdResponse response =
                authService.findId(request);

        return ApiResponse.success(
                "아이디 찾기가 완료되었습니다.",
                response
        );
    }

    // 휴대전화 인증 기반 비밀번호 재설정
    @ApiOperation(
            value = "비밀번호 재설정",
            notes = "RESET_PASSWORD 목적으로 휴대전화 인증을 완료한 후 "
                    + "아이디와 인증 전화번호가 일치하는 LOCAL 계정의 비밀번호를 재설정합니다."
    )
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @ApiParam(
                    value = "비밀번호 재설정 요청 정보",
                    required = true
            )
            @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);

        return ApiResponse.success(
                "비밀번호가 재설정되었습니다.",
                null
        );
    }

    @ApiOperation(
            value = "로그인 상태 비밀번호 변경",
            notes = "로그인한 LOCAL 회원의 현재 비밀번호를 확인한 후 "
                    + "새 비밀번호로 변경합니다. 변경 후 모든 Refresh Token을 폐기합니다."
    )
    @PutMapping("/password/change")
    public ApiResponse<Void> changePassword(
            @ApiIgnore Authentication authentication,

            @ApiParam(
                    value = "비밀번호 변경 요청 정보",
                    required = true
            )
            @RequestBody ChangePasswordRequest request,

            HttpServletResponse servletResponse
    ) {
        Long userId =
                (Long) authentication.getPrincipal();

        authService.changePassword(
                userId,
                request
        );

        refreshTokenCookieProvider
                .deleteRefreshTokenCookie(servletResponse);

        return ApiResponse.success(
                "비밀번호가 변경되었습니다. 다시 로그인해 주세요.",
                null
        );
    }
}