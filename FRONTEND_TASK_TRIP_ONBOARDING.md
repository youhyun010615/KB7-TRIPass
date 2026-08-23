# 프론트 작업 목록 — 여행/계좌 온보딩 + 여행 저축 집계 연동

> 백엔드(DB 스키마 + API)는 구현 완료. 아래는 프론트 작업자가 붙여야 하는 화면/로직 목록입니다.
> API 계약은 실제 구현된 코드 기준이며, 변경 시 이 문서도 함께 업데이트해 주세요.

---

## 0. 배경 정책 (구현 전 필독)

- 온보딩(여행 등록 → 계좌 등록, 둘 다 skip 가능)은 **가입 직후 딱 1회만** 노출됩니다. skip 해도 이후 로그인마다 다시 뜨지 않습니다.
- "여행 저축 집계"는 **여행 등록 + 계좌 등록이 둘 다 충족된 시점**부터 시작됩니다. 어느 쪽을 먼저 하든, 나중 조건이 채워지는 순간 집계가 시작됩니다.
- 집계 시작 시점에 월렛에 잔액이 있으면 "이 금액을 여행 저축에 반영할까요?" 프롬프트가 1회 뜹니다.
  - 반영 선택 → 잔액 그대로 여행 저축액으로 인정 (돈 이동 없음)
  - 반영 거부 → 잔액 전액이 연동 계좌로 송금되고 월렛은 0원부터 시작
- 집계 시작 이후 발생하는 모든 월렛 입출금은 건별 확인 없이 자동으로 여행 저축 기록에 잡힙니다.
- 미션은 여행 등록 + 계좌 등록 + (해당 월) 분석 리포트 확인까지 모두 되어야 생성할 수 있습니다.

---

## 1. API 계약 변경/추가 사항

### 1-1. `GET /api/v1/trips/current-lifecycle` (기존 API, 응답 필드 추가됨)

홈탭 진입 시 항상 호출하는 기존 API입니다. 아래 필드가 새로 추가되었습니다.

```jsonc
{
  "tripId": 12,
  "tripName": "스위스 여행",
  "lifecycle": "PREPARING",       // NONE / PREPARING / TRAVELING / REVIEW / ARCHIVED (기존)
  "status": "PLANNING",
  "startDate": "2026-09-01",
  "endDate": "2026-09-10",
  "travelModeAvailable": false,
  "missionAvailable": true,
  "startReportAvailable": false,
  "startReportAcknowledged": false,
  "endingReviewRequired": false,

  // ▼ 신규 필드
  "hasTrip": true,                       // 여행 등록 여부
  "hasLinkedAccount": true,               // 계좌(Codef) 연동 여부
  "savingsTrackingStarted": true,         // 여행 저축 집계 시작 여부 (hasTrip && hasLinkedAccount 둘 다 충족된 시점부터 true)
  "needsWalletReflectPrompt": true,       // true면 잔액 반영 프롬프트를 띄워야 함
  "walletReflectAmount": 350000,          // 프롬프트에 표시할 금액 (needsWalletReflectPrompt=false면 null)
  "onboardingPending": false              // true면 온보딩 시퀀스를 아직 한 번도 안 보여준 계정
}
```

- 여행이 아예 없는 경우 `lifecycle: "NONE"`이며 `hasTrip: false`, `savingsTrackingStarted: false`로 내려옵니다.
- **이 응답 하나로 4가지 상태(신규/계좌만/여행만/둘다)를 전부 판단할 수 있습니다.** `hasTrip`과 `hasLinkedAccount` 조합으로 분기하세요.

### 1-2. `POST /api/v1/trips/onboarding/ack` (신규)

온보딩 시퀀스를 1회 봤다는 것을 서버에 기록합니다. 온보딩 플로우 진입 시(첫 화면 노출 시점, skip이든 진행이든 상관없이) 1번 호출하면 됩니다. Body 없음.

```
POST /api/v1/trips/onboarding/ack
→ 200 OK
```

호출 후 `onboardingPending`은 영구히 `false`가 됩니다(재로그인해도 다시 안 뜸).

### 1-3. `POST /api/v1/trips/{id}/wallet-reflect` (신규)

