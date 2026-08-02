# TRIPass — 해외여행 기반 금융 자산관리 서비스

KB IT's Your Life 7기 팀 프로젝트

## 서비스 개요

한국을 방문하는 외국인을 대상으로 **저축모드(여행 전)** 와 **여행모드(여행 중)** 를 전환하며 사용하는 금융 자산관리 앱입니다.

- **저축모드**: 목표 금액 설정 → 저축 플랜 → 자산 관리
- **여행모드**: 예산 관리 → 지출 추적(OCR 영수증) → 환율 환전 → 일정 연동

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Frontend | Vue.js 3 + Vite + Pinia + Vue Router + Axios |
| Backend | Spring Framework 5.3.37 (Legacy MVC, **NOT Boot**) |
| ORM | MyBatis 3.4.6 + MyBatis-Spring 1.3.2 |
| DB Connection | HikariCP 2.7.4 |
| Security | Spring Security 5.8.14 + JWT (jjwt 0.11.5) |
| Database | MySQL |
| Runtime | Java 17, Tomcat 9 |
| Build | Gradle (WAR) |
| Styling | Tailwind CSS v4 |

---

## 로컬 개발 환경 설정

### 사전 요구사항

- Java 17
- Node.js 20+
- MySQL 8.x
- Tomcat 9

### 1. DB 초기화

```sql
CREATE DATABASE tripass CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

스키마 초기화 SQL: `backend/src/main/resources/sql/schema.sql`

```bash
# MySQL Workbench 또는 CLI에서 실행
mysql -u root -p tripass < backend/src/main/resources/sql/schema.sql
```

### 2. 환경변수 설정

백엔드 환경변수:

```bash
cd backend
cp .env.example .env.local
# .env.local 에 DB 비밀번호, JWT 시크릿, 외부 API 키 입력
```

`application.properties` 는 `${환경변수명:기본값}` 형식으로 환경변수를 참조합니다.  
민감 정보는 `application-local.properties` 또는 OS 환경변수로 주입하세요.

프론트엔드 환경변수:

```bash
cd frontend
cp .env.example .env.local
# VITE_API_BASE_URL=http://localhost:8080/api/v1 입력
```

### 3. 백엔드 빌드 및 배포

```bash
cd backend
./gradlew war
# build/libs/tripass.war 를 Tomcat webapps/ 에 배포 후 Tomcat 실행
```

기본 포트: http://localhost:8080

### 4. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

개발 서버: http://localhost:5173

---

## 프로젝트 구조

```
KB7-TRIPass/
├── backend/
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/tripass/
│       │   ├── common/
│       │   │   ├── config/          # WebAppInitializer, RootConfig, WebMvcConfig, SecurityConfig
│       │   │   ├── response/        # ApiResponse<T>
│       │   │   └── exception/       # CustomException, GlobalExceptionHandler
│       │   ├── auth/                # 인증 (송형진)
│       │   ├── mypage/              # 마이페이지 (송형진)
│       │   ├── profile/             # 금융프로필 (송형진)
│       │   ├── financial/           # 금융상품 (송형진)
│       │   ├── asset/               # 자산관리 (이아영)
│       │   ├── saving/              # 저축/홈 (권유현)
│       │   ├── travel/              # 여행모드/예산 (권원영)
│       │   ├── exchange/            # 환율환전 (권원영)
│       │   ├── prepay/              # 사전지출 (권원영)
│       │   ├── schedule/            # 일정 (홍유진)
│       │   ├── expense/             # 지출 (홍유진)
│       │   ├── ocr/                 # 영수증OCR (홍유진)
│       │   └── report/              # 리포트 (홍유진)
│       └── resources/
│           ├── application.properties
│           ├── mybatis-config.xml
│           ├── log4j2.xml
│           └── mapper/              # MyBatis XML Mapper (도메인별)
└── frontend/
    └── src/
        ├── api/index.js             # axios 인스턴스 (JWT 자동 삽입, 401 처리)
        ├── router/index.js          # 라우트 + 인증 가드
        ├── stores/
        │   ├── auth.js              # 로그인 상태, accessToken
        │   └── travel.js            # 여행모드, 선택 국가
        ├── views/                   # 도메인별 페이지 컴포넌트
        └── components/common/       # 공통 UI 컴포넌트
