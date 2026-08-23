package com.tripass.asset.service.codef;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 로컬 QA 전용 가상 금융기관.
 *
 * <p>서비스 DB에 계좌/카드를 미리 넣지 않고 실제 CODEF 형태의 응답만 제공한다.
 * 사용자가 기존 연동 화면에서 아래 자격증명을 입력한 뒤에야 AssetService가 데이터를 저장한다.</p>
 *
 * <h3>Mock 사용자</h3>
 * <ul>
 *   <li>{@code tripassqa} / {@code Mock1234!} — 저축 모드 QA용 (국내 소비 분석·미션)</li>
 *   <li>{@code tripasstravel} / {@code Mock1234!} — 여행 모드 QA용 (저축 이력 + 유럽 여행 지출: 프랑스→독일→스위스)</li>
 *   <li>{@code demoprep} / {@code Mock1234!} — 시연 계정: 여행 전(준비중), 도쿄 여행 D-8</li>
 *   <li>{@code demotravel} / {@code Mock1234!} — 시연 계정: 여행 중, 프랑스(종료)→독일(진행중)→스위스(예정)</li>
 *   <li>{@code demodone} / {@code Mock1234!} — 시연 계정: 여행 후, 방콕 2주 여행 종료</li>
 *   <li>{@code demofresh} / {@code Mock1234!} — QA 계정: 여행 미등록 신규 상태. 계좌 연동 시
 *       2026년 1~8월 급여·고정비·소비 내역이 실거래 유사 패턴으로 채워진다(여행 목표 없이
 *       계좌·카드부터 연결하는 온보딩 흐름 테스트용)</li>
 * </ul>
 *
 * <p>demoprep/demotravel/demodone은 TriPass 시연 영상 촬영용으로 추가된 계정으로, DB에 이미
 * 완성된 시나리오 데이터(trips/schedules/receipts/transactions 등)가 시드되어 있다. 이 Mock
 * 페르소나는 "계좌 재연동" 등 CODEF 로그인 플로우를 라이브로 시연할 때 자격증명이 통과하도록
 * 최소한의 응답만 제공하며, DB 시드 데이터와 완전히 동일하지는 않다.</p>
 */
public class MockCodefClient implements CodefClient {

    public static final String LOGIN_ID = "tripassqa";
    public static final String TRAVEL_LOGIN_ID = "tripasstravel";
    public static final String PASSWORD = "Mock1234!";
    public static final String CONNECTED_ID = "MOCK-CONNECTED-TRIPASS-QA";
    public static final String TRAVEL_CONNECTED_ID = "MOCK-CONNECTED-TRIPASS-TRAVEL";
    public static final String BANK_ORGANIZATION = "0004";
    public static final String CARD_ORGANIZATION = "0301";

    // ===== 시연 데모 계정 (demoprep / demotravel / demodone) =====
    public static final String DEMO_PREP_LOGIN_ID = "demoprep";
    public static final String DEMO_TRAVEL_LOGIN_ID = "demotravel";
    public static final String DEMO_DONE_LOGIN_ID = "demodone";
    public static final String DEMO_PASSWORD = "Mock1234!";
    public static final String DEMO_PREP_CONNECTED_ID = "MOCK-CONNECTED-DEMO-PREP";
    public static final String DEMO_TRAVEL_CONNECTED_ID = "MOCK-CONNECTED-DEMO-TRAVEL";
    public static final String DEMO_DONE_CONNECTED_ID = "MOCK-CONNECTED-DEMO-DONE";

    // ===== 신규 QA 계정 (28세 직장인, 1~8월 실거래 유사 데이터, 계좌·카드 연동 시연용) =====
    public static final String DEMO_FRESH_LOGIN_ID = "demofresh";
    public static final String DEMO_FRESH_CONNECTED_ID = "MOCK-CONNECTED-DEMO-FRESH";

    // ===== yuhyun 데모 계정 (실제 거래 + 가상 데이터, 8개 체크포인트 시나리오) =====
    public static final String YUHYUN_LOGIN_ID = "yuhyun";
    public static final String YUHYUN_PASSWORD = "Mock1234!";
    public static final String YUHYUN_CONNECTED_ID = "MOCK-CONNECTED-YUHYUN";
    public static final String YUHYUN_BANK_ACCOUNT = "496501-01-110300";
    public static final String YUHYUN_NORI_CARD_NO = "5412-****-****-9901";
    public static final String YUHYUN_TRAVEL_CARD_NO = "5412-****-****-9902";

    // ===== yuhyun 데모 계정 복제본 (동일 계정 동시 QA 충돌 방지용, yuhyun2~yuhyun5) =====
    // 로그인 ID/비밀번호/연동 정보만 다르고 거래 데이터는 yuhyun과 동일한 것을 그대로 재사용한다.
    private record YuhyunClone(String loginId, String connectedId, String bankAccount,
                                String noriCardNo, String travelCardNo) {
    }

    private static final List<YuhyunClone> YUHYUN_CLONES = List.of(
            new YuhyunClone("yuhyun2", "MOCK-CONNECTED-YUHYUN2", "496501-01-110301",
                    "5412-****-****-9903", "5412-****-****-9904"),
            new YuhyunClone("yuhyun3", "MOCK-CONNECTED-YUHYUN3", "496501-01-110302",
                    "5412-****-****-9905", "5412-****-****-9906"),
            new YuhyunClone("yuhyun4", "MOCK-CONNECTED-YUHYUN4", "496501-01-110303",
                    "5412-****-****-9907", "5412-****-****-9908"),
            new YuhyunClone("yuhyun5", "MOCK-CONNECTED-YUHYUN5", "496501-01-110304",
                    "5412-****-****-9909", "5412-****-****-9910")
    );

    private static YuhyunClone findCloneByLoginId(String loginId) {
        return YUHYUN_CLONES.stream().filter(c -> c.loginId().equals(loginId)).findFirst().orElse(null);
    }

    private static YuhyunClone findCloneByConnectedId(String connectedId) {
        return YUHYUN_CLONES.stream().filter(c -> c.connectedId().equals(connectedId)).findFirst().orElse(null);
    }

    private static final String ACCESS_TOKEN = "mock-codef-access-token";

