import kbTravelersImage from '@/assets/cards/kb-travelers-tosimi.png'
import hanaTravelogImage from '@/assets/travel-cards/hana-travelog.png'
import wooriWibeeTravelImage from '@/assets/travel-cards/woori-wibee-travel.png'
import shinhanSolTravelImage from '@/assets/travel-cards/shinhan-sol-travel.png'
import shinhanSolTravelBackImage from '@/assets/travel-cards/shinhan-sol-travel-back.png'

const SETTLEMENT_TYPE_LABELS = {
    DIRECT: '지원 외화 직접 보유·차감',
    USD_CONVERSION: '현지통화를 USD로 환산 후 차감',
}

// 카드사별 실제 발급 트래블카드 이미지(각 카드사 공식 홈페이지 제공 상품 이미지).
const CARD_COMPANY_IMAGES = [
    { match: 'KB', image: kbTravelersImage },
    { match: '하나', image: hanaTravelogImage },
    { match: '우리', image: wooriWibeeTravelImage },
    { match: '신한', image: shinhanSolTravelImage },
]

// 카드사 공식 홈페이지에서 뒷면 이미지를 구할 수 있었던 카드만 등록한다.
// (체크카드 뒷면은 대부분 마그네틱선·서명란뿐이라 공식 뒷면 이미지를 제공하지 않는 카드사가 많다.)
const CARD_COMPANY_BACK_IMAGES = [
    { match: 'KB', image: kbTravelersImage },
    { match: '신한', image: shinhanSolTravelBackImage },
]

/**
 * 카드사명으로 실제 트래블카드 앞면 이미지를 찾는다. 매칭되는 이미지가 없으면 null을 반환한다.
 *
 * @param {string|null|undefined} cardCompany
 * @returns {string|null}
 */
export function getTravelCardImage(cardCompany) {
    const found = CARD_COMPANY_IMAGES.find(
        (entry) => cardCompany?.includes(entry.match),
    )

    return found?.image || null
}

/**
 * 카드사명으로 실제 트래블카드 뒷면 이미지를 찾는다. 매칭되는 이미지가 없으면 null을 반환한다.
 *
 * @param {string|null|undefined} cardCompany
 * @returns {string|null}
 */
export function getTravelCardBackImage(cardCompany) {
    const found = CARD_COMPANY_BACK_IMAGES.find(
        (entry) => cardCompany?.includes(entry.match),
    )

    return found?.image || null
}

/**
 * 트래블카드 결제 방식을 사용자용 문구로 변환한다.
 *
 * @param {string|null|undefined} settlementType
 * @returns {string}
 */
export function getSettlementTypeLabel(
    settlementType,
) {
    if (
        settlementType === null ||
        settlementType === undefined ||
        String(settlementType).trim() === ''
    ) {
        return '정보 없음'
    }

    return (
        SETTLEMENT_TYPE_LABELS[settlementType] ||
        settlementType
    )
}