```

---

## 관련 문서

| 문서 | 설명 |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | Issue · 브랜치 · 커밋 · PR · 충돌 해결 규칙 |
| [API_CONVENTION.md](API_CONVENTION.md) | REST API 설계 규칙 (URL, 응답 형식, 페이징 등) |
| [DB_CONVENTION.md](DB_CONVENTION.md) | DB 테이블/컬럼 설계 규칙 |
| [DOMAIN_BOUNDARIES.md](DOMAIN_BOUNDARIES.md) | 팀원별 담당 패키지 · 테이블 경계 |
| [API_MAPPING.md](API_MAPPING.md) | 요구사항 ID별 API · 테이블 · 진척도 추적 |
| [TRIPass Notion 화면정의서](https://app.notion.com/p/TRIPass-3adaa200f0a58096a624cb6487e9badf) | 전체 서비스 화면 설계 |

---

## 팀 도메인 담당

| 팀원 | 도메인 | 패키지 |
|---|---|---|
| 송형진 | AUTH, MYP, PRO, GDS | auth, mypage, profile, financial |
| 이아영 | AST | asset |
| 권유현 | SAV | saving |
| 권원영 | TRV/BUD, FXC, PRE | travel, exchange, prepay |
| 홍유진 | SCH, EXP, OCR, REP | schedule, expense, ocr, report |

---

## API 응답 형식

모든 API는 `ApiResponse<T>` 래퍼로 반환합니다.

```json
{
  "code": "SUCCESS",
  "message": "요청 성공",
  "data": { ... }
}
```

에러 응답:

```json
{
  "code": "INVALID_INPUT",
  "message": "입력값이 올바르지 않습니다.",
  "data": null
}
```

---

## Git 컨벤션

> **경고**: 모든 커밋, 브랜치, PR은 **Issue 번호(#)** 를 매개로 추적됩니다. 규격 위반 시 리뷰 없이 Reject/Close 처리됩니다.

### 브랜치 전략 (Git Flow)

```
main      (배포 전용 / 직접 커밋 절대 금지)
└── develop (개발 통합 / 직접 커밋 절대 금지)
     ├── feature/navbar-#1
     ├── feature/home-#2
     └── fix/login-#3
```

**브랜치 명명 규칙** (Regex 검사 기준)

형식: `<prefix>/<기능명>-#<issue-number>`

- `<prefix>`: `feature`, `fix`, `refactor` 만 허용
- `<기능명>`: 소문자 영문과 하이픈(`-`)만 사용
- 마지막은 반드시 `-#<이슈번호>` 형식

| 사용 가능 | 사용 불가 |
|---|---|
| `feature/navbar-#1` | `feature/Navbar_1` (대문자/언더바/이슈번호 형식 오류) |
| `fix/home-expense-#6` | `feature/auth` (이슈번호 없음) |

---

### 커밋 메시지 형식

```
<type>(<scope>): <message> (#<issue-number>)

[optional body - 72자 줄바꿈 및 상세 설명]

[optional footer - Closes #<issue-number>]
```

**Type & Scope 명세**

| Type | 설명 | 허용 Scope |
|---|---|---|
| `feat` | 새로운 기능 추가 | `navbar`, `home`, `account`, `statistics`, |
| `fix` | 버그 수정 | `settings`, `pinia`, `database`, `router`, |
| `refactor` | 기능 변경 없는 코드 구조 개선 | `auth`, `api`, `common` |
| `style` | 포맷팅, 세미콜론 등 (코드 변화 X) | *(위 목록의 키워드만 허용)* |
| `docs` | 문서 수정 (README 등) | |
| `chore` | 패키지, 빌드, 환경설정 변경 | |

**엄격한 작성 규칙**

- 제목 첫 글자는 소문자 영문 (`feat(...)`, `fix(...)`)
- 제목 끝에 마침표(`.`) 절대 금지
- 이슈 번호는 메시지 맨 끝에 `(#이슈번호)` 로 명시
- 제목 전체 70자 이내
- 본문과 제목 사이 한 줄 공백

**올바른 커밋 예시**

```
feat(navbar): add logo and menu items (#1)
```

```
feat(pinia): implement user auth store actions (#4)

- Add fetchProfile and login actions
- Implement token refresh mechanism on expired sessions
- Handle 401 unauthorized errors gracefully

Closes #4
```

---

### Pull Request 규칙

**PR 제목 형식**

```
<type>(<scope>): <PR 요약 내용> (#<issue-number>)
```

예시: `feat(navbar): complete navbar component implementation (#1)`

작업 중인 PR은 제목 맨 앞에 `[WIP]` 필수 추가.

**Merge 승인 조건 (팀장 체크리스트)**

- [ ] PR 본문에 `Closes #이슈번호` 포함 여부 확인
- [ ] 커밋 메시지 규격(Type/Scope/이슈번호) 검증
- [ ] 코드 리뷰 Approve **2명 이상** 수령
- [ ] 병합 방식은 **Squash and merge** 만 사용 (히스토리 단일화)

**절대 금지 (Auto Rejection)**

- `main` 또는 `develop` 브랜치 직접 Push
- Issue 번호 없는 커밋 / PR
- 1개 PR에 여러 Issue 작업 혼합 (1 Issue = 1 Feature Branch = 1 PR)
- 테스트 실패 및 빌드 오류 상태에서 PR 생성
