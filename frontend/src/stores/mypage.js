import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as api from '@/api/notificationApi'
import { now as currentDateTime } from '@/utils/devDate'

export const useMypageStore = defineStore('mypage', () => {
  const notifications = ref([])
  const settings = ref({ 
    allEnabled: true, 
    travelScheduleEnabled: true, 
    exchangeRateEnabled: true, 
    checklistEnabled: true, 
    travelReportEnabled: true 
  })

  const unreadCount = computed(() => notifications.value.filter(item => !item.read).length)

  // API로부터 알림 목록 가져오기
  async function fetchNotifications() {
    try {
      const response = await api.getNotifications()
      notifications.value = response.data.data.map(item => {
        // 알림 유형 매핑 (대문자 -> 소문자)
        const type = item.notificationType ? item.notificationType.toLowerCase() : 'saving';
        
        // 시간 포맷팅 (예: "오늘 14:30" 또는 "08월 14일")
        const date = new Date(item.createdAt);
        const now = currentDateTime();
        let timeStr = '';
        
        if (date.toDateString() === now.toDateString()) {
          timeStr = `오늘 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
        } else {
          timeStr = `${(date.getMonth() + 1).toString().padStart(2, '0')}월 ${date.getDate().toString().padStart(2, '0')}일`;
        }

        return {
          ...item,
          type,
          time: timeStr,
          read: item.isRead !== undefined ? item.isRead : item.read
        };
      }).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    } catch (error) {
      console.error('알림 목록 조회 실패', error)
    }
  }

  // 단건 읽음 처리
  async function markRead(id) {
    const item = notifications.value.find(item => item.id === id)
    if (item && !item.read) {
      // 낙관적 업데이트: 서버 응답 전 즉시 UI 반영
      item.read = true 
      
      try {
        await api.markNotificationRead(id)
      } catch (error) {
        // 실패 시에만 원복
        item.read = false
        console.error('알림 읽음 처리 실패', error)
      }
    }
  }

  // 전체 읽음 처리
  async function markAllRead() {
    // 낙관적 업데이트
    const previousStates = notifications.value.map(item => ({ id: item.id, read: item.read }))
    notifications.value.forEach(item => { item.read = true })
    
    try {
      await api.markAllNotificationsRead()
    } catch (error) {
      // 실패 시 원복
      notifications.value.forEach(item => {
        const prev = previousStates.find(p => p.id === item.id)
        if (prev) item.read = prev.read
      })
      console.error('알림 전체 읽음 처리 실패', error)
    }
  }

  // 알림 설정 가져오기
  async function fetchSettings() {
    try {
      const response = await api.getSettings()
      if (response.data.data) {
        settings.value = response.data.data
      }
    } catch (error) {
      console.error('알림 설정 조회 실패', error)
    }
  }

  // 알림 설정 변경 순서 보장을 위한 큐
  let settingsQueue = Promise.resolve();

  const notificationTypeKeys = [
    'travelScheduleEnabled',
    'exchangeRateEnabled',
    'checklistEnabled',
    'travelReportEnabled',
  ]

  // 알림 설정 토글
  async function toggleSetting(key) {
    // 이전 상태 전체를 스냅샷으로 남겨서, 전체 알림 토글처럼 여러 필드가
    // 한 번에 바뀌는 경우에도 실패 시 전부 원복할 수 있게 한다.
    const previousSettings = { ...settings.value }
    const nextValue = !settings.value[key]
    settings.value[key] = nextValue

    // 전체 알림을 껐다 켰다 하면 개별 알림 항목도 같이 맞춰서, 화면과 실제
    // 발송 여부가 항상 일치하도록 한다.
    if (key === 'allEnabled') {
      notificationTypeKeys.forEach((typeKey) => {
        settings.value[typeKey] = nextValue
      })
    }

    // 큐를 사용하여 요청 순서 보장
    settingsQueue = settingsQueue.then(async () => {
      try {
        await api.updateSettings(settings.value)
      } catch (error) {
        settings.value = previousSettings // 실패 시 전체 원복
        console.error('알림 설정 수정 실패', error)
      }
    });
    return settingsQueue;
  }

  return { notifications, settings, unreadCount, fetchNotifications, markRead, markAllRead, fetchSettings, toggleSetting }
})