잔액 반영 프롬프트에 대한 사용자 선택을 서버에 전달합니다.

```
POST /api/v1/trips/12/wallet-reflect
Content-Type: application/json

{ "reflect": true }   // 반영함
{ "reflect": false }  // 반영 안 함 → 서버가 자동으로 연동 계좌에 송금 처리
```

- 이미 처리된 경우 `409 WALLET_REFLECT_ALREADY_RESOLVED` 반환 (버튼 중복 클릭 방어는 프론트에서도 해주세요)
- 아직 집계가 시작되지 않은 트립에 호출하면 `400 WALLET_REFLECT_NOT_READY`

### 1-4. 미션 생성/카테고리 선택 API — 에러 코드 추가

`POST /api/v1/saving/missions/{targetYearMonth}` 및 미션 카테고리 선택 API가 아래 에러코드를 새로 던질 수 있습니다. 기존에는 여행이 없으면 그냥 통과되던 구멍이었는데 이제 막힙니다.

| errorCode | 상황 | 화면에 보여줄 문구 |
|---|---|---|
| `TRIP_REQUIRED_FOR_MISSION` | 여행 미등록 | "여행 계획을 등록해야 미션을 진행할 수 있어요" |
| `ACCOUNT_REQUIRED_FOR_MISSION` | 여행은 있지만 계좌 미연동 | "계좌를 등록해야 미션을 진행할 수 있어요" |
| `MISSION_REPORT_NOT_VIEWED` | 이번 달 분석 리포트 미확인 | 리포트 화면으로 유도 |
| `MISSION_NOT_ALLOWED` | 여행 중/종료 상태 (기존) | 기존 문구 유지 |

**미션 탭 진입 시 이 에러를 기다리지 말고, `current-lifecycle`의 `hasTrip`/`hasLinkedAccount`로 선제적으로 안내 문구를 보여주세요** (아래 3장 참고). 에러코드는 방어용입니다.

---

## 2. 온보딩 플로우 (신규 화면/라우팅)

1. 회원가입 성공 직후, `current-lifecycle` 조회 결과 `onboardingPending: true`면 온보딩 시퀀스로 진입.
2. **1단계: 여행 등록 화면** (기존 여행 등록 폼 재사용 가능) — 상단/하단에 "다음에 할게요" 스킵 버튼.
3. 스킵 또는 등록 완료 → **2단계: 계좌 등록 화면** (기존 `FinancialProfileView.vue` 재사용 가능) — 마찬가지로 스킵 가능.
4. 스킵 또는 완료 → `POST /trips/onboarding/ack` 호출 → 홈으로 이동.
5. **이 시퀀스는 온보딩 진입 시점(1단계 진입 즉시)에 `ack`를 한 번 호출**해서, 중간에 앱을 끄고 나가도 다시 안 뜨게 하세요. (질문 있으면 문의: "1단계 진입 시 vs 시퀀스 완주 시" 중 진입 시로 확정되었습니다.)
6. 이후 로그인마다 `onboardingPending`은 `false`이므로 이 플로우는 다시 노출되지 않습니다. 여행/계좌 등록은 이후 마이페이지·홈 CTA를 통해서만 가능합니다(기존 경로 유지).

---

## 3. 탭별 화면 분기 (상태 매트릭스)

`current-lifecycle`의 `hasTrip` × `hasLinkedAccount` 조합으로 4가지 상태를 판단하세요.

| | `hasLinkedAccount: false` | `hasLinkedAccount: true` |
|---|---|---|
| **`hasTrip: false`** | ① 신규(둘다 스킵) | ② 계좌만 등록 |
| **`hasTrip: true`** | ③ 여행만 등록 | ④ 둘 다 등록 (`savingsTrackingStarted: true`) |

### 홈탭

| 상태 | 표시 내용 |
|---|---|
| ①② (여행 없음) | "여행 계획 등록하기" CTA만. 그 아래 아무것도 안 뜸 |
| ③④ (여행 있음) | 여행 계획 카드탭 + 환율탭. **④만** 월별 저축 금액 + 미션 카드까지 추가 |

