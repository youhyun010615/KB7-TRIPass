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
import com.tripass.auth.security.RefreshTokenCookieProvider;
import com.tripass.auth.service.PhoneVerificationService;
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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

//일반 회원가입 및 로그인 컨트롤러
@Api(tags = "AUTH - 인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

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
                        .getRefreshToken(
                                servletRequest
                        );

        try {
            // 쿠키가 존재하면 DB의 Refresh Token을 폐기한다.
            authService.logout(refreshToken);

        } finally {
            //토큰이 만료되거나 검증에 실패하더라도 브라우저 쿠키는 반드시 삭제한다.
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
}