    // ===== tripassqa 데이터 =====
    private static final List<Map<String, Object>> BANK_TRANSACTIONS = buildBankTransactions();
    private static final List<Map<String, Object>> GENERAL_CARD_TRANSACTIONS = buildGeneralCardTransactions();
    private static final List<Map<String, Object>> TRAVEL_CARD_TRANSACTIONS = List.of(
            cardTransaction("20260718", "101000", "42000", "QT2001", "대한항공", "항공사"),
            cardTransaction("20260719", "143000", "185000", "QT2002", "호텔스닷컴", "숙박"),
            cardTransaction("20260722", "180000", "68000", "QT2003", "스위스패스", "해외교통"),
            cardTransaction("20260724", "193000", "52000", "QT2004", "ZURICH DINING", "해외음식점"),
            cardTransaction("20260809", "160000", "36000", "QT2005", "JR EAST", "해외교통"),
            cardTransaction("20260810", "193000", "52000", "QT2006", "SUSHI TOKYO", "해외음식점"),
            cardTransaction("20260812", "140000", "89000", "QT2007", "TOKYO HOTEL", "숙박"),
            cardTransaction("20260814", "173000", "31000", "QT2008", "TOKYO SOUVENIR", "해외쇼핑")
    );

    // ===== tripasstravel 데이터 =====
    private static final List<Map<String, Object>> TRAVEL_USER_BANK_TRANSACTIONS = buildTravelUserBankTransactions();
    private static final List<Map<String, Object>> TRAVEL_USER_CARD_TRANSACTIONS = buildTravelUserCardTransactions();
    private static final List<Map<String, Object>> TRAVEL_USER_TRAVELCARD_TRANSACTIONS = buildTravelUserTravelCardTransactions();

    // ===== demofresh 데이터 (28세 직장인, 2026년 1~8월) =====
    private static final List<Map<String, Object>> FRESH_BANK_TRANSACTIONS = buildFreshBankTransactions();
    private static final List<Map<String, Object>> FRESH_CARD_TRANSACTIONS = buildFreshCardTransactions();
    private static final List<Map<String, Object>> FRESH_TRAVEL_CARD_TRANSACTIONS = buildFreshTravelCardTransactions();

    // ===== yuhyun 데모 데이터 (JSON 파일 로딩) =====
    private static final List<Map<String, Object>> YUHYUN_BANK_TRANSACTIONS = loadYuhyunBankTransactions();
    private static final List<Map<String, Object>> YUHYUN_NORI_CARD_TRANSACTIONS = loadYuhyunNoriCardTransactions();
    private static final List<Map<String, Object>> YUHYUN_TRAVEL_CARD_TRANSACTIONS = loadYuhyunTravelCardTransactions();

    @Override
    public String encodePassword(String plainPassword) {
        return plainPassword;
    }

    @Override
    public String getAccessToken() {
        return ACCESS_TOKEN;
    }

    @Override
    public Map<String, Object> callApi(String accessToken, String path, Map<String, Object> body) {
        if (!ACCESS_TOKEN.equals(accessToken)) {
            return failure("CF-01001", "유효하지 않은 Mock 액세스 토큰입니다.");
        }

        return switch (path) {
            case "/v1/account/create", "/v1/account/add" -> connect(body);
            case "/v1/kr/bank/p/account/account-list" -> bankAccounts(body);
            case "/v1/kr/card/p/account/card-list" -> cards(body);
            case "/v1/kr/bank/p/account/transaction-list" -> bankTransactions(body);
            case "/v1/kr/card/p/account/approval-list" -> cardTransactions(body);
            default -> failure("CF-04004", "지원하지 않는 Mock CODEF 경로입니다: " + path);
        };
    }

    private String resolveLoginId(Map<String, Object> body) {
        Object rawAccounts = body.get("accountList");
        if (rawAccounts instanceof List<?> accounts && !accounts.isEmpty()
                && accounts.get(0) instanceof Map<?, ?> account) {
            return String.valueOf(account.get("id"));
        }
        return null;
    }

    private String resolveConnectedId(Map<String, Object> body) {
        return String.valueOf(body.get("connectedId"));
    }

    private boolean isTravelUser(Map<String, Object> body) {
        return TRAVEL_CONNECTED_ID.equals(resolveConnectedId(body));
    }

