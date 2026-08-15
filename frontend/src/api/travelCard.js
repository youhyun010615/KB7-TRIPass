import api from '@/api'

/**
 * 트래블카드 목록 조회
 *
 * @param {Object} params
 * @param {string} [params.keyword]
 * @param {string} [params.currencyCode]
 * @param {boolean} [params.instantUse]
 * @param {boolean} [params.transitCard]
 */
export async function fetchTravelCards(params = {}) {
    const response = await api.get(
        '/products/travel-cards',
        { params },
    )

    return response.data.data
}

/**
 * 트래블카드 상세 조회
 *
 * @param {number|string} cardId
 */
export async function fetchTravelCardDetail(cardId) {
    const response = await api.get(
        `/products/travel-cards/${cardId}`,
    )

    return response.data.data
}

/**
 * 선택한 트래블카드 비교 조회
 *
 * @param {Array<number>} cardIds
 */
export async function fetchTravelCardComparison(cardIds) {
    const response = await api.get(
        '/products/travel-cards/comparison',
        {
            params: {
                cardIds: cardIds.join(','),
            },
        },
    )

    return response.data.data
}