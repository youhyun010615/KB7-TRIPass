<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const authStore = useAuthStore()
const originalPhone = authStore.user?.phoneNumber ?? '010-1234-5678'
const name = ref(authStore.user?.name ?? '권유현')
const phone = ref(originalPhone)
const verificationCode = ref('')
const verificationStatus = ref('idle')
const verificationMessage = ref('')

const normalizedPhone = computed(() => phone.value.replace(/[^0-9]/g, ''))
const isPhoneValid = computed(() => /^01[016789]\d{7,8}$/.test(normalizedPhone.value))
const isPhoneChanged = computed(() => normalizedPhone.value !== originalPhone.replace(/[^0-9]/g, ''))
const canSave = computed(() => name.value.trim() && isPhoneValid.value && (!isPhoneChanged.value || verificationStatus.value === 'verified'))

watch(phone, () => {
  verificationCode.value = ''
  verificationStatus.value = 'idle'
  verificationMessage.value = isPhoneChanged.value ? '변경할 번호는 인증이 필요해요.' : ''
})

function sendVerificationCode() {
  if (!isPhoneValid.value) return
  verificationStatus.value = 'sent'
  verificationMessage.value = '인증번호를 발송했어요. 시연용 인증번호는 123456이에요.'
}

function verifyPhone() {
  if (verificationCode.value === '123456') {
    verificationStatus.value = 'verified'
    verificationMessage.value = '휴대폰 번호 인증이 완료됐어요.'
  } else {
    verificationStatus.value = 'failed'
    verificationMessage.value = '인증번호가 일치하지 않아요.'
  }
}

function saveProfile() {
  if (!canSave.value) return
  authStore.updateUser({ name: name.value.trim(), phoneNumber: phone.value })
  router.replace('/mypage/profile')
}
</script>

<template>
  <div class="min-h-screen pb-20 flex flex-col" style="background: #F7F4EE">
    <div class="flex items-center justify-between px-5 pt-14 pb-4">
      <button type="button" class="p-1" aria-label="뒤로 가기" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M15 18L9 12L15 6" stroke="#1A1A1A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
      <h1 class="text-base font-bold text-gray-900">회원정보 수정</h1>
      <div class="w-8" />
    </div>

    <div class="px-4 mt-2 flex flex-col gap-3">
      <div>
        <label for="profile-name" class="text-xs text-gray-400 mb-1.5 block">이름</label>
        <div class="bg-white rounded-2xl px-5 py-4"><input id="profile-name" v-model="name" class="w-full text-sm text-gray-900 bg-transparent outline-none" placeholder="이름 입력"></div>
      </div>

      <div>
        <label for="profile-phone" class="text-xs text-gray-400 mb-1.5 block">휴대폰 번호</label>
        <div class="flex gap-2">
          <div class="min-w-0 flex-1 bg-white rounded-2xl px-5 py-4"><input id="profile-phone" v-model="phone" type="tel" inputmode="numeric" class="w-full text-sm text-gray-900 bg-transparent outline-none" placeholder="010-1234-5678"></div>
          <button type="button" :disabled="!isPhoneChanged || !isPhoneValid" class="px-3 rounded-2xl border text-xs font-semibold whitespace-nowrap disabled:text-gray-400 disabled:border-gray-200" style="color:#3B5BDB;border-color:#3B5BDB" @click="sendVerificationCode">{{ verificationStatus === 'sent' || verificationStatus === 'failed' ? '재요청' : '번호 인증' }}</button>
        </div>
      </div>

      <div v-if="verificationStatus === 'sent' || verificationStatus === 'failed'">
        <label for="verification-code" class="text-xs text-gray-400 mb-1.5 block">인증번호</label>
        <div class="flex gap-2">
          <div class="min-w-0 flex-1 bg-white rounded-2xl px-5 py-4"><input id="verification-code" v-model="verificationCode" inputmode="numeric" maxlength="6" class="w-full text-sm text-gray-900 bg-transparent outline-none" placeholder="6자리 입력"></div>
          <button type="button" :disabled="verificationCode.length !== 6" class="px-4 rounded-2xl text-white text-xs font-semibold disabled:opacity-40" style="background:#3B5BDB" @click="verifyPhone">확인</button>
        </div>
      </div>
      <p v-if="verificationMessage" class="text-xs" :class="verificationStatus === 'failed' ? 'text-red-500' : verificationStatus === 'verified' ? 'text-blue-600' : 'text-gray-500'">{{ verificationMessage }}</p>
    </div>

    <div class="px-4 mt-auto pt-6 flex flex-col gap-3">
      <button type="button" :disabled="!canSave" class="w-full h-14 rounded-2xl text-white font-bold text-base disabled:opacity-40" style="background:#1A337A" @click="saveProfile">수정 내용 저장</button>
      <button type="button" class="w-full h-14 rounded-2xl font-bold text-sm border" style="color:#1A337A;border-color:#1A337A;background:transparent" @click="router.push('/mypage/password')">비밀번호 변경</button>
    </div>

    <BottomNav />
  </div>
</template>
