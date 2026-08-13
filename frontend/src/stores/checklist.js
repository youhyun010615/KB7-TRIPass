import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchChecklistSummary,
  fetchChecklists,
  toggleChecklistItem,
  createChecklistItem,
  deleteChecklistItem
} from '@/api/checklist'

export const useChecklistStore = defineStore('checklist', () => {
  // ----------------------------------------------------
  // 1. State
  // ----------------------------------------------------
  // 여행 전체 요약 통계 (대시보드 메인 카드용)
  const summary = ref({
    totalCompletedCount: 0,
    totalItemCount: 0,
    prepCompletedCount: 0,
    prepItemCount: 0,
    returnCompletedCount: 0,
    returnItemCount: 0
  })

  // 단계별 상세 목록 (이월 항목 / 현재 탭 항목 분리)
  const carriedOverChecklists = ref([])
  const currentChecklists = ref([])

  // 로딩 상태
  const loading = ref(false)

  // ----------------------------------------------------
  // 2. Actions
  // ----------------------------------------------------

 // [GET] 요약 통계 조회
  async function loadSummary(tripId) {
    try {
      const data = await fetchChecklistSummary(tripId)
      if (data) summary.value = data
    } catch (error) {
      console.error('요약 조회 실패:', error)
    }
  }

  // [GET] 상세 목록 조회
  async function loadChecklists(tripId, type, ddayStage) {
    loading.value = true
    try {
      const data = await fetchChecklists(tripId, type, ddayStage)
      if (data) {
        carriedOverChecklists.value = data.carriedOverChecklists || []
        currentChecklists.value = data.currentChecklists || []
      }
    } catch (error) {
      console.error('목록 조회 실패:', error)
    } finally {
      loading.value = false
    }
  }

  // [PATCH] 완료 상태 토글 (Optimistic UI Update + Fallback)
  async function toggleItem(tripId, itemId, currentStatus) {
    const newStatus = !currentStatus

    // UI 즉시 반영 (낙관적 업데이트)
    const findAndToggle = (list) => {
      const item = list.find((target) => target.id === itemId)
      if (item) {
        item.isCompleted = newStatus
        return true
      }
      return false
    }

    const updated = findAndToggle(currentChecklists.value) || findAndToggle(carriedOverChecklists.value)

    try {
      await toggleChecklistItem(tripId, itemId, newStatus)
      await loadSummary(tripId) // 전체 완료 요약 수치 갱신
    } catch (error) {
      console.error('토글 실패:', error)
      // 실패 시 롤백
      if (updated) {
        findAndToggle(currentChecklists.value)
        findAndToggle(carriedOverChecklists.value)
      }
      alert('상태 변경에 실패했습니다.')
    }
  }

  // [POST] 커스텀 항목 추가
  async function addItem(tripId, itemData) {
    try {
      const result = await createChecklistItem(tripId, itemData)
      if (result && result.id) {
        // 목록 및 요약 데이터 최신화
        await loadChecklists(tripId, itemData.checklistType, itemData.ddayStage)
        await loadSummary(tripId)
        return true
      }
    } catch (error) {
      console.error('항목 추가 실패:', error)
      alert('항목 추가 중 오류가 발생했습니다.')
      return false
    }
  }

  // [DELETE] 항목 삭제 (기본 템플릿 항목인 경우 백엔드 400 에러 처리)
  async function removeItem(tripId, itemId, type, ddayStage) {
    try {
      await deleteChecklistItem(tripId, itemId)
      await loadChecklists(tripId, type, ddayStage)
      await loadSummary(tripId)
      return true
    } catch (error) {
      console.error('항목 삭제 실패:', error)
      const errorMsg = error.response?.data?.message || '기본 제공 템플릿 항목은 삭제할 수 없습니다.'
      alert(errorMsg)
      return false
    }
  }

  return {
    summary,
    carriedOverChecklists,
    currentChecklists,
    loading,
    loadSummary,
    loadChecklists,
    toggleItem,
    addItem,
    removeItem
  }
})