미션 카드(④ 전용) 로직:
- 전달 분석 리포트(`report_status`)가 `PENDING`이면 "리포트 보기" 카드 노출, 미션 카드는 숨김
- 리포트를 1회 확인(`PATCH /saving/analyses/{yearMonth}/view`)하면 그 이후부터 "미션 등록하기" 노출
- **"홈탭에 뜨는 리포트는 1회만"** — 리포트를 본 뒤에는 홈탭에서 리포트 카드가 다시 안 뜸(마이페이지나 저축탭에서는 재열람 가능해도 무방)
- 이 PENDING/VIEWED는 `target_year_month`(분석월)마다 독립적이라, 다음 달이 되면 새 리포트에 대해 다시 1회 확인이 필요합니다.

### 저축 미션 탭

모든 상태에서 미션 탭 자체는 진입 가능하되, 안내 문구로 분기:

| 상태 | 문구 |
|---|---|
| ①② (여행 없음) | "여행 계획과 계좌 등록을 해야 미션을 진행할 수 있어요" (①) / "여행 계획을 등록해야 미션을 진행할 수 있어요" (②) |
| ③ (여행만) | "계좌를 등록해야 미션을 진행할 수 있어요" |
| ④ (둘 다) | 정상 화면: 미션 저축액 / 이번달 자금 체크 / 수행중인 미션 |

탭 자체에 "저축 미션이 뭐하는 탭인지" 짧은 설명 문구를 상단에 고정으로 추가해 주세요(①②③ 상태 공통, 신규 카피 필요 — 디자인/기획 확인).

### 월렛 탭

모든 상태 공통:
- 월렛 잔액 탭은 항상 정상 노출 (여행 등록 여부와 무관하게 충전/저축 가능)
- 트래블카드 미연동 시 "트래블카드를 연동해주세요" 노출 (기존 로직 유지)
- **월별 합산 금액은 `savingsTrackingStarted: true`(④)일 때만 노출**, 그 외엔 숨김

### 환율 탭

상태 무관, 항상 전체 국가 조회 (변경 없음 — 이미 이렇게 동작 중)

### 마이페이지

변경 없음

---

## 4. 잔액 반영 프롬프트 UI

`current-lifecycle` 응답에서 `needsWalletReflectPrompt: true`가 오면(주로 홈탭 진입 시), 모달/바텀시트로 노출:

- 문구 예시: "여행 등록 전 월렛에 모아둔 **{walletReflectAmount}원**이 있어요. 이 금액을 여행 저축에 포함할까요?"
- 버튼 2개: "포함할게요" / "포함 안 할게요"
- 포함 → `POST /trips/{tripId}/wallet-reflect { reflect: true }`
- 포함 안 함 → 같은 API에 `{ reflect: false }` — **이 경우 월렛 잔액이 실제로 계좌로 이체되고 월렛이 0원이 됩니다.** 이체된다는 사실을 버튼 문구나 confirm에서 한 번 더 알려주는 걸 권장합니다(예: "선택하신 금액은 연동 계좌로 송금돼요").
- 응답 성공 시 `current-lifecycle`을 다시 불러와서 `needsWalletReflectPrompt: false`로 갱신 확인 후 모달 닫기.
- 이미 처리됨(409) 응답 시 조용히 모달만 닫기(다른 기기/탭에서 이미 처리된 경우 대비).

---

## 5. 참고 — 이번 세션에서 같이 고친 프론트 이슈 (온보딩 작업과 무관하지만 관련 파일 겹침)

- `HomeView.vue` / `stores/travelMode.js`: 탭 이동 시 자꾸 독일(기본 국가)로 튀던 버그 수정 완료. 앱 최초 실행 + 저축↔여행 모드 전환 시에만 오늘 날짜 국가 자동 선택하도록 `needsAutoSelect`/`lifecycleChecked` 플래그 추가.
- `stores/auth.js`: 계정 전환 시 이전 계정의 localStorage/Pinia 캐시가 안 지워지던 버그 수정. `logout()`/`handleLoginSuccess()`에서 `tripass-*` 전체 캐시 정리 + `travelModeStore.resetForNewSession()` + `travelStore.resetGoal()` 호출하도록 변경.

이 두 건은 이미 로컬에 적용되어 있고 커밋/배포는 아직 안 된 상태입니다. 온보딩 작업과 함께 배포하시면 됩니다.
