<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const authStore = useAuthStore()
const editingName = ref(false)
const draftName = ref(authStore.user?.name ?? '권유현')

const user = computed(() => ({
  name: authStore.user?.name ?? '권유현',
  loginId: authStore.user?.loginId ?? authStore.user?.id ?? 'tripass',
  phoneNumber: authStore.user?.phoneNumber ?? '010-1234-5678',
  email: authStore.user?.email ?? 'youhyun@email.com',
  loginProvider: authStore.user?.loginProvider ?? 'LOCAL',
}))
const isSocial = computed(() => user.value.loginProvider !== 'LOCAL')
const providerName = computed(() => ({ KAKAO: '카카오', GOOGLE: '구글', NAVER: '네이버' }[user.value.loginProvider] ?? user.value.loginProvider))
const profileRows = computed(() => isSocial.value
  ? [
      { label: '로그인 방식', value: `${providerName.value} 로그인` },
      { label: '로그인 이메일', value: user.value.email },
      { label: '휴대폰 번호', value: user.value.phoneNumber || '등록된 번호가 없어요.' },
    ]
  : [
      { label: '로그인 아이디', value: user.value.loginId },
      { label: '휴대폰 번호', value: user.value.phoneNumber },
    ])

function startNameEdit() {
  draftName.value = user.value.name
  editingName.value = true
}

function cancelNameEdit() {
  draftName.value = user.value.name
  editingName.value = false
}

function saveName() {
  const nextName = draftName.value.trim()
  if (!nextName) return
  authStore.updateUser({ name: nextName })
  editingName.value = false
}
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col" style="background: #F7F4EE">
    <div class="flex items-center justify-between px-5 pt-14 pb-4">
      <button type="button" class="p-1" aria-label="뒤로 가기" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M15 18L9 12L15 6" stroke="#1A1A1A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
      <h1 class="text-base font-bold text-gray-900">회원정보</h1>
      <button type="button" class="text-xs font-medium" style="color: #3B5BDB" @click="router.push('/mypage/password')">비밀번호 수정</button>
    </div>

    <div class="mx-4 bg-white rounded-2xl px-5 py-4 flex items-center gap-4">
      <div class="w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold flex-shrink-0" style="background: #EEF2FF; color: #3B5BDB">{{ user.name[0] }}</div>
      <div>
        <p class="font-bold text-gray-900 text-base">{{ user.name }}</p>
        <p class="text-xs text-gray-400 mt-0.5">{{ isSocial ? `${providerName} MEMBER` : 'TRIPASS MEMBER' }}</p>
      </div>
    </div>

    <div class="px-4 mt-5 flex flex-col gap-3">
      <div>
        <div class="mb-1.5 flex items-center justify-between">
          <p class="text-xs text-gray-400">이름</p>
          <button v-if="!editingName" type="button" class="text-xs font-semibold" style="color:#3B5BDB" @click="startNameEdit">수정</button>
        </div>
        <div class="bg-white rounded-2xl px-5 py-3.5 flex items-center gap-2">
          <input v-if="editingName" v-model="draftName" class="min-w-0 flex-1 text-sm text-gray-900 bg-transparent outline-none" maxlength="20" aria-label="이름" @keyup.enter="saveName">
          <p v-else class="flex-1 text-sm text-gray-900">{{ user.name }}</p>
          <template v-if="editingName">
            <button type="button" class="text-xs text-gray-400" @click="cancelNameEdit">취소</button>
            <button type="button" :disabled="!draftName.trim()" class="text-xs font-semibold disabled:opacity-40" style="color:#3B5BDB" @click="saveName">저장</button>
          </template>
        </div>
      </div>
      <div v-for="row in profileRows" :key="row.label">
        <p class="text-xs text-gray-400 mb-1.5">{{ row.label }}</p>
        <div class="bg-white rounded-2xl px-5 py-4"><p class="text-sm text-gray-900">{{ row.value }}</p></div>
      </div>
    </div>

    <BottomNav />
  </div>
</template>
