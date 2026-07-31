# CONTRIBUTING — KB-TRIPass 협업 가이드

> 이 문서는 TRIPass 프로젝트의 모든 개발 협업 규칙을 정의합니다.  
> **규격 위반 시 리뷰 없이 Reject/Close 처리됩니다.**

---

## 1. Issue 생성 방법

### 원칙
- 모든 작업은 반드시 Issue를 먼저 생성한 후 시작합니다.
- 코드 작업 없이 Issue만 존재하는 것은 허용됩니다 (기획 단계).
- Issue 없이 생성된 브랜치/PR은 즉시 Close 됩니다.

### Issue 제목 형식

```
[도메인] 기능 설명
```

예시:
- `[AUTH] 로그인 API 구현`
- `[SAV] 홈 대시보드 조회 화면 개발`
- `[EXP] 지출 내역 목록 페이징 처리`

### Issue 본문에 포함할 내용

```markdown
## 작업 목적
이 기능이 필요한 이유를 서술합니다.

## 작업 범위
- 수정/추가할 파일 목록
- 구현할 API (있는 경우)
- 연관 화면 (있는 경우)

## 완료 조건 (Definition of Done)
- [ ] API 구현 완료 및 응답 포맷 검증
- [ ] 로컬 환경 정상 동작 확인
```

### 도메인 레이블

| 레이블 | 담당자 |
|---|---|
| `AUTH` `MYP` `PRO` `GDS` | 송형진 |
| `AST` | 이아영 |
| `SAV` | 권유현 |
| `TRV` `BUD` `FXC` `PRE` | 권원영 |
| `SCH` `EXP` `OCR` `REP` | 홍유진 |

---

## 2. 브랜치 생성 규칙

### 브랜치 구조

```
main      (배포 전용 / 직접 커밋 절대 금지)
└── develop (개발 통합 / 직접 커밋 절대 금지)
     ├── feature/<기능명>-#<issue-number>
     ├── fix/<기능명>-#<issue-number>
     └── refactor/<기능명>-#<issue-number>
```

### 명명 규칙

형식: `<prefix>/<기능명>-#<issue-number>`

| 규칙 | 상세 |
|---|---|
| `<prefix>` | `feature`, `fix`, `refactor` 중 하나 |
| `<기능명>` | 소문자 영문 + 하이픈(`-`)만 허용 |
| 이슈 번호 | 반드시 `-#숫자` 형식으로 마무리 |

```bash
# 올바른 예
git checkout -b feature/auth-login-#1
git checkout -b fix/exchange-rate-null-#12
git checkout -b refactor/saving-store-#7

# 잘못된 예 (Reject)
feature/AuthLogin_1    # 대문자, 언더바
feature/auth-login     # 이슈 번호 없음
feature/login          # 도메인 구분 없음
```

### 브랜치 생성 절차

```bash
git checkout develop
git pull origin develop
git checkout -b feature/<기능명>-#<issue-number>
```

---

## 3. 커밋 규칙

### 커밋 메시지 형식

```
<type>(<scope>): <message> (#<issue-number>)

[optional body - 72자 줄바꿈]

[optional footer - Closes #<issue-number>]
```

### Type 목록

| Type | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 구조 개선 |
| `style` | 포맷팅, 세미콜론 등 (코드 변화 X) |
| `docs` | 문서 수정 (README 등) |
| `chore` | 패키지, 빌드, 환경설정 변경 |

### 허용 Scope 목록

`navbar`, `home`, `account`, `statistics`, `settings`, `pinia`, `database`, `router`, `auth`, `api`, `common`

### 엄격한 작성 규칙

- 제목 첫 글자는 **소문자** 영문
- 제목 끝에 마침표(`.`) **절대 금지**
- 이슈 번호는 메시지 맨 끝에 `(#이슈번호)` 로 명시
- 제목 전체 **70자 이내**
- 본문과 제목 사이 **한 줄 공백**

### 올바른 예시

```
feat(auth): add login api integration (#1)
```

```
feat(pinia): implement user auth store actions (#4)

- Add fetchProfile and login actions
- Implement token refresh mechanism on expired sessions
- Handle 401 unauthorized errors gracefully

Closes #4
```

---

## 4. PR 규칙

### PR 제목 형식

```
<type>(<scope>): <PR 요약 내용> (#<issue-number>)
```

예시: `feat(auth): complete login and signup api (#1)`

- 작업 중인 PR은 제목 맨 앞에 **`[WIP]`** 필수 추가

### PR 본문

PR 생성 시 `.github/PULL_REQUEST_TEMPLATE.md` 가 자동 로드됩니다.  
모든 항목을 빠짐없이 작성해야 합니다.

### 절대 금지 (Auto Rejection)

| 위반 | 처리 |
|---|---|
| `main` 또는 `develop` 직접 Push | 즉시 Reject |
| Issue 번호 없는 커밋 / PR | 즉시 Reject |
| 1개 PR에 여러 Issue 작업 혼합 | 즉시 Reject |
| 테스트 실패 / 빌드 오류 상태에서 PR 생성 | 즉시 Reject |

---

## 5. 리뷰 및 병합 조건

### 리뷰어 지정

- 자신의 담당 도메인이 아닌 팀원 **최소 1명** 을 리뷰어로 지정

### Merge 승인 체크리스트 (팀장 확인)

- [ ] PR 본문에 `Closes #이슈번호` 포함 여부 확인
- [ ] 커밋 메시지 규격 (Type / Scope / 이슈번호) 검증
- [ ] 코드 리뷰 Approve **최소 1개 이상** 수령
- [ ] 로컬에서 빌드/동작 확인 완료

### 병합 방식

> **Squash and merge 만 사용** (히스토리 단일화)

`feature/*` → `develop` 으로만 PR.  
`develop` → `main` 은 팀장만 진행.

---

## 6. 충돌 해결 원칙

### 원칙 1 — 공통 코드는 먼저 논의

`common/` 패키지 (config, response, exception, util) 수정이 필요하면  
반드시 팀장 또는 전체 팀원과 **사전 논의 후** 작업합니다.

### 원칙 2 — develop 최신화 후 작업

```bash
# 작업 시작 전 항상 develop 을 최신으로 유지
git checkout develop
git pull origin develop
git checkout feature/my-feature-#N
git rebase develop
```

### 원칙 3 — 충돌 발생 시

1. 충돌이 발생한 파일의 **담당자에게 즉시 알림**
2. 담당자가 직접 해결하는 것을 원칙으로 함
3. 해결 불가 시 팀장 중재

### 원칙 4 — 도메인 경계 준수

각 팀원은 자신의 담당 도메인 패키지 외 코드를 임의로 수정하지 않습니다.  
담당 패키지 경계는 `DOMAIN_BOUNDARIES.md` 를 참고하세요.
