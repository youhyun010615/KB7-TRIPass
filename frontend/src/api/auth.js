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
