const SETTLEMENT_TYPE_LABELS = {
    DIRECT: '지원 외화 직접 보유·차감',
    USD_CONVERSION: '현지통화를 USD로 환산 후 차감',
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