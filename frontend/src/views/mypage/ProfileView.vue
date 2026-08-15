<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  getMyProfile,
  updateMyProfile,
} from '@/api/mypage'
import BottomNav from '@/components/common/BottomNav.vue'

const router = useRouter()
const authStore = useAuthStore()

const profile = ref(null)
const draftName = ref('')

const loading = ref(true)
const saving = ref(false)
const editingName = ref(false)
const errorMessage = ref('')

const isLocal = computed(
    () => profile.value?.loginProvider === 'LOCAL',
)

const isSocial = computed(
    () => ['KAKAO', 'GOOGLE'].includes(
        profile.value?.loginProvider,
    ),
)

const providerName = computed(() => {
  const providers = {
    LOCAL: 'TRIPASS',
    KAKAO: '카카오',
    GOOGLE: '구글',
  }

  return providers[profile.value?.loginProvider]
      ?? profile.value?.loginProvider
      ?? ''
})

const memberLabel = computed(() =>
    `${providerName.value} MEMBER`,
)

const profileInitial = computed(() =>
    profile.value?.name?.trim()?.charAt(0) || 'T',
)

const profileRows = computed(() => {
  if (!profile.value) {
    return []
  }

  const rows = [
    {
      label: '로그인 방식',
      value: isLocal.value
          ? '일반 로그인'
          : `${providerName.value} 로그인`,
    },
    {
      label: isLocal.value
          ? '로그인 아이디'
          : '로그인 이메일',
      value: profile.value.loginId,
    },
  ]

  // 소셜 회원은 휴대전화번호가 없으면 항목 자체를 표시하지 않는다.
  if (profile.value.phoneNumber) {
    rows.push({
      label: '휴대폰 번호',
      value: formatPhoneNumber(
          profile.value.phoneNumber,
      ),
    })
  }

  return rows
})

const canSaveName = computed(() => {
  const normalizedName = draftName.value.trim()

  return normalizedName.length > 0
      && normalizedName.length <= 100
      && normalizedName !== profile.value?.name
      && !saving.value
})

const joinedDate = computed(() =>
    formatJoinedDate(profile.value?.createdAt),
)

onMounted(() => {
  loadProfile()
})

async function loadProfile() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getMyProfile()
    profile.value = response.data.data
    draftName.value = profile.value.name
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message
        ?? '회원정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function startNameEdit() {
  draftName.value = profile.value?.name ?? ''
  errorMessage.value = ''
  editingName.value = true
}

function cancelNameEdit() {
  draftName.value = profile.value?.name ?? ''
  errorMessage.value = ''
  editingName.value = false
}

async function saveName() {
  const normalizedName = draftName.value.trim()

  errorMessage.value = ''

  if (!normalizedName) {
    errorMessage.value = '이름을 입력해 주세요.'
    return
  }

  if (normalizedName.length > 100) {
    errorMessage.value =
        '이름은 100자 이하로 입력해 주세요.'
    return
  }

  if (
      normalizedName === profile.value?.name
      || saving.value
  ) {
    return
  }

  saving.value = true

  try {
    const response = await updateMyProfile({
      name: normalizedName,
    })

    profile.value = response.data.data
    draftName.value = profile.value.name

    // 헤더와 다른 화면에서도 변경된 이름을 사용할 수 있도록 동기화
    authStore.updateUser({
      name: profile.value.name,
    })

    editingName.value = false
  } catch (error) {
    errorMessage.value =
        error.response?.data?.message
        ?? '회원정보를 수정하지 못했습니다.'
  } finally {
    saving.value = false
  }
}

function formatPhoneNumber(value) {
  const digits = String(value ?? '')
      .replace(/\D/g, '')

  if (digits.length === 11) {
    return digits.replace(
        /(\d{3})(\d{4})(\d{4})/,
        '$1-$2-$3',
    )
  }

  if (digits.length === 10) {
    return digits.replace(
        /(\d{3})(\d{3})(\d{4})/,
        '$1-$2-$3',
    )
  }

  return value
}

function formatJoinedDate(value) {
  if (!value) {
    return '-'
  }

  return value
      .slice(0, 10)
      .replaceAll('-', '.')
}
</script>

