/*
 * 카테고리 조회 API가 연동되기 전까지 사용하는
 * 영수증 지출 카테고리 임시 목록입니다.
 *
 * spending_categories 테이블의 PK와 반드시 일치해야 합니다.
 */
export const receiptCategories = [
    { id: 1, code: 'FOOD', name: '식비' },
    { id: 2, code: 'TRANSPORT', name: '교통' },
    { id: 3, code: 'LODGING', name: '숙박' },
    { id: 4, code: 'SHOPPING', name: '쇼핑' },
    { id: 5, code: 'SIGHTSEEING', name: '관광' },
    { id: 6, code: 'OTHER', name: '기타' },
    { id: 7, code: 'CAFE', name: '카페' },
    { id: 8, code: 'LIVING', name: '생활비' },
    { id: 9, code: 'LEISURE', name: '취미여가' },
]