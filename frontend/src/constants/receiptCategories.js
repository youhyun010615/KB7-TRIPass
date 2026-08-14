/*
 * 카테고리 조회 API가 연동되기 전까지 사용하는
 * 영수증 지출 카테고리 임시 목록입니다.
 *
 * spending_categories 테이블의 PK와 순서가
 * 반드시 일치해야 합니다.
 */
export const receiptCategories = [
    { id: 1, name: '식비' },
    { id: 2, name: '교통' },
    { id: 3, name: '숙박' },
    { id: 4, name: '쇼핑' },
    { id: 5, name: '관광' },
    { id: 6, name: '기타' },
]