<template>
  <div
      class="flex min-h-screen flex-col pb-24"
      style="background: #F4F6FB"
  >
    <!-- 헤더 -->
    <header
        class="flex items-center px-5 pb-4 pt-14"
    >
      <button
          type="button"
          class="flex h-8 w-8 items-center justify-center"
          aria-label="뒤로 가기"
          @click="router.back()"
      >
        <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
        >
          <path
              d="M15 18L9 12L15 6"
              stroke="#172033"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
          />
        </svg>
      </button>

      <h1
          class="flex-1 pr-8 text-center text-lg font-bold text-[#172033]"
      >
        회원정보
      </h1>
    </header>

    <!-- 최초 조회 로딩 -->
    <div
        v-if="loading"
        class="flex flex-1 items-center justify-center px-4"
    >
      <p class="text-sm text-gray-400">
        회원정보를 불러오는 중입니다.
      </p>
    </div>

    <!-- 최초 조회 실패 -->
    <div
        v-else-if="errorMessage && !profile"
        class="flex flex-1 flex-col items-center justify-center px-6"
    >
      <p
          class="text-center text-sm text-red-500"
          role="alert"
      >
        {{ errorMessage }}
      </p>

      <button
          type="button"
          class="mt-4 rounded-xl bg-[#173E8F] px-5 py-2.5 text-sm font-semibold text-white"
          @click="loadProfile"
      >
        다시 시도
      </button>
    </div>

    <template v-else-if="profile">
      <!-- 회원 요약 카드 -->
      <section
          class="mx-4 rounded-3xl bg-[#173E8F] px-5 py-5 text-white shadow-lg"
      >
        <div class="flex items-center gap-4">
          <div
              class="flex h-14 w-14 flex-shrink-0 items-center justify-center rounded-full border border-[#F1D47B] bg-white/10 text-xl font-bold"
          >
            {{ profileInitial }}
          </div>

          <div class="min-w-0 flex-1">
            <p class="truncate text-xl font-bold">
              {{ profile.name }}
            </p>

            <p class="mt-1 truncate text-xs text-blue-100">
              {{ profile.loginId }}
            </p>
          </div>
        </div>

        <div
            class="mt-5 grid grid-cols-2 gap-4 border-t border-white/20 pt-4"
        >
          <div>
            <p
                class="text-[10px] font-medium uppercase tracking-wide text-blue-200"
            >
              로그인 방식
            </p>

            <p class="mt-1 text-sm font-semibold">
              {{
                isLocal
                    ? '일반 로그인'
                    : `${providerName} 로그인`
              }}
            </p>
          </div>

          <div>
            <p
                class="text-[10px] font-medium uppercase tracking-wide text-blue-200"
            >
              가입일
            </p>

            <p class="mt-1 text-sm font-semibold">
              {{ joinedDate }}
            </p>
          </div>
        </div>
      </section>

      <!-- 기본 정보 -->
      <section class="mx-4 mt-6">
        <h2
            class="mb-3 px-1 text-base font-bold text-[#566176]"
        >
          기본 정보
        </h2>

        <div
            class="overflow-hidden rounded-3xl bg-white px-5 shadow-sm"
        >
          <!-- 이름 -->
          <div
              class="border-b border-[#E7EBF2] py-4"
          >
            <div
                class="flex items-center justify-between gap-4"
            >
              <p
                  class="w-20 flex-shrink-0 text-sm text-[#9AA7BD]"
              >
                이름
              </p>

              <div class="min-w-0 flex-1">
                <input
                    v-if="editingName"
                    v-model="draftName"
                    type="text"
                    maxlength="100"
                    class="w-full rounded-xl border border-[#C9D5EA] px-3 py-2 text-sm font-semibold text-[#172033] outline-none focus:border-[#2F6BFF]"
                    aria-label="이름"
                    :disabled="saving"
                    @keyup.enter="saveName"
                />

                <p
                    v-else
                    class="truncate text-sm font-semibold text-[#172033]"
                >
                  {{ profile.name }}
                </p>
              </div>

              <button
                  v-if="!editingName"
                  type="button"
                  class="flex-shrink-0 text-xs font-semibold text-[#2F6BFF]"
                  @click="startNameEdit"
              >
                수정
              </button>
            </div>

            <div
                v-if="editingName"
                class="mt-3 flex justify-end gap-2"
            >
              <button
                  type="button"
                  class="rounded-lg px-3 py-1.5 text-xs text-gray-500"
                  :disabled="saving"
                  @click="cancelNameEdit"
              >
                취소
              </button>

              <button
                  type="button"
                  class="rounded-lg bg-[#173E8F] px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-40"
                  :disabled="!canSaveName"
                  @click="saveName"
              >
                {{ saving ? '저장 중' : '저장' }}
              </button>
            </div>
          </div>

          <!-- 로그인 정보 -->
          <div
              v-for="(row, index) in profileRows"
              :key="row.label"
              class="flex items-center gap-4 py-4"
              :class="{
              'border-b border-[#E7EBF2]':
                index < profileRows.length - 1
            }"
          >
            <p
                class="w-20 flex-shrink-0 text-sm text-[#9AA7BD]"
            >
              {{ row.label }}
            </p>

            <p
                class="min-w-0 flex-1 break-all text-sm font-semibold text-[#172033]"
            >
              {{ row.value }}
            </p>
          </div>
        </div>

        <!-- 이름 수정 오류 -->
        <p
            v-if="errorMessage"
            class="mt-3 px-2 text-sm text-red-500"
            role="alert"
        >
          {{ errorMessage }}
        </p>
      </section>

      <!-- LOCAL 회원 보안 설정 -->
      <section
          v-if="isLocal"
          class="mx-4 mt-6"
      >
        <h2
            class="mb-3 px-1 text-base font-bold text-[#566176]"
        >
          보안
        </h2>

        <button
            type="button"
            class="flex w-full items-center rounded-3xl bg-white px-5 py-4 text-left shadow-sm"
            @click="router.push('/mypage/password')"
        >
          <span
              class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-[#EEF3FF]"
          >
            <svg
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
            >
              <rect
                  x="5"
                  y="10"
                  width="14"
                  height="10"
                  rx="2"
                  stroke="#2F6BFF"
                  stroke-width="1.8"
              />
              <path
                  d="M8 10V7.5C8 5.57 9.57 4 11.5 4H12.5C14.43 4 16 5.57 16 7.5V10"
                  stroke="#2F6BFF"
                  stroke-width="1.8"
                  stroke-linecap="round"
              />
            </svg>
          </span>

          <span class="ml-3 min-w-0 flex-1">
            <strong
                class="block text-sm font-bold text-[#172033]"
            >
              비밀번호 변경
            </strong>

            <small
                class="mt-1 block text-xs text-[#9AA7BD]"
            >
              안전한 계정 관리를 위해 비밀번호를 변경해요.
            </small>
          </span>

          <span
              class="ml-3 text-xl text-[#B4BED0]"
              aria-hidden="true"
          >
            ›
          </span>
        </button>
      </section>
    </template>

    <BottomNav />
  </div>
</template>
