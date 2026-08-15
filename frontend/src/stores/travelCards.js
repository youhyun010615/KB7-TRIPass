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
            listLoading.value = true
            errorMessage.value = ''

            try {
                cards.value = await fetchTravelCards(
                    buildListParams(),
                )

                return cards.value
            } catch (error) {
                cards.value = []
                errorMessage.value = getErrorMessage(
                    error,
                    '트래블카드 목록을 불러오지 못했습니다.',
                )

                throw error
            } finally {
                listLoading.value = false
            }
        }

        async function loadCardDetail(cardId) {
            detailLoading.value = true
            errorMessage.value = ''
            selectedCard.value = null

            try {
                selectedCard.value =
                    await fetchTravelCardDetail(cardId)

                return selectedCard.value
            } catch (error) {
                errorMessage.value = getErrorMessage(
                    error,
                    '트래블카드 상세 정보를 불러오지 못했습니다.',
                )

                throw error
            } finally {
                detailLoading.value = false
            }
        }

        async function loadComparison() {
            comparisonLoading.value = true
            errorMessage.value = ''

            if (!comparedCardIds.value.length) {
                comparisonCards.value = []
                comparisonLoading.value = false

                return comparisonCards.value
            }

            try {
                comparisonCards.value =
                    await fetchTravelCardComparison(
                        comparedCardIds.value,
                    )

                // 실제 조회된 활성 카드만 비교 선택 상태에 유지한다.
                comparedCardIds.value =
                    comparisonCards.value.map(
                        (card) => Number(card.id),
                    )

                return comparisonCards.value
            } catch (error) {
                comparisonCards.value = []
                errorMessage.value = getErrorMessage(
                    error,
                    '트래블카드 비교 정보를 불러오지 못했습니다.',
                )

                throw error
            } finally {
                comparisonLoading.value = false
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