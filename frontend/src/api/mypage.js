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

export function resetAccount() {
    return api.post('/dev/reset-account')
}

export function setOverrideDate(date) {
    // 운영 백엔드의 @RequestParam 계약과 body 기반 계약을 모두 지원한다.
    // 배포 시점에 백엔드 버전이 달라도 같은 날짜가 정상 전달된다.
    return api.post('/dev/override-date', { date }, { params: { date } })
}

export function clearOverrideDate() {
    return api.delete('/dev/override-date')
}

export function getCurrentDate() {
    return api.get('/dev/current-date')
}
