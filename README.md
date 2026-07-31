# TRIPass — 외국인 여행자를 위한 금융 자산관리 앱

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

### 1. DB 설정

```sql
CREATE DATABASE tripass CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 백엔드 설정

```bash
cd backend
```

`src/main/resources/application-local.properties` 파일 생성 (gitignore 처리됨):

```properties
db.password=your_mysql_password
```

빌드 및 Tomcat 배포:

```bash
./gradlew war
# build/libs/tripass.war 를 Tomcat webapps/ 에 배포 후 Tomcat 실행
```

기본 포트: http://localhost:8080

### 3. 프론트엔드 설정

```bash
cd frontend
cp .env.example .env.local
# VITE_API_BASE_URL 확인 (기본값: http://localhost:8080/api/v1)
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

### 브랜치 전략 (Git Flow)

```
main
└── develop
    └── feature/[도메인]-[작업명]
```

예시: `feature/auth-login`, `feature/saving-home`, `feature/exchange-rate`

### 커밋 메시지 형식

```
<type>: <한국어 요약>
```

| type | 사용 시점 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변화 없음) |
| `style` | UI/CSS 수정 |
| `docs` | 문서 수정 |
| `chore` | 빌드 설정, 패키지 변경 |

예시:
```
feat: 로그인 API 구현
fix: 환율 조회 시 null 처리 오류 수정
```

### PR 규칙

- `feature/*` → `develop` 으로 PR
- 최소 1명 리뷰 후 merge
- PR 제목: `[도메인] 작업 내용` (예: `[AUTH] 로그인/회원가입 API`)
