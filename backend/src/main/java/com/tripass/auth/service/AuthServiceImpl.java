package com.tripass.auth.service;

import com.tripass.auth.dto.response.CheckLoginIdResponse;
import com.tripass.auth.mapper.UserMapper;
import com.tripass.auth.dto.request.SignupRequest;
import com.tripass.auth.dto.response.SignupResponse;
import com.tripass.auth.dto.request.LoginRequest;
import com.tripass.auth.dto.response.LoginResponse;
import com.tripass.auth.security.JwtTokenProvider;
import com.tripass.auth.model.User;
import com.tripass.common.exception.CustomException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

//일반 회원가입 및 로그인 기능 구현 service
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
    //로그인 아이디 형식 - 영문, 숫자 6~20자
    private static final Pattern LOGIN_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9]{6,20}$");

    private final UserMapper userMapper;
    private final PhoneVerificationService phoneVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 영문, 숫자, 특수문자를 각각 포함하는 8~64자
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,64}$"
            );

    // 010으로 시작하는 숫자 11자리
    private static final Pattern PHONE_NUMBER_PATTERN =
            Pattern.compile("^010\\d{8}$");


    //회원 가입 아이디 중복체크

    @Override
    public CheckLoginIdResponse checkLoginId(String loginId) {
        String normalizedLoginId = normalizeLoginId(loginId);
        int count =userMapper.countLocalUserByLoginId(normalizedLoginId);

        return new CheckLoginIdResponse(count == 0);
    }
    // 아이디 앞 뒤 공백 제거하고 형식 검사
    private String normalizeLoginId(String loginId){
        if(loginId == null){
            throw new IllegalArgumentException(
                    "아이디를 입력해 주세요."
            );
        }
        String normalizedLoginId = loginId.trim();

        if (!LOGIN_ID_PATTERN
                .matcher(normalizedLoginId)
                .matches()) {

            throw new IllegalArgumentException(
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
            throw new IllegalArgumentException(
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
                    "LOGIN_ID_DUPLICATED",
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
                        "SIGNUP_FAILED",
                        "회원가입 처리에 실패했습니다."
                );
            }
        } catch (DataIntegrityViolationException exception) {
            //중복 확인 직후 다른 요청이 같은 아이디를 저장하는 상황도 DB 유니크키로 방지한다.
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    "LOGIN_ID_DUPLICATED",
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
    public LoginResponse login(LoginRequest request) {
        if(request == null){
            throw new IllegalArgumentException("로그인 정보를 입력해주세요");
        }

        String loginId = normalizeLoginId(request.getLoginId());
        String password = requireText(request.getPassword(), "비밀번호를 입력해 주세요");

        User user = userMapper.findLocalUserByLoginId(loginId);

        //아이디가 없거나 비밀번호 틀린 경우 오류 반환
        if(user == null
                || user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())){
            throw new CustomException(
                    HttpStatus.UNAUTHORIZED,
                    "LOGIN_FAILED",
                    "아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }
        String accessToken = jwtTokenProvider.createAccessToken(user);
        LoginResponse.UserInfo userInfo =
                new LoginResponse.UserInfo(
                        user.getId(),
                        user.getLoginId(),
                        user.getName(),
                        user.getLoginProvider()
                );
        return  new LoginResponse(
                accessToken,
                "Bearer",
                jwtTokenProvider.getAccessExpirationSeconds(),
                userInfo
        );
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

            throw new IllegalArgumentException(
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

            throw new IllegalArgumentException(
                    "비밀번호는 영문, 숫자, 특수문자를 포함해 8~64자로 입력해 주세요."
            );
        }

        return password;
    }

    //휴대전화번호를 숫자로 정규화하고 형식을 검사한다.
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException(
                    "휴대전화번호를 입력해 주세요."
            );
        }

        String normalizedPhoneNumber =
                phoneNumber.replaceAll("[^0-9]", "");

        if (!PHONE_NUMBER_PATTERN
                .matcher(normalizedPhoneNumber)
                .matches()) {throw new IllegalArgumentException("휴대전화번호는 010으로 시작하는 11자리 번호로 입력해 주세요.");}

        return normalizedPhoneNumber;
    }

    //필수 문자열의 null과 공백 여부를 검사한다.
    private String requireText(
            String value,
            String errorMessage
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    errorMessage
            );
        }

        return value.trim();
    }
}
