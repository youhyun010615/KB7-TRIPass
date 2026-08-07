import api from '@/api'

//회원가입 아이디 중복 여부 확인
export function checkLoginId(loginId){
    return api.get('/auth/check-id',{
        params:{
            loginId,
        },
    })
}

//휴대전화 인증번호 발송
export function sendPhoneCode(phoneNumber){
    return api.post('/auth/phone/send',{
        phoneNumber,
        purpose: 'SIGNUP',
    })
}

//사용자가 입력한 휴대전화 인증번호 확인
export function verifyPhoneCode({
    requestId,
    phoneNumber,
    code,}){
    return api.post('/auth/phone/verify',{
        requestId,
        phoneNumber,
        code,
    })
}

//일반 회원가입
export function signup({
    name,
    loginId,
    password,
    phoneNumber,
    phoneVerificationRequestId,
                       }){
    return api.post('/auth/signup', {
        name,
        loginId,
        password,
        phoneNumber,
        phoneVerificationRequestId,
    })
}

//일반 로그인
export function login({loginId, password,}){
    return api.post('/auth/login',{
        loginId,
        password,
    })
}

//Refresh Token 쿠키를 이용한 Access Token 재발급
export function refreshAccessToken() {
    return api.post('/auth/refresh')
}

//현재 브라우저의 RefreshToken 폐기 및 쿠키 삭제
export function logout() {
    return api.post('/auth/logout')
}