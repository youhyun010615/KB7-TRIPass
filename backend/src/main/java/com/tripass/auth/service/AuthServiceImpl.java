package com.tripass.auth.service;

import com.tripass.auth.dto.response.CheckLoginIdResponse;
import com.tripass.auth.mapper.UserMapper;
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
import com.tripass.auth.model.RefreshToken;
import com.tripass.auth.security.JwtTokenProvider;
import com.tripass.auth.model.User;
import com.tripass.common.exception.CustomException;
import com.tripass.mypage.mapper.NotificationSettingMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;
import java.util.List;
import java.util.stream.Collectors;

//일반 회원가입 및 로그인 기능 구현 service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    //로그인 아이디 형식 - 영문, 숫자 6~20자
    private static final Pattern LOGIN_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9]{6,20}$");

    private final UserMapper userMapper;
    private final PhoneVerificationService phoneVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final NotificationSettingMapper notificationSettingMapper;

    // 영문, 숫자, 허용된 특수문자를 포함하는 8~64자리
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=?])[A-Za-z\\d!@#$%^&*()_+\\-=?]{8,64}$"
            );

    // 010으로 시작하는 숫자 11자리
    private static final Pattern PHONE_NUMBER_PATTERN =
            Pattern.compile("^010\\d{8}$");


    //회원 가입 아이디 중복체크

    @Override
    public CheckLoginIdResponse checkLoginId(String loginId) {
        String normalizedLoginId = normalizeLoginId(loginId);
        int count = userMapper.countLocalUserByLoginId(normalizedLoginId);

        return new CheckLoginIdResponse(count == 0);
    }

    // 아이디 앞 뒤 공백 제거하고 형식 검사
    private String normalizeLoginId(String loginId) {
        if (loginId == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_LOGIN_ID_REQUIRED",
                    "아이디를 입력해 주세요."
            );
        }
        String normalizedLoginId = loginId.trim();

        if (!LOGIN_ID_PATTERN
                .matcher(normalizedLoginId)
                .matches()) {

            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_INVALID_LOGIN_ID",
                    "아이디는 영문과 숫자로 6~20자 이내로 입력해 주세요."
            );
        }
        return normalizedLoginId;
    }

    //회원가입 처리

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (request == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_SIGNUP_REQUEST_REQUIRED",
                    "회원가입 정보를 입력해 주세요."
            );
        }

        String name =
                normalizeName(request.getName());

        String loginId =
                normalizeLoginId(request.getLoginId());

        String password =
                validatePassword(request.getPassword());

        String phoneNumber =
                normalizePhoneNumber(
                        request.getPhoneNumber()
                );

        String verificationRequestId =
                requireText(
                        request.getPhoneVerificationRequestId(),
                        "휴대전화 인증을 완료해 주세요."
                );

        //프론트의 중복 확인 결과를 신뢰하지 않고실제 회원가입 시점에 다시 검사한다.
        if (userMapper.countLocalUserByLoginId(loginId) > 0) {
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    "AUTH_LOGIN_ID_DUPLICATED",
                    "이미 사용 중인 아이디입니다."
            );
        }

        //인증 목적, 전화번호, 성공 여부,만료 여부와 사용 여부를 검사한다.

        phoneVerificationService
                .validateSignupVerification(
                        verificationRequestId,
                        phoneNumber
                );

        User user = new User();

        user.setName(name);
        user.setLoginId(loginId);
        user.setPassword(
                passwordEncoder.encode(password)
        );
        user.setPhoneNumber(phoneNumber);
        user.setLoginProvider("LOCAL");

        try {
            int insertedRows =
                    userMapper.insertLocalUser(user);

            if (insertedRows != 1) {
                throw new CustomException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "AUTH_SIGNUP_FAILED",
                        "회원가입 처리에 실패했습니다."
                );
            }
            
            // 기본 알림 설정 생성
            notificationSettingMapper.insertDefaultSetting(user.getId());
            
        } catch (DuplicateKeyException exception) {
            // 동시 요청으로 LOCAL 로그인 아이디 유니크 제약조건이 위반된 경우
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    "AUTH_LOGIN_ID_DUPLICATED",
                    "이미 사용 중인 아이디입니다."
            );
        }

        // 회원 저장과 인증 결과 사용 처리는 @Transactional로 하나의 트랜잭션에 포함된다.
        phoneVerificationService
                .markVerificationAsUsed(
                        verificationRequestId
                );

        return new SignupResponse(user.getId());
    }

    //일반 로그인 처리


    @Override
    @Transactional
    public LoginResult login(LoginRequest request) {
        if (request == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_LOGIN_REQUEST_REQUIRED",
                    "로그인 정보를 입력해 주세요."
            );
        }

        String loginId = normalizeLoginId(request.getLoginId());
        String password = requireText(request.getPassword(), "비밀번호를 입력해 주세요");

        User user = userMapper.findLocalUserByLoginId(loginId);

        //아이디가 없거나 비밀번호 틀린 경우 오류 반환
        if (user == null
                || user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_LOGIN_FAILED",
                    "아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }
        String accessToken =
                jwtTokenProvider.createAccessToken(user);

        String refreshToken =
                jwtTokenProvider.createRefreshToken(user);

// Refresh Token 원문은 저장하지 않고 해시하여 DB에 저장한다.
        refreshTokenService.saveRefreshToken(
                user.getId(),
                refreshToken
        );

        LoginResponse.UserInfo userInfo =
                new LoginResponse.UserInfo(
                        user.getId(),
                        user.getLoginId(),
                        user.getName(),
                        user.getLoginProvider()
                );

        LoginResponse loginResponse =
                new LoginResponse(
                        accessToken,
                        "Bearer",
                        jwtTokenProvider
                                .getAccessExpirationSeconds(),
                        userInfo
                );

        return new LoginResult(
                loginResponse,
                refreshToken
        );
    }

    //Refresh Token을 이용해 AccessToken과 RefreshToken을 재발급한다.
    @Override
    @Transactional
    public TokenRefreshResult refreshToken(String refreshToken) {
        //토큰ID, 해시, 만료 및 폐기 여부 검사
        RefreshToken savedToken = refreshTokenService.validateRefreshToken(refreshToken);

        //연결된 회원 조회
        User user = userMapper.findActiveUserById(savedToken.getUserId());

        if (user == null) {
            throw new CustomException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_INVALID_REFRESH_TOKEN",
                    "로그인 정보가 만료되었거나 유효하지 않습니다."
            );
        }
        //기존 RefreshToken 먼저 폐기
        refreshTokenService.revokeRefreshToken(refreshToken);

        //새로운 AccessToken과 RefreshToken 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);

        //새로운 RefreshToken 해시 DB에 저장
        refreshTokenService.saveRefreshToken(user.getId(), newRefreshToken);

        TokenRefreshResponse response =
                new TokenRefreshResponse(
                        newAccessToken,
                        "Bearer",
                        jwtTokenProvider.getAccessExpirationSeconds()
                );
        return new TokenRefreshResult(
                response,
                newRefreshToken
        );
    }

    @Override
    @Transactional
    public FindIdResponse findId(FindIdRequest request) {
        if (request == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_FIND_ID_REQUEST_REQUIRED",
                    "아이디 찾기 정보를 입력해 주세요."
            );
        }

        String name =
                normalizeName(request.getName());

        String phoneNumber =
                normalizePhoneNumber(
                        request.getPhoneNumber()
                );

        String verificationRequestId =
                requireText(
                        request.getPhoneVerificationRequestId(),
                        "휴대전화 인증을 완료해 주세요."
                );

        phoneVerificationService
                .validateFindIdVerification(
                        verificationRequestId,
                        phoneNumber
                );

        List<String> loginIds =
                userMapper
                        .findLocalLoginIdsByNameAndPhoneNumber(
                                name,
                                phoneNumber
                        );

        if (loginIds == null || loginIds.isEmpty()) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "AUTH_USER_NOT_FOUND",
                    "입력한 정보와 일치하는 회원을 찾을 수 없습니다."
            );
        }

        List<String> maskedLoginIds =
                loginIds.stream()
                        .map(this::maskLoginId)
                        .collect(Collectors.toList());

        phoneVerificationService
                .markVerificationAsUsed(
                        verificationRequestId
                );

        return new FindIdResponse(maskedLoginIds);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_RESET_PASSWORD_REQUEST_REQUIRED",
                    "비밀번호 재설정 정보를 입력해 주세요."
            );
        }

        String loginId =
                normalizeLoginId(
                        request.getLoginId()
                );

        String phoneNumber =
                normalizePhoneNumber(
                        request.getPhoneNumber()
                );

        String verificationRequestId =
                requireText(
                        request.getPhoneVerificationRequestId(),
                        "휴대전화 인증을 완료해 주세요."
                );

        String newPassword =
                validatePassword(
                        request.getNewPassword()
                );

        phoneVerificationService
                .validateResetPasswordVerification(
                        verificationRequestId,
                        phoneNumber
                );

        User user =
                userMapper.findLocalUserByLoginId(
                        loginId
                );

        if (user == null
                || !phoneNumber.equals(user.getPhoneNumber())) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_ACCOUNT_INFORMATION_MISMATCH",
                    "아이디와 인증한 휴대전화번호가 일치하지 않습니다."
            );
        }

        if (user.getPassword() != null
                && passwordEncoder.matches(
                newPassword,
                user.getPassword()
        )) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_PASSWORD_SAME_AS_CURRENT",
                    "새 비밀번호는 기존 비밀번호와 다르게 설정해 주세요."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(newPassword);

        int updatedRows =
                userMapper.updatePassword(
                        user.getId(),
                        encodedPassword
                );

        if (updatedRows != 1) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AUTH_PASSWORD_RESET_FAILED",
                    "비밀번호 재설정에 실패했습니다."
            );
        }

        refreshTokenService
                .revokeAllByUserId(
                        user.getId()
                );

        phoneVerificationService
                .markVerificationAsUsed(
                        verificationRequestId
                );
    }

    @Override
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {
        if (userId == null) {
            throw new CustomException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_LOGIN_REQUIRED",
                    "로그인이 필요합니다."
            );
        }

        if (request == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_CHANGE_PASSWORD_REQUEST_REQUIRED",
                    "비밀번호 변경 정보를 입력해 주세요."
            );
        }

        String currentPassword = request.getCurrentPassword();

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_CURRENT_PASSWORD_REQUIRED",
                    "현재 비밀번호를 입력해 주세요."
            );
        }

        String newPassword =
                validatePassword(request.getNewPassword());

        String newPasswordConfirm =
                request.getNewPasswordConfirm();

        if (newPasswordConfirm == null
                || !newPassword.equals(newPasswordConfirm)) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_NEW_PASSWORD_CONFIRM_MISMATCH",
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        User user =
                userMapper.findActiveLocalUserWithPasswordById(
                        userId
                );

        if (user == null || user.getPassword() == null) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "AUTH_LOCAL_USER_NOT_FOUND",
                    "비밀번호를 변경할 수 있는 회원을 찾을 수 없습니다."
            );
        }

        if (!passwordEncoder.matches(
                currentPassword,
                user.getPassword()
        )) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_CURRENT_PASSWORD_MISMATCH",
                    "현재 비밀번호가 일치하지 않습니다."
            );
        }

        if (passwordEncoder.matches(
                newPassword,
                user.getPassword()
        )) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_PASSWORD_SAME_AS_CURRENT",
                    "새 비밀번호는 현재 비밀번호와 다르게 설정해 주세요."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(newPassword);

        int updatedRows =
                userMapper.updatePassword(
                        userId,
                        encodedPassword
                );

        if (updatedRows != 1) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AUTH_PASSWORD_CHANGE_FAILED",
                    "비밀번호 변경에 실패했습니다."
            );
        }

        refreshTokenService.revokeAllByUserId(userId);
    }

    //문자열 길이 검사
    private String normalizeName(String name) {

        String normalizedName =
                requireText(
                        name,
                        "이름을 입력해 주세요."
                );

        if (normalizedName.length() < 2
                || normalizedName.length() > 100) {

            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_INVALID_NAME",
                    "이름은 2~100자로 입력해 주세요."
            );
        }

        return normalizedName;
    }

    // 비밀번호 보안 형식을 검사한다.
    private String validatePassword(String password) {
        if (password == null
                || !PASSWORD_PATTERN
                .matcher(password)
                .matches()) {

            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_INVALID_PASSWORD",
                    "비밀번호는 영문, 숫자, 특수문자를 포함해 8~64자로 입력해 주세요."
            );
        }

        return password;
    }

    //휴대전화번호를 숫자로 정규화하고 형식을 검사한다.
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_PHONE_NUMBER_REQUIRED",
                    "휴대전화번호를 입력해 주세요."
            );
        }

        String normalizedPhoneNumber =
                phoneNumber.replaceAll("[^0-9]", "");

        if (!PHONE_NUMBER_PATTERN
                .matcher(normalizedPhoneNumber)
                .matches()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_INVALID_PHONE_NUMBER",
                    "휴대전화번호는 010으로 시작하는 11자리 번호로 입력해 주세요."
            );
        }

        return normalizedPhoneNumber;
    }

    //필수 문자열의 null과 공백 여부를 검사한다.
    private String requireText(
            String value,
            String errorMessage
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "AUTH_REQUIRED_FIELD_MISSING",
                    errorMessage
            );
        }

        return value.trim();
    }

    // 현재 브라우저의 Refresh Token을 폐기한다.
    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null
                || refreshToken.isBlank()) {
            return;
        }
        refreshTokenService
                .revokeRefreshToken(refreshToken);
    }

    //아이디 마스킹
    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.isEmpty()) {
            return "";
        }

        int visibleLength =
                Math.max(1, loginId.length() / 2);

        int maskedLength =
                loginId.length() - visibleLength;

        return loginId.substring(0, visibleLength)
                + "*".repeat(maskedLength);
    }
}
