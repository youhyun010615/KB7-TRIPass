import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

import {
    fetchTravelCardComparison,
    fetchTravelCardDetail,
    fetchTravelCards,
} from '@/api/travelCard'

const STORAGE_KEY = 'tripass-travel-card-comparison'

function loadComparedCardIds() {
    try {
        const storedValue = localStorage.getItem(STORAGE_KEY)

        if (!storedValue) {
            return []
        }

        const parsedValue = JSON.parse(storedValue)

        if (!Array.isArray(parsedValue)) {
            return []
        }

        return [...new Set(
            parsedValue
                .map((cardId) => Number(cardId))
                .filter((cardId) =>
                    Number.isInteger(cardId) && cardId > 0,
                ),
        )].slice(0, 3)
    } catch {
        return []
    }
}

function getErrorMessage(
    error,
    fallbackMessage,
) {
    return (
        error.response?.data?.message ||
        error.message ||
        fallbackMessage
    )
}

function areSameCardIds(leftIds, rightIds) {
    return (
        leftIds.length === rightIds.length &&
        leftIds.every(
            (cardId, index) =>
                cardId === rightIds[index],
        )
    )
}

export const useTravelCardsStore = defineStore(
    'travelCards',
    () => {
        const cards = ref([])
        const selectedCard = ref(null)
        const comparisonCards = ref([])

        const keyword = ref('')
        const currencyCode = ref('')
        const instantUse = ref(null)
        const transitCard = ref(null)

        const comparedCardIds = ref(
            loadComparedCardIds(),
        )

        const listLoading = ref(false)
        const detailLoading = ref(false)
        const comparisonLoading = ref(false)
        const errorMessage = ref('')
        // 가장 최근 목록·상세·비교 요청만 상태에 반영하기 위한 요청 순번
        let listRequestSequence = 0
        let detailRequestSequence = 0
        let comparisonRequestSequence = 0

        const comparedCardCount = computed(
            () => comparedCardIds.value.length,
        )

        const isComparisonFull = computed(
            () => comparedCardIds.value.length >= 3,
        )

        function isCompared(cardId) {
            return comparedCardIds.value.includes(
                Number(cardId),
            )
        }

        function buildListParams() {
            const params = {}

            const normalizedKeyword =
                keyword.value.trim()

            if (normalizedKeyword) {
                params.keyword = normalizedKeyword
            }

            if (currencyCode.value) {
                params.currencyCode =
                    currencyCode.value
            }

            if (instantUse.value !== null) {
                params.instantUse =
                    instantUse.value
            }

            if (transitCard.value !== null) {
                params.transitCard =
                    transitCard.value
            }

            return params
        }

        async function loadCards() {
            const requestSequence =
                ++listRequestSequence

            // 요청 시작 시점의 검색·필터 조건을 고정한다.
            const requestParams = buildListParams()

            listLoading.value = true
            errorMessage.value = ''

            try {
                const response =
                    await fetchTravelCards(requestParams)

                // 더 최신 목록 요청이 시작됐다면
                // 이전 응답은 화면에 반영하지 않는다.
                if (
                    requestSequence !==
                    listRequestSequence
                ) {
                    return null
                }

                cards.value = response

                return cards.value
            } catch (error) {
                // 가장 최근 목록 요청의 오류만 표시한다.
                if (
                    requestSequence ===
                    listRequestSequence
                ) {
                    cards.value = []
                    errorMessage.value = getErrorMessage(
                        error,
                        '트래블카드 목록을 불러오지 못했습니다.',
                    )
                }

                throw error
            } finally {
                // 이전 요청 완료가 최신 요청의 로딩 상태를
                // 종료하지 않도록 한다.
                if (
                    requestSequence ===
                    listRequestSequence
                ) {
                    listLoading.value = false
                }
            }
        }

        async function loadCardDetail(cardId) {
            const requestSequence =
                ++detailRequestSequence

            detailLoading.value = true
            errorMessage.value = ''
            selectedCard.value = null

            try {
                const response =
                    await fetchTravelCardDetail(cardId)

                // 요청 도중 다른 카드 상세 조회가 시작된 경우
                // 이전 응답은 화면 상태에 반영하지 않는다.
                if (
                    requestSequence !==
                    detailRequestSequence
                ) {
                    return null
                }

                selectedCard.value = response

                return selectedCard.value
            } catch (error) {
                // 가장 최근 요청에서 발생한 오류만 표시한다.
                if (
                    requestSequence ===
                    detailRequestSequence
                ) {
                    errorMessage.value = getErrorMessage(
                        error,
                        '트래블카드 상세 정보를 불러오지 못했습니다.',
                    )
                }

                throw error
            } finally {
                // 이전 요청의 완료가 최신 요청의 로딩 상태를
                // 종료하지 않도록 한다.
                if (
                    requestSequence ===
                    detailRequestSequence
                ) {
                    detailLoading.value = false
                }
            }
        }

        async function loadComparison() {
            const requestSequence =
                ++comparisonRequestSequence

            const requestedCardIds = [
                ...comparedCardIds.value,
            ]

            comparisonLoading.value = true
            errorMessage.value = ''

            if (!requestedCardIds.length) {
                comparisonCards.value = []
                comparisonLoading.value = false

                return comparisonCards.value
            }

            try {
                const response =
                    await fetchTravelCardComparison(
                        requestedCardIds,
                    )

                const isLatestRequest =
                    requestSequence ===
                    comparisonRequestSequence

                const isSelectionUnchanged =
                    areSameCardIds(
                        requestedCardIds,
                        comparedCardIds.value,
                    )

                // 요청 이후 카드 선택이 변경됐거나
                // 더 최신 요청이 시작됐다면 이전 응답을 무시한다.
                if (
                    !isLatestRequest ||
                    !isSelectionUnchanged
                ) {
                    return null
                }

                comparisonCards.value = response

                // 실제 조회된 활성 카드만 선택 상태에 유지한다.
                comparedCardIds.value =
                    response.map(
                        (card) => Number(card.id),
                    )

                return comparisonCards.value
            } catch (error) {
                const isLatestRequest =
                    requestSequence ===
                    comparisonRequestSequence

                const isSelectionUnchanged =
                    areSameCardIds(
                        requestedCardIds,
                        comparedCardIds.value,
                    )

                // 이전 요청의 오류가 현재 화면을 덮지 않게 한다.
                if (
                    isLatestRequest &&
                    isSelectionUnchanged
                ) {
                    comparisonCards.value = []
                    errorMessage.value = getErrorMessage(
                        error,
                        '트래블카드 비교 정보를 불러오지 못했습니다.',
                    )
                }

                throw error
            } finally {
                // 이전 요청 완료가 최신 요청의 로딩을 종료하지 않게 한다.
                if (
                    requestSequence ===
                    comparisonRequestSequence
                ) {
                    comparisonLoading.value = false
                }
            }
        }

        function addComparisonCard(cardId) {
            const normalizedCardId = Number(cardId)

            if (
                !Number.isInteger(normalizedCardId) ||
                normalizedCardId <= 0
            ) {
                return false
            }

            if (isCompared(normalizedCardId)) {
                return true
            }

            if (isComparisonFull.value) {
                return false
            }

            comparedCardIds.value.push(
                normalizedCardId,
            )

            return true
        }

        function removeComparisonCard(cardId) {
            const normalizedCardId = Number(cardId)

            comparedCardIds.value =
                comparedCardIds.value.filter(
                    (selectedCardId) =>
                        selectedCardId !== normalizedCardId,
                )

            comparisonCards.value =
                comparisonCards.value.filter(
                    (card) =>
                        Number(card.id) !== normalizedCardId,
                )
        }

        function toggleComparisonCard(cardId) {
            if (isCompared(cardId)) {
                removeComparisonCard(cardId)
                return true
            }

            return addComparisonCard(cardId)
        }

        function clearComparison() {
            comparedCardIds.value = []
            comparisonCards.value = []
        }

        function resetFilters() {
            keyword.value = ''
            currencyCode.value = ''
            instantUse.value = null
            transitCard.value = null
        }

        function clearError() {
            errorMessage.value = ''
        }

        watch(
            comparedCardIds,
            (cardIds) => {
                try {
                    localStorage.setItem(
                        STORAGE_KEY,
                        JSON.stringify(cardIds),
                    )
                } catch (error) {
                    console.warn(
                        '트래블카드 비교 선택 상태를 저장하지 못했습니다.',
                        error,
                    )
                }
            },
            { deep: true },
        )

        return {
            cards,
            selectedCard,
            comparisonCards,

            keyword,
            currencyCode,
            instantUse,
            transitCard,

            comparedCardIds,
            comparedCardCount,
            isComparisonFull,

            listLoading,
            detailLoading,
            comparisonLoading,
            errorMessage,

            isCompared,
            loadCards,
            loadCardDetail,
            loadComparison,

            addComparisonCard,
            removeComparisonCard,
            toggleComparisonCard,
            clearComparison,

            resetFilters,
            clearError,
        }
    },
)