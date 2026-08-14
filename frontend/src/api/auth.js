import api from '@/api'

// 회원가입 아이디 중복 여부 확인
export function checkLoginId(loginId) {
    return api.get('/auth/check-id', {
        params: {
            loginId,
        },
    })
}

// 목적에 따른 휴대전화 인증번호 발송
export function sendPhoneCode({phoneNumber, purpose,}) {
    return api.post('/auth/phone/send', {
        phoneNumber,
        purpose,
    })
}

// 사용자가 입력한 휴대전화 인증번호 확인
export function verifyPhoneCode({
                                    requestId,
                                    phoneNumber,
                                    code,
                                }) {
    return api.post('/auth/phone/verify', {
        requestId,
        phoneNumber,
        code,
    })
}

// 일반 회원가입
export function signup({
                           name,
                           loginId,
                           password,
                           phoneNumber,
                           phoneVerificationRequestId,
                       }) {
    return api.post('/auth/signup', {
        name,
        loginId,
        password,
        phoneNumber,
        phoneVerificationRequestId,
    })
}

// 일반 로그인
export function login({
                          loginId,
                          password,
                      }) {
    return api.post('/auth/login', {
        loginId,
        password,
    })
}

// 아이디 찾기
export function findId({
                           name,
                           phoneNumber,
                           phoneVerificationRequestId,
                       }) {
    return api.post('/auth/find-id', {
        name,
        phoneNumber,
        phoneVerificationRequestId,
    })
}

// 휴대전화 인증 기반 비밀번호 재설정
export function resetPassword({
                                  loginId,
                                  phoneNumber,
                                  phoneVerificationRequestId,
                                  newPassword,
                              }) {
    return api.post('/auth/reset-password', {
        loginId,
        phoneNumber,
        phoneVerificationRequestId,
        newPassword,
    })
}

// 로그인 상태에서 비밀번호 변경
export function changePassword({
                                   currentPassword,
                                   newPassword,
                                   newPasswordConfirm,
                               }) {
    return api.put('/auth/password/change', {
        currentPassword,
        newPassword,
        newPasswordConfirm,
    })
}

// Refresh Token 쿠키를 이용한 Access Token 재발급
export function refreshAccessToken() {
    return api.post('/auth/refresh')
}

// 현재 브라우저의 Refresh Token 폐기 및 쿠키 삭제
export function logout() {
    return api.post('/auth/logout')
}

// 백엔드에서 카카오 OAuth state와 인가 URL을 발급받는다.
export function getKakaoAuthorizationUrl() {
    return api.get(
        '/auth/social/kakao/authorization-url',
    )
}

// 카카오 콜백으로 전달된 인가 코드와 state를 백엔드에 전달한다.
export function loginWithKakao({
                                   code,
                                   state,
                               }) {
    return api.post(
        '/auth/social/kakao',
        {
            code,
            state,
        },
    )
}

// 백엔드에서 Google OAuth state와 인가 URL을 발급받는다.
export function getGoogleAuthorizationUrl() {
    return api.get(
        '/auth/social/google/authorization-url',
    )
}

// Google 콜백으로 전달된 인가 코드와 state를 백엔드에 전달한다.
export function loginWithGoogle({
                                    code,
                                    state,
                                }) {
    return api.post(
        '/auth/social/google',
        {
            code,
            state,
        },
    )
}
