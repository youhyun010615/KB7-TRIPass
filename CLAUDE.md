# CLAUDE.md — TRIPass 프로젝트 AI 어시스턴트 가이드

이 파일은 Claude Code가 프로젝트 작업 시 자동으로 읽는 컨텍스트 파일입니다.

---

## 프로젝트 개요

**TRIPass**: 한국을 방문하는 외국인을 위한 여행 기반 금융 자산관리 앱.  
두 가지 모드: **저축모드** (여행 전 목표 저축) / **여행모드** (여행 중 예산·지출 관리).

KB IT's Your Life 7기 팀 프로젝트 — 기술 스택이 고정되어 있으므로 반드시 준수해야 합니다.

---

## 절대 변경 불가 기술 스택

| 구분 | 기술 | 버전 |
|---|---|---|
| Frontend | Vue.js | 3 |
| 상태관리 | Pinia | latest |
| 라우터 | Vue Router | 4 |
| HTTP | Axios | latest |
| 빌드 | Vite | latest |
| Backend | **Spring Framework** (**NOT Boot**) | **5.3.37** |
| ORM | MyBatis | **3.4.6** |
| MyBatis-Spring | MyBatis-Spring | **1.3.2** |
| DB Pool | HikariCP | **2.7.4** |
| Security | Spring Security | **5.8.14** |
| JWT | jjwt | 0.11.5 |
| DB | MySQL | 8.x |
| Runtime | Java | 17 |
| Server | Tomcat | 9 |
| Build | Gradle (WAR 배포) | |

### 절대 사용 금지

- `@SpringBootApplication` — Spring Boot가 아님
- `spring-boot-starter-*` — Boot 의존성 전부 금지
- `SpringApplication.run()` — Boot 진입점 금지
- `JPA`, `Hibernate` — MyBatis만 사용
- `application.yml` — `.properties` 파일만 사용

---

## 프로젝트 구조

```
KB7-TRIPass/
├── backend/
│   ├── build.gradle
│   └── src/main/java/com/tripass/
│       ├── common/
│       │   ├── config/        # Spring MVC 설정 (수정 시 팀 합의 필요)
│       │   ├── response/      # ApiResponse<T>
│       │   └── exception/     # CustomException, GlobalExceptionHandler
│       └── [domain]/          # 각 도메인 (controller/service/mapper/dto)
│   └── src/main/resources/
│       ├── application.properties
│       ├── mybatis-config.xml
│       ├── log4j2.xml
│       └── mapper/[domain]/   # MyBatis XML Mapper
└── frontend/src/
    ├── api/index.js            # axios 인스턴스
    ├── router/index.js
    ├── stores/                 # Pinia stores
    ├── views/[domain]/         # 페이지 컴포넌트
    └── components/common/      # 공통 UI 컴포넌트
```

---

## Spring MVC 필수 패턴

### 진입점 (web.xml 없음)

`WebAppInitializer.java` 가 `AbstractAnnotationConfigDispatcherServletInitializer` 를 상속해 Spring MVC를 초기화합니다. `web.xml` 파일은 존재하지 않으며 생성하지 마세요.

### 두 컨텍스트 구조

- **RootConfig**: DataSource, MyBatis, Services, Transactions (`@Service`, `@Mapper` 스캔)
- **WebMvcConfig**: Controllers, MVC 설정 (`@Controller`, `@RestController` 스캔)

두 설정이 분리되어 있습니다. 새 도메인 추가 시 양쪽 `@ComponentScan` / `@MapperScan` 에 패키지를 추가해야 합니다.

### Controller 패턴

```java
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseDto>>> getExpenses() {
        return ResponseEntity.ok(ApiResponse.success(expenseService.findAll()));
    }
}
```

### Service 패턴

```java
@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseMapper expenseMapper;

    public ExpenseService(ExpenseMapper expenseMapper) {
        this.expenseMapper = expenseMapper;
    }

    @Transactional
    public void createExpense(ExpenseDto dto) {
        expenseMapper.insert(dto);
    }
}
```

### MyBatis Mapper 패턴

```java
// Java Interface (in [domain]/mapper/)
@Mapper
public interface ExpenseMapper {
    List<ExpenseDto> findAll();
    void insert(ExpenseDto dto);
}
```

```xml
<!-- XML (in resources/mapper/expense/ExpenseMapper.xml) -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.tripass.expense.mapper.ExpenseMapper">
    <select id="findAll" resultType="ExpenseDto">
        SELECT * FROM expenses WHERE is_deleted = 0
    </select>
</mapper>
```

- `mapUnderscoreToCamelCase=true` 설정됨 → `expense_date` → `expenseDate` 자동 매핑
- Mapper XML 위치: `src/main/resources/mapper/[domain]/`
- Mapper 인터페이스 위치: `com.tripass.[domain].mapper`

### 예외 처리 패턴

```java
// 비즈니스 예외 throw
throw new CustomException(HttpStatus.NOT_FOUND, "EXPENSE_NOT_FOUND", "해당 지출 내역을 찾을 수 없습니다.");

// GlobalExceptionHandler 가 자동으로 ApiResponse.error() 로 변환
```

### 공통 응답 패턴

```java
// 성공
return ResponseEntity.ok(ApiResponse.success(data));
return ResponseEntity.status(201).body(ApiResponse.success("생성 완료", data));

// 삭제 등 응답 없는 경우
return ResponseEntity.noContent().build();
```

---

## DB 핵심 규칙

- 테이블/컬럼명: `snake_case` 소문자
- PK: `id BIGINT AUTO_INCREMENT PRIMARY KEY`
- FK 명: `<참조테이블단수>_id` (예: `user_id`, `trip_id`)
- 필수 컬럼: `created_at`, `updated_at` 모든 테이블
- 논리 삭제: `is_deleted TINYINT(1) DEFAULT 0`, `deleted_at DATETIME`
- 조회 쿼리: 반드시 `WHERE is_deleted = 0` 포함
- 금액(KRW): `INT` / 외화: `DECIMAL(15,2)` + `currency_code VARCHAR(3)`
- Enum: 숫자 아닌 **문자열** 저장 (`VARCHAR`)

---

## API 핵심 규칙

- Base URL: `/api/v1`
- URL: 소문자 + 하이픈, 복수 명사 (예: `/api/v1/saving-goals`)
- 응답: 항상 `ApiResponse<T>` 래퍼 사용
- 날짜: `yyyy-MM-dd` / 날짜+시간: `yyyy-MM-dd'T'HH:mm:ss`
- 인증: `Authorization: Bearer <token>` 헤더

---

## 팀원 도메인 담당

| 팀원 | 담당 도메인 | Java 패키지 |
|---|---|---|
| 송형진 | AUTH, MYP, PRO, GDS | `auth`, `mypage`, `profile`, `financial` |
| 이아영 | AST | `asset` |
| 권유현 | SAV | `saving` |
| 권원영 | TRV, BUD, FXC, PRE | `travel`, `exchange`, `prepay` |
| 홍유진 | SCH, EXP, OCR, REP | `schedule`, `expense`, `ocr`, `report` |

**`common/` 패키지는 팀 합의 후에만 수정.** 개인 도메인 외 코드를 임의로 수정하지 않습니다.

---

## Git 규칙 요약

```
브랜치: <prefix>/<기능명>-#<issue-number>
커밋:   <type>(<scope>): <message> (#<issue-number>)
병합:   Squash and merge, feature/* → develop 만
```

`main`, `develop` 직접 커밋/푸시 금지.

---

## 자주 쓰는 명령어

```bash
# 백엔드 빌드
cd backend && ./gradlew war

# 프론트엔드 개발 서버
cd frontend && npm run dev

# 의존성 확인
cd backend && ./gradlew dependencies

# 개발 브랜치 최신화
git checkout develop && git pull origin develop
```

---

## 참고 문서

- [CONTRIBUTING.md](CONTRIBUTING.md) — 협업 규칙 전체
- [API_CONVENTION.md](API_CONVENTION.md) — API 설계 규칙
- [DB_CONVENTION.md](DB_CONVENTION.md) — DB 설계 규칙
- [DOMAIN_BOUNDARIES.md](DOMAIN_BOUNDARIES.md) — 담당 경계
- [API_MAPPING.md](API_MAPPING.md) — 전체 API 목록 및 진척도
