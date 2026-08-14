// 010으로 시작하는 숫자 11자리
export const PHONE_NUMBER_PATTERN = /^010\d{8}$/

// 영문, 숫자, 특수문자를 포함하며 공백이 없는 8~64자
export const PASSWORD_PATTERN =
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d\s])\S{8,64}$/

// 숫자 6자리 인증번호
export const VERIFICATION_CODE_PATTERN = /^\d{6}$/