    private Map<String, Object> connect(Map<String, Object> body) {
        Object rawAccounts = body.get("accountList");
        if (!(rawAccounts instanceof List<?> accounts) || accounts.isEmpty()
                || !(accounts.get(0) instanceof Map<?, ?> account)) {
            return failure("CF-01002", "연동 계정 정보가 없습니다.");
        }

        String loginId = String.valueOf(account.get("id"));
        String password = String.valueOf(account.get("password"));
        String organization = String.valueOf(account.get("organization"));

        if (!BANK_ORGANIZATION.equals(organization) && !CARD_ORGANIZATION.equals(organization)) {
            return failure("CF-01003", "Mock에서 지원하지 않는 금융기관입니다.");
        }

        if (LOGIN_ID.equals(loginId) && PASSWORD.equals(password)) {
            return success(Map.of("connectedId", CONNECTED_ID));
        }
        if (TRAVEL_LOGIN_ID.equals(loginId) && PASSWORD.equals(password)) {
            return success(Map.of("connectedId", TRAVEL_CONNECTED_ID));
        }
        if (DEMO_PREP_LOGIN_ID.equals(loginId) && DEMO_PASSWORD.equals(password)) {
            return success(Map.of("connectedId", DEMO_PREP_CONNECTED_ID));
        }
        if (DEMO_TRAVEL_LOGIN_ID.equals(loginId) && DEMO_PASSWORD.equals(password)) {
            return success(Map.of("connectedId", DEMO_TRAVEL_CONNECTED_ID));
        }
        if (DEMO_DONE_LOGIN_ID.equals(loginId) && DEMO_PASSWORD.equals(password)) {
            return success(Map.of("connectedId", DEMO_DONE_CONNECTED_ID));
        }
        if (DEMO_FRESH_LOGIN_ID.equals(loginId) && DEMO_PASSWORD.equals(password)) {
            return success(Map.of("connectedId", DEMO_FRESH_CONNECTED_ID));
        }
        if (YUHYUN_LOGIN_ID.equals(loginId) && YUHYUN_PASSWORD.equals(password)) {
            return success(Map.of("connectedId", YUHYUN_CONNECTED_ID));
        }
        YuhyunClone clone = findCloneByLoginId(loginId);
        if (clone != null && YUHYUN_PASSWORD.equals(password)) {
            return success(Map.of("connectedId", clone.connectedId()));
        }

        return failure("CF-01002", "Mock 금융기관 아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    private Map<String, Object> bankAccounts(Map<String, Object> body) {
        String connectedId = resolveConnectedId(body);
        if (!BANK_ORGANIZATION.equals(String.valueOf(body.get("organization")))) {
            return failure("CF-01004", "연동되지 않은 Mock 은행입니다.");
        }

        if (CONNECTED_ID.equals(connectedId)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("resDepositTrust", List.of(
                    mapOf(
                            "resAccount", "98765432101234",
                            "resAccountName", "KB Star 정기예금",
                            "resAccountKind", "예금",
                            "resAccountBalance", "12000000",
                            "resWithdrawableAmount", "12000000"
                    ),
                    mapOf(
                            "resAccount", "12345678901234",
                            "resAccountName", "KB QA 주거래통장",
                            "resAccountKind", "입출금",
                            "resAccountBalance", "14379300",
                            "resWithdrawableAmount", "14379300"
                    )
            ));
            return success(data);
        }

        if (TRAVEL_CONNECTED_ID.equals(connectedId)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("resDepositTrust", List.of(
                    mapOf(
                            "resAccount", "55512340001234",
                            "resAccountName", "KB 여행적금",
                            "resAccountKind", "입출금",
                            "resAccountBalance", "8520000",
                            "resWithdrawableAmount", "8520000"
                    )
            ));
            return success(data);
        }

        if (DEMO_PREP_CONNECTED_ID.equals(connectedId)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("resDepositTrust", List.of(
                    mapOf("resAccount", "44401230001111", "resAccountName", "KB국민은행 급여통장",
                            "resAccountKind", "입출금", "resAccountBalance", "13295000", "resWithdrawableAmount", "13295000"),
                    mapOf("resAccount", "44401230002222", "resAccountName", "KB국민은행 여행저축통장",
                            "resAccountKind", "입출금", "resAccountBalance", "780000", "resWithdrawableAmount", "780000")
            ));
            return success(data);
        }

        if (DEMO_TRAVEL_CONNECTED_ID.equals(connectedId)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("resDepositTrust", List.of(
                    mapOf("resAccount", "44402230001111", "resAccountName", "KB국민은행 여행통장",
                            "resAccountKind", "입출금", "resAccountBalance", "320000", "resWithdrawableAmount", "320000"),
                    mapOf("resAccount", "44402230002222", "resAccountName", "KB국민은행 생활비통장",
                            "resAccountKind", "입출금", "resAccountBalance", "1580000", "resWithdrawableAmount", "1580000")
            ));
            return success(data);
        }

        if (DEMO_DONE_CONNECTED_ID.equals(connectedId)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("resDepositTrust", List.of(
                    mapOf("resAccount", "44403230001111", "resAccountName", "KB국민은행 여행통장",
                            "resAccountKind", "입출금", "resAccountBalance", "1150000", "resWithdrawableAmount", "1150000"),
                    mapOf("resAccount", "44403230002222", "resAccountName", "KB국민은행 급여통장",
                            "resAccountKind", "입출금", "resAccountBalance", "3020000", "resWithdrawableAmount", "3020000")
            ));
            return success(data);
        }

        if (DEMO_FRESH_CONNECTED_ID.equals(connectedId)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("resDepositTrust", List.of(
                    mapOf("resAccount", "44404230001111", "resAccountName", "KB국민은행 급여통장",
                            "resAccountKind", "입출금", "resAccountBalance", "10624000", "resWithdrawableAmount", "10624000")
            ));
            return success(data);
        }

        if (YUHYUN_CONNECTED_ID.equals(connectedId)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("resDepositTrust", List.of(
                    mapOf("resAccount", YUHYUN_BANK_ACCOUNT, "resAccountName", "KB국민은행 종합통장",
                            "resAccountKind", "입출금", "resAccountBalance", "990924", "resWithdrawableAmount", "990924")
            ));
            return success(data);
        }

        YuhyunClone clone = findCloneByConnectedId(connectedId);
        if (clone != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("resDepositTrust", List.of(
                    mapOf("resAccount", clone.bankAccount(), "resAccountName", "KB국민은행 종합통장",
                            "resAccountKind", "입출금", "resAccountBalance", "990924", "resWithdrawableAmount", "990924")
            ));
            return success(data);
        }

        return failure("CF-01004", "연동되지 않은 Mock 은행입니다.");
    }

    private Map<String, Object> cards(Map<String, Object> body) {
        String connectedId = resolveConnectedId(body);
        if (!CARD_ORGANIZATION.equals(String.valueOf(body.get("organization")))) {
            return failure("CF-01004", "연동되지 않은 Mock 카드사입니다.");
        }

        if (CONNECTED_ID.equals(connectedId)) {
            return success(List.of(
                    mapOf(
                            "resCardName", "KB QA 체크카드",
                            "resCardNo", "5412-****-****-2710",
                            "resCardType", "02",
                            "resPaymentAccount", "12345678901234"
                    ),
                    mapOf(
                            "resCardName", "트래블러스 체크카드",
                            "resCardNo", "5412-****-****-2711",
                            "resCardType", "02",
                            "resPaymentAccount", "12345678901234"
                    )
            ));
        }

        if (TRAVEL_CONNECTED_ID.equals(connectedId)) {
            return success(List.of(
                    mapOf(
                            "resCardName", "KB 생활비 체크카드",
                            "resCardNo", "5412-****-****-8801",
                            "resCardType", "02",
                            "resPaymentAccount", "55512340001234"
                    ),
                    mapOf(
                            "resCardName", "KB 트래블 체크카드",
                            "resCardNo", "5412-****-****-8802",
                            "resCardType", "02",
                            "resPaymentAccount", "55512340001234"
                    )
            ));
        }

        if (DEMO_PREP_CONNECTED_ID.equals(connectedId)) {
            return success(List.of(mapOf(
                    "resCardName", "KB Star 체크카드", "resCardNo", "5412-****-****-3301",
                    "resCardType", "02", "resPaymentAccount", "44401230001111"
            )));
        }

        if (DEMO_TRAVEL_CONNECTED_ID.equals(connectedId)) {
            return success(List.of(mapOf(
                    "resCardName", "KB 트래블러스 체크카드", "resCardNo", "5412-****-****-8801",
                    "resCardType", "02", "resPaymentAccount", "44402230001111"
            )));
        }

        if (DEMO_DONE_CONNECTED_ID.equals(connectedId)) {
            return success(List.of(mapOf(
                    "resCardName", "KB 트래블러스 체크카드", "resCardNo", "5412-****-****-7701",
                    "resCardType", "02", "resPaymentAccount", "44403230001111"
            )));
        }

        if (DEMO_FRESH_CONNECTED_ID.equals(connectedId)) {
            return success(List.of(
                    mapOf("resCardName", "KB Star 체크카드", "resCardNo", "5412-****-****-4401",
                            "resCardType", "02", "resPaymentAccount", "44404230001111"),
                    mapOf("resCardName", "KB 트래블러스 체크카드", "resCardNo", "5412-****-****-4402",
                            "resCardType", "02", "resPaymentAccount", "44404230001111")
            ));
        }

        if (YUHYUN_CONNECTED_ID.equals(connectedId)) {
            return success(List.of(
                    mapOf("resCardName", "KB nori 체크카드", "resCardNo", YUHYUN_NORI_CARD_NO,
                            "resCardType", "02", "resPaymentAccount", YUHYUN_BANK_ACCOUNT),
                    mapOf("resCardName", "KB 트래블러스 체크카드", "resCardNo", YUHYUN_TRAVEL_CARD_NO,
                            "resCardType", "02", "resPaymentAccount", YUHYUN_BANK_ACCOUNT)
            ));
        }

        {
            YuhyunClone clone = findCloneByConnectedId(connectedId);
            if (clone != null) {
                return success(List.of(
                        mapOf("resCardName", "KB nori 체크카드", "resCardNo", clone.noriCardNo(),
                                "resCardType", "02", "resPaymentAccount", clone.bankAccount()),
                        mapOf("resCardName", "KB 트래블러스 체크카드", "resCardNo", clone.travelCardNo(),
                                "resCardType", "02", "resPaymentAccount", clone.bankAccount())
                ));
            }
        }

        return failure("CF-01004", "연동되지 않은 Mock 카드사입니다.");
    }

    private Map<String, Object> bankTransactions(Map<String, Object> body) {
        String connectedId = resolveConnectedId(body);
        if (!BANK_ORGANIZATION.equals(String.valueOf(body.get("organization")))) {
            return failure("CF-01004", "연동되지 않은 Mock 은행입니다.");
        }
        String account = String.valueOf(body.get("account"));

        if (CONNECTED_ID.equals(connectedId)) {
            List<Map<String, Object>> transactions = "12345678901234".equals(account)
                    ? filterByDate(BANK_TRANSACTIONS, body, "resAccountTrDate")
                    : List.of();
            return success(Map.of("resTrHistoryList", transactions));
        }

        if (TRAVEL_CONNECTED_ID.equals(connectedId)) {
            List<Map<String, Object>> transactions = "55512340001234".equals(account)
                    ? filterByDate(TRAVEL_USER_BANK_TRANSACTIONS, body, "resAccountTrDate")
                    : List.of();
            return success(Map.of("resTrHistoryList", transactions));
        }

        if (DEMO_FRESH_CONNECTED_ID.equals(connectedId)) {
            List<Map<String, Object>> transactions = "44404230001111".equals(account)
                    ? filterByDate(FRESH_BANK_TRANSACTIONS, body, "resAccountTrDate")
                    : List.of();
            return success(Map.of("resTrHistoryList", transactions));
        }

        // 데모 계정 3종은 거래내역이 이미 DB에 시연용으로 시드되어 있으므로,
        // 여기서는 재연동/동기화 버튼이 에러 없이 동작하도록 빈 목록만 반환한다.
        if (DEMO_PREP_CONNECTED_ID.equals(connectedId)
                || DEMO_TRAVEL_CONNECTED_ID.equals(connectedId)
                || DEMO_DONE_CONNECTED_ID.equals(connectedId)) {
            return success(Map.of("resTrHistoryList", List.of()));
        }

        if (YUHYUN_CONNECTED_ID.equals(connectedId)) {
            List<Map<String, Object>> transactions = YUHYUN_BANK_ACCOUNT.equals(account)
                    ? filterByDate(YUHYUN_BANK_TRANSACTIONS, body, "resAccountTrDate")
                    : List.of();
            return success(Map.of("resTrHistoryList", transactions));
        }

        {
            YuhyunClone clone = findCloneByConnectedId(connectedId);
            if (clone != null) {
                List<Map<String, Object>> transactions = clone.bankAccount().equals(account)
                        ? filterByDate(YUHYUN_BANK_TRANSACTIONS, body, "resAccountTrDate")
                        : List.of();
                return success(Map.of("resTrHistoryList", transactions));
            }
        }

        return failure("CF-01004", "연동되지 않은 Mock 은행입니다.");
    }

    private Map<String, Object> cardTransactions(Map<String, Object> body) {
        String connectedId = resolveConnectedId(body);
        String cardNo = String.valueOf(body.get("cardNo"));

        if (CONNECTED_ID.equals(connectedId)) {
            List<Map<String, Object>> source = switch (cardNo) {
                case "5412-****-****-2710" -> GENERAL_CARD_TRANSACTIONS;
                case "5412-****-****-2711" -> TRAVEL_CARD_TRANSACTIONS;
                default -> List.of();
            };
            return success(filterByDate(source, body, "resUsedDate"));
        }

        // 데모 계정 3종: 카드 거래내역도 DB에 이미 시드되어 있으므로 빈 목록 반환.
        if (DEMO_PREP_CONNECTED_ID.equals(connectedId)
                || DEMO_TRAVEL_CONNECTED_ID.equals(connectedId)
                || DEMO_DONE_CONNECTED_ID.equals(connectedId)) {
            return success(List.of());
        }

        if (TRAVEL_CONNECTED_ID.equals(connectedId)) {
            List<Map<String, Object>> source = switch (cardNo) {
                case "5412-****-****-8801" -> TRAVEL_USER_CARD_TRANSACTIONS;
                case "5412-****-****-8802" -> TRAVEL_USER_TRAVELCARD_TRANSACTIONS;
                default -> List.of();
            };
            return success(filterByDate(source, body, "resUsedDate"));
        }

        if (DEMO_FRESH_CONNECTED_ID.equals(connectedId)) {
            List<Map<String, Object>> source = switch (cardNo) {
                case "5412-****-****-4401" -> FRESH_CARD_TRANSACTIONS;
                case "5412-****-****-4402" -> FRESH_TRAVEL_CARD_TRANSACTIONS;
                default -> List.of();
            };
            return success(filterByDate(source, body, "resUsedDate"));
        }

        if (YUHYUN_CONNECTED_ID.equals(connectedId)) {
            List<Map<String, Object>> source = switch (cardNo) {
                case "5412-****-****-9901" -> YUHYUN_NORI_CARD_TRANSACTIONS;
                case "5412-****-****-9902" -> YUHYUN_TRAVEL_CARD_TRANSACTIONS;
                default -> List.of();
            };
            return success(filterByDate(source, body, "resUsedDate"));
        }

        {
            YuhyunClone clone = findCloneByConnectedId(connectedId);
            if (clone != null) {
                List<Map<String, Object>> source = List.of();
                if (clone.noriCardNo().equals(cardNo)) source = YUHYUN_NORI_CARD_TRANSACTIONS;
                else if (clone.travelCardNo().equals(cardNo)) source = YUHYUN_TRAVEL_CARD_TRANSACTIONS;
                return success(filterByDate(source, body, "resUsedDate"));
            }
        }

        return failure("CF-01004", "연동되지 않은 Mock 카드사입니다.");
    }

    private List<Map<String, Object>> filterByDate(
            List<Map<String, Object>> source,
            Map<String, Object> body,
            String dateField
    ) {
        String start = String.valueOf(body.getOrDefault("startDate", "00000000")).replace("-", "");
        String end = String.valueOf(body.getOrDefault("endDate", "99999999")).replace("-", "");
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> item : source) {
            String date = String.valueOf(item.get(dateField));
            if (date.compareTo(start) >= 0 && date.compareTo(end) <= 0) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    // ===================================================================
    // tripassqa 거래 데이터 (저축 모드 QA)
    // ===================================================================

    private static List<Map<String, Object>> buildBankTransactions() {
        List<Map<String, Object>> transactions = new ArrayList<>();
        transactions.add(bankTransaction("20260401", "090000", "3500000", "0", "3500000", "4월 급여"));
        transactions.add(bankTransaction("20260405", "081500", "0", "296000", "3204000", "KB카드 결제"));
        transactions.add(bankTransaction("20260410", "120000", "0", "150000", "3054000", "여행 월렛 저축"));
        transactions.add(bankTransaction("20260425", "183000", "0", "89000", "2965000", "통신비 자동이체"));
        transactions.add(bankTransaction("20260501", "090000", "3500000", "0", "6465000", "5월 급여"));
        transactions.add(bankTransaction("20260505", "081500", "0", "318000", "6147000", "KB카드 결제"));
        transactions.add(bankTransaction("20260510", "120000", "0", "150000", "5997000", "여행 월렛 저축"));
        transactions.add(bankTransaction("20260525", "183000", "0", "89000", "5908000", "통신비 자동이체"));
        transactions.add(bankTransaction("20260601", "090000", "3500000", "0", "9408000", "6월 급여"));
        transactions.add(bankTransaction("20260605", "081500", "0", "342000", "9066000", "KB카드 결제"));
        transactions.add(bankTransaction("20260610", "120000", "0", "200000", "8866000", "여행 월렛 저축"));
        transactions.add(bankTransaction("20260625", "183000", "0", "89000", "8777000", "통신비 자동이체"));
        transactions.add(bankTransaction("20260701", "090000", "3500000", "0", "12277000", "7월 급여"));
        transactions.add(bankTransaction("20260705", "081500", "0", "546000", "11731000", "KB카드 결제"));
        transactions.add(bankTransaction("20260710", "120000", "0", "150000", "11581000", "여행 월렛 저축"));
        transactions.add(bankTransaction("20260725", "183000", "0", "89000", "11492000", "통신비 자동이체"));
        transactions.add(bankTransaction("20260801", "090000", "3500000", "0", "14992000", "8월 급여"));
        transactions.add(bankTransaction("20260805", "081500", "0", "412700", "14579300", "KB카드 결제"));
        transactions.add(bankTransaction("20260812", "121000", "0", "200000", "14379300", "여행 월렛 저축"));
        return List.copyOf(transactions);
    }

    /**
     * 2026년 4~6월 비교 데이터, 7월 분석 데이터, 8월 미션 진행 데이터를 모두 포함한다.
     * 사용자가 Mock 카드를 연결한 뒤 승인내역을 동기화해야 서비스 DB에 적재된다.
     */
    private static List<Map<String, Object>> buildGeneralCardTransactions() {
        List<Map<String, Object>> transactions = new ArrayList<>();
        int[] comparisonDays = {2, 6, 11, 17, 24};

        for (int month = 4; month <= 6; month++) {
            String yearMonth = "2026" + String.format("%02d", month);
            int adjustment = (month - 4) * 500;
            addRegularCategoryTransactions(transactions, yearMonth, "FOOD", "일반음식점",
                    new String[]{"한상차림", "오늘의식탁", "도시락공방"},
                    comparisonDays, 10500 + adjustment, 1000);
            addRegularCategoryTransactions(transactions, yearMonth, "CAFE", "커피전문점",
                    new String[]{"카페모먼트", "브루잉하우스", "데일리커피"},
                    comparisonDays, 7500 + adjustment, 500);
            addRegularCategoryTransactions(transactions, yearMonth, "SHOPPING", "일반의류",
                    new String[]{"스타일샵", "데일리몰", "라이프마켓"},
                    comparisonDays, 14000 + adjustment, 1500);
            addRegularCategoryTransactions(transactions, yearMonth, "LIVING", "편의점",
                    new String[]{"KB편의점", "생활마켓", "우리약국"},
                    comparisonDays, 11000 + adjustment, 700);
            addRegularCategoryTransactions(transactions, yearMonth, "TRANSPORT", "택시",
                    new String[]{"KB택시", "서울교통", "모바일택시"},
                    comparisonDays, 9000 + adjustment, 500);
            addRegularCategoryTransactions(transactions, yearMonth, "LEISURE", "영화공연장",
                    new String[]{"메가시네마", "문화극장", "플레이존"},
                    comparisonDays, 12000 + adjustment, 1200);
        }

        int[] analysisDays = {2, 5, 9, 13, 18, 24};
        addRegularCategoryTransactions(transactions, "202607", "FOOD", "일반음식점",
                new String[]{"한상차림", "오늘의식탁", "키친테이블"}, analysisDays, 16000, 1200);
        addRegularCategoryTransactions(transactions, "202607", "CAFE", "커피전문점",
                new String[]{"카페모먼트", "브루잉하우스", "데일리커피"}, analysisDays, 9000, 600);
        addRegularCategoryTransactions(transactions, "202607", "SHOPPING", "일반의류",
                new String[]{"스타일샵", "데일리몰", "라이프마켓"}, analysisDays, 22000, 1800);
        addRegularCategoryTransactions(transactions, "202607", "LIVING", "편의점",
                new String[]{"KB편의점", "생활마켓", "우리약국"}, analysisDays, 12000, 800);
        addRegularCategoryTransactions(transactions, "202607", "TRANSPORT", "택시",
                new String[]{"KB택시", "서울교통", "모바일택시"}, analysisDays, 9500, 700);
        addRegularCategoryTransactions(transactions, "202607", "LEISURE", "영화공연장",
                new String[]{"메가시네마", "문화극장", "플레이존"}, analysisDays, 15500, 1500);

        transactions.add(cardTransaction("20260707", "211000", "12900", "QA2607OTHER01",
                "넷플릭스", "온라인서비스"));
        transactions.add(cardTransaction("20260714", "133000", "14500", "QA2607OTHER02",
                "교보문고", "서점"));
        transactions.add(cardTransaction("20260727", "170000", "27000", "QA2607OTHER03",
                "반려생활", "기타서비스"));

        int[] missionDays = {2, 5, 9, 12, 16};
        addCategoryTransactions(transactions, "202608", "FOOD", "일반음식점",
                new String[]{"한상차림", "오늘의식탁", "키친테이블"}, missionDays,
                new int[]{9000, 10000, 18000, 17000, 12000});
        addCategoryTransactions(transactions, "202608", "CAFE", "커피전문점",
                new String[]{"카페모먼트", "브루잉하우스", "데일리커피"}, missionDays,
                new int[]{4000, 4500, 7000, 7500, 5000});
        addCategoryTransactions(transactions, "202608", "SHOPPING", "일반의류",
                new String[]{"스타일샵", "데일리몰", "라이프마켓"}, missionDays,
                new int[]{9000, 11000, 21000, 22000, 15000});
        addCategoryTransactions(transactions, "202608", "LIVING", "편의점",
                new String[]{"KB편의점", "생활마켓", "우리약국"}, missionDays,
                new int[]{6000, 6500, 9000, 10000, 7000});
        addCategoryTransactions(transactions, "202608", "TRANSPORT", "택시",
                new String[]{"KB택시", "서울교통", "모바일택시"}, missionDays,
                new int[]{4500, 5000, 7000, 7500, 5500});
        addCategoryTransactions(transactions, "202608", "LEISURE", "영화공연장",
                new String[]{"메가시네마", "문화극장", "플레이존"}, missionDays,
                new int[]{7000, 8000, 14000, 15000, 9000});

        return List.copyOf(transactions);
    }

    // ===================================================================
    // tripasstravel 거래 데이터 (여행 모드 QA)
    // 시나리오: 4~7월 저축하며 여행 준비 → 8/16 출발 (프랑스 → 독일 → 스위스)
    // ===================================================================

    private static List<Map<String, Object>> buildTravelUserBankTransactions() {
        List<Map<String, Object>> t = new ArrayList<>();
        // 4월: 급여 + 저축 + 생활비
        t.add(bankTransaction("20260401", "090000", "3200000", "0", "3200000", "4월 급여"));
        t.add(bankTransaction("20260405", "100000", "0", "280000", "2920000", "KB카드 결제"));
        t.add(bankTransaction("20260410", "120000", "0", "300000", "2620000", "여행 월렛 저축"));
        t.add(bankTransaction("20260425", "183000", "0", "85000", "2535000", "통신비 자동이체"));
        // 5월
        t.add(bankTransaction("20260501", "090000", "3200000", "0", "5735000", "5월 급여"));
        t.add(bankTransaction("20260505", "100000", "0", "310000", "5425000", "KB카드 결제"));
        t.add(bankTransaction("20260510", "120000", "0", "300000", "5125000", "여행 월렛 저축"));
        t.add(bankTransaction("20260525", "183000", "0", "85000", "5040000", "통신비 자동이체"));
        // 6월
        t.add(bankTransaction("20260601", "090000", "3200000", "0", "8240000", "6월 급여"));
        t.add(bankTransaction("20260605", "100000", "0", "295000", "7945000", "KB카드 결제"));
        t.add(bankTransaction("20260610", "120000", "0", "350000", "7595000", "여행 월렛 저축"));
        t.add(bankTransaction("20260625", "183000", "0", "85000", "7510000", "통신비 자동이체"));
        // 7월
        t.add(bankTransaction("20260701", "090000", "3200000", "0", "10710000", "7월 급여"));
        t.add(bankTransaction("20260705", "100000", "0", "330000", "10380000", "KB카드 결제"));
        t.add(bankTransaction("20260710", "120000", "0", "350000", "10030000", "여행 월렛 저축"));
        t.add(bankTransaction("20260715", "140000", "0", "1200000", "8830000", "항공권 결제"));
        t.add(bankTransaction("20260725", "183000", "0", "85000", "8745000", "통신비 자동이체"));
        // 8월 (출발 전)
        t.add(bankTransaction("20260801", "090000", "3200000", "0", "11945000", "8월 급여"));
        t.add(bankTransaction("20260805", "100000", "0", "225000", "11720000", "KB카드 결제"));
        t.add(bankTransaction("20260810", "120000", "0", "400000", "11320000", "여행 월렛 저축"));
        t.add(bankTransaction("20260814", "150000", "0", "2800000", "8520000", "트래블카드 환전 충전"));
        return List.copyOf(t);
    }

    private static List<Map<String, Object>> buildTravelUserCardTransactions() {
        List<Map<String, Object>> t = new ArrayList<>();
        int[] days = {3, 8, 14, 20, 26};

        // 4~7월 국내 소비 (저축 기간)
        for (int month = 4; month <= 7; month++) {
            String ym = "2026" + String.format("%02d", month);
            int adj = (month - 4) * 300;
            addRegularCategoryTransactions(t, ym, "FOOD", "일반음식점",
                    new String[]{"맛있는집", "한솥도시락", "김밥천국"}, days, 9000 + adj, 800);
            addRegularCategoryTransactions(t, ym, "CAFE", "커피전문점",
                    new String[]{"스타벅스", "이디야", "투썸"}, days, 5500 + adj, 400);
            addRegularCategoryTransactions(t, ym, "SHOPPING", "일반의류",
                    new String[]{"유니클로", "자라", "올리브영"}, days, 12000 + adj, 1200);
            addRegularCategoryTransactions(t, ym, "LIVING", "편의점",
                    new String[]{"CU편의점", "GS25", "세븐일레븐"}, days, 8000 + adj, 500);
            addRegularCategoryTransactions(t, ym, "TRANSPORT", "택시",
                    new String[]{"카카오택시", "서울지하철", "버스"}, days, 7000 + adj, 400);
        }

        // 8월 출발 전 (1~15일)
        int[] preTripDays = {2, 5, 10, 13};
        addCategoryTransactions(t, "202608", "FOOD", "일반음식점",
                new String[]{"맛있는집", "한솥도시락", "김밥천국"}, preTripDays,
                new int[]{8500, 9200, 11000, 7800});
        addCategoryTransactions(t, "202608", "CAFE", "커피전문점",
                new String[]{"스타벅스", "이디야"}, preTripDays,
                new int[]{5500, 4800, 6200, 5000});
        addCategoryTransactions(t, "202608", "LIVING", "편의점",
                new String[]{"CU편의점", "GS25"}, preTripDays,
                new int[]{3200, 4500, 2800, 5100});

        return List.copyOf(t);
    }

    // ===================================================================
    // demofresh 거래 데이터 (28세 직장인, 2026년 1~8월)
    // 급여 실수령 295만원, 월세·통신비·적금 자동이체 + 체크카드 일상 소비.
    // ===================================================================

    private static List<Map<String, Object>> buildFreshBankTransactions() {
        List<Map<String, Object>> t = new ArrayList<>();
        int[] cardBill = {780000, 810000, 895000, 860000, 920000, 875000, 1050000, 990000};
        long balance = 1_200_000L;
        for (int month = 1; month <= 8; month++) {
            String ym = "2026" + String.format("%02d", month);

            balance += 2_950_000L;
            t.add(bankTransaction(ym + "01", "090000", "2950000", "0", String.valueOf(balance), month + "월 급여"));

            balance -= cardBill[month - 1];
            t.add(bankTransaction(ym + "05", "081500", "0", String.valueOf(cardBill[month - 1]), String.valueOf(balance), "KB카드 결제"));

            balance -= 550_000L;
            t.add(bankTransaction(ym + "10", "140000", "0", "550000", String.valueOf(balance), "월세·관리비 자동이체"));

            balance -= 62_000L;
            t.add(bankTransaction(ym + "15", "093000", "0", "62000", String.valueOf(balance), "통신비 자동이체"));

            // 8월은 오늘(8/21) 이전까지만 발생한 거래로 제한한다.
            if (month < 8) {
                balance -= 300_000L;
                t.add(bankTransaction(ym + "25", "100000", "0", "300000", String.valueOf(balance), "정기적금 자동이체"));
            }
        }
        return List.copyOf(t);
    }

    private static List<Map<String, Object>> buildFreshCardTransactions() {
        List<Map<String, Object>> t = new ArrayList<>();

        for (int month = 1; month <= 8; month++) {
            String ym = "2026" + String.format("%02d", month);
            boolean isCurrentMonth = month == 8; // 오늘(8/21) 이전까지만 생성
            int adj = (month - 1) * 250;

            int[] foodDays = isCurrentMonth ? new int[]{3, 8, 13, 19} : new int[]{3, 8, 13, 19, 26};
            addRegularCategoryTransactions(t, ym, "FOOD", "일반음식점",
                    new String[]{"한솥도시락", "김밥천국", "정성반상"}, foodDays, 9500 + adj, 800);

            int[] cafeDays = isCurrentMonth ? new int[]{2, 9, 15, 21} : new int[]{2, 9, 15, 21, 27};
            addRegularCategoryTransactions(t, ym, "CAFE", "커피전문점",
                    new String[]{"스타벅스", "이디야", "메가커피"}, cafeDays, 5200 + adj, 400);

            int[] livingDays = isCurrentMonth ? new int[]{4, 11, 17} : new int[]{4, 11, 17, 23, 29};
            addRegularCategoryTransactions(t, ym, "LIVING", "편의점",
                    new String[]{"CU편의점", "GS25", "세븐일레븐"}, livingDays, 6500 + adj, 400);

            int[] transportDays = isCurrentMonth ? new int[]{6, 14, 20} : new int[]{6, 14, 20, 25};
            addRegularCategoryTransactions(t, ym, "TRANSPORT", "대중교통",
                    new String[]{"서울교통공사", "카카오택시"}, transportDays, 8000 + adj, 500);

            int[] shoppingDays = isCurrentMonth ? new int[]{10} : new int[]{10, 24};
            addRegularCategoryTransactions(t, ym, "SHOPPING", "온라인쇼핑",
                    new String[]{"쿠팡", "무신사", "올리브영"}, shoppingDays, 35000 + adj, 5000);

            int[] deliveryDays = isCurrentMonth ? new int[]{7, 16} : new int[]{7, 16, 28};
            addRegularCategoryTransactions(t, ym, "DELIVERY", "배달서비스",
                    new String[]{"배달의민족", "쿠팡이츠"}, deliveryDays, 18000 + adj, 1500);

            t.add(cardTransaction(ym + "01", "080000", "17000", "FR" + ym.substring(2) + "SUB1", "넷플릭스", "온라인서비스"));
            t.add(cardTransaction(ym + "01", "080500", "14900", "FR" + ym.substring(2) + "SUB2", "유튜브 프리미엄", "온라인서비스"));
            t.add(cardTransaction(ym + "02", "190000", "89000", "FR" + ym.substring(2) + "GYM", "스포애니 피트니스", "스포츠시설"));
        }

        return List.copyOf(t);
    }

    private static List<Map<String, Object>> buildFreshTravelCardTransactions() {
        return List.of();
    }

    /**
     * 여행 중 해외 트래블카드 거래.
     * 프랑스 파리 (8/16~8/19) → 독일 뮌헨 (8/20~8/22) → 스위스 취리히 (8/23~8/25)
     */
    private static List<Map<String, Object>> buildTravelUserTravelCardTransactions() {
        List<Map<String, Object>> t = new ArrayList<>();

        // === 프랑스 파리 (8/16 ~ 8/19) ===
        t.add(cardTransaction("20260816", "143000", "48000", "TV0801", "TAXI PARISIEN", "해외교통"));
        t.add(cardTransaction("20260816", "190000", "35000", "TV0802", "LE PETIT BISTRO", "해외음식점"));
        t.add(cardTransaction("20260816", "210000", "22000", "TV0803", "GALERIES LAFAYETTE", "해외쇼핑"));

        t.add(cardTransaction("20260817", "083000", "32000", "TV0804", "CAFE DE FLORE", "해외카페"));
        t.add(cardTransaction("20260817", "110000", "18000", "TV0805", "METRO PARIS RATP", "해외교통"));
        t.add(cardTransaction("20260817", "140000", "55000", "TV0806", "LE BON MARCHE", "해외쇼핑"));
        t.add(cardTransaction("20260817", "200000", "42000", "TV0808", "BRASSERIE LIPP", "해외음식점"));

        t.add(cardTransaction("20260818", "090000", "25000", "TV0809", "MUSEE DU LOUVRE", "해외관광"));
        t.add(cardTransaction("20260818", "130000", "28000", "TV0810", "BOULANGERIE PAIN", "해외음식점"));
        t.add(cardTransaction("20260818", "160000", "38000", "TV0811", "PRINTEMPS PARIS", "해외쇼핑"));
        t.add(cardTransaction("20260818", "193000", "45000", "TV0812", "LE COMPTOIR PARIS", "해외음식점"));

        t.add(cardTransaction("20260819", "080000", "15000", "TV0813", "UBER PARIS CDG", "해외교통"));
        t.add(cardTransaction("20260819", "100000", "30000", "TV0814", "DUTY FREE CDG", "해외쇼핑"));
        t.add(cardTransaction("20260819", "120000", "18000", "TV0815", "CDG AIRPORT CAFE", "해외음식점"));

        // === 독일 뮌헨 (8/20 ~ 8/22) ===
        t.add(cardTransaction("20260820", "150000", "12000", "TV0816", "DB BAHN TICKET", "해외교통"));
        t.add(cardTransaction("20260820", "180000", "25000", "TV0817", "HOFBRAUHAUS MUNCHEN", "해외음식점"));
        t.add(cardTransaction("20260820", "200000", "32000", "TV0818", "KAUFHOF MARIENPLATZ", "해외쇼핑"));
        t.add(cardTransaction("20260820", "220000", "9500", "TV0819", "CAFE LUITPOLD", "해외카페"));

        t.add(cardTransaction("20260821", "090000", "20000", "TV0820", "SCHLOSS NYMPHENBURG", "해외관광"));
        t.add(cardTransaction("20260821", "130000", "18000", "TV0821", "AUGUSTINER KELLER", "해외음식점"));
        t.add(cardTransaction("20260821", "170000", "42000", "TV0822", "MAXIMILIANSTRASSE", "해외쇼핑"));

        // === 스위스 취리히 (8/23 ~ 8/25, 아직 미래) ===

        return List.copyOf(t);
    }

    // ===================================================================
    // 공통 유틸리티
    // ===================================================================

    private static void addRegularCategoryTransactions(
            List<Map<String, Object>> transactions,
            String yearMonth,
            String categoryCode,
            String merchantType,
            String[] merchants,
            int[] days,
            int baseAmount,
            int step
    ) {
        int[] amounts = new int[days.length];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = baseAmount + (i % 3) * step;
        }
        addCategoryTransactions(transactions, yearMonth, categoryCode, merchantType, merchants, days, amounts);
    }

    private static void addCategoryTransactions(
            List<Map<String, Object>> transactions,
            String yearMonth,
            String categoryCode,
            String merchantType,
            String[] merchants,
            int[] days,
            int[] amounts
    ) {
        if (days.length != amounts.length) {
            throw new IllegalArgumentException("Mock 거래 일자와 금액 개수가 일치해야 합니다.");
        }
        for (int i = 0; i < days.length; i++) {
            String date = yearMonth + String.format("%02d", days[i]);
            String time = String.format("%02d%02d00", 9 + (i * 2) % 12, (i * 11) % 60);
            String approvalNo = "QA" + yearMonth.substring(2) + categoryCode + String.format("%02d", i + 1);
            transactions.add(cardTransaction(
                    date,
                    time,
                    String.valueOf(amounts[i]),
                    approvalNo,
                    merchants[i % merchants.length],
                    merchantType
            ));
        }
    }

    private static Map<String, Object> bankTransaction(
            String date, String time, String deposit, String withdrawal, String balance, String description
    ) {
        return mapOf(
                "resAccountTrDate", date,
                "resAccountTrTime", time,
                "resAccountIn", deposit,
                "resAccountOut", withdrawal,
                "resAfterTranBalance", balance,
                "resAccountDesc1", description,
                "resAccountDesc2", description
        );
    }

    private static Map<String, Object> cardTransaction(
            String date, String time, String amount, String approvalNo, String merchant, String merchantType
    ) {
        return mapOf(
                "resUsedDate", date,
                "resUsedTime", time,
                "resUsedAmount", amount,
                "resCancelYN", "N",
                "resApprovalNo", approvalNo,
                "resMemberStoreName", merchant,
                "resMemberStoreType", merchantType
        );
    }

    private static Map<String, Object> success(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", mapOf("code", "CF-00000", "message", "성공"));
        response.put("data", data);
        return response;
    }

    private static Map<String, Object> failure(String code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", mapOf("code", code, "message", message));
        response.put("data", Map.of());
        return response;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadJsonTransactions(String... resourcePaths) {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> merged = new ArrayList<>();
        for (String path : resourcePaths) {
            try (InputStream is = MockCodefClient.class.getResourceAsStream(path)) {
                if (is == null) {
                    throw new RuntimeException("Mock JSON not found: " + path);
                }
                List<Map<String, Object>> items = mapper.readValue(is,
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                merged.addAll(items);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load mock JSON: " + path, e);
            }
        }
        merged.sort((a, b) -> {
            String dateA = String.valueOf(a.values().iterator().next());
            String dateB = String.valueOf(b.values().iterator().next());
            return dateA.compareTo(dateB);
        });
        return List.copyOf(merged);
    }

    private static List<Map<String, Object>> loadYuhyunBankTransactions() {
        return loadJsonTransactions(
                "/mock/demo-bank-transactions.json",
                "/mock/demo-virtual-bank-transactions.json"
        );
    }

    private static List<Map<String, Object>> loadYuhyunNoriCardTransactions() {
        return loadJsonTransactions(
                "/mock/demo-card-transactions.json",
                "/mock/demo-virtual-card-transactions.json"
        );
    }

    private static List<Map<String, Object>> loadYuhyunTravelCardTransactions() {
        return loadJsonTransactions("/mock/demo-travel-card-transactions.json");
    }

    private static Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
