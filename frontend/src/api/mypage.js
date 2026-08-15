import api from '@/api'

// 로그인한 회원의 회원정보 조회
export function getMyProfile() {
    return api.get('/users/me')
}

// 로그인한 회원의 TRIPass 내부 이름 변경
export function updateMyProfile({ name }) {
    return api.patch('/users/me', {
        name,
    })
}