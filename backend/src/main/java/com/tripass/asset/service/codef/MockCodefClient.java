package com.tripass.asset.service.codef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 로컬 QA 전용 가상 금융기관.
 *
 * <p>서비스 DB에 계좌/카드를 미리 넣지 않고 실제 CODEF 형태의 응답만 제공한다.
 * 사용자가 기존 연동 화면에서 아래 자격증명을 입력한 뒤에야 AssetService가 데이터를 저장한다.</p>
 */
public class MockCodefClient implements CodefClient {

    public static final String LOGIN_ID = "tripassqa";
    public static final String PASSWORD = "Mock1234!";
    public static final String CONNECTED_ID = "MOCK-CONNECTED-TRIPASS-QA";
    public static final String BANK_ORGANIZATION = "0004";
    public static final String CARD_ORGANIZATION = "0301";

    private static final String ACCESS_TOKEN = "mock-codef-access-token";

    private static final List<Map<String, Object>> BANK_TRANSACTIONS = List.of(
            bankTransaction("20260701", "090000", "3500000", "0", "3500000", "7월 급여"),
            bankTransaction("20260703", "081500", "0", "65000", "3435000", "KB카드 결제"),
            bankTransaction("20260710", "120000", "0", "150000", "3285000", "여행 월렛 저축"),
            bankTransaction("20260725", "183000", "0", "89000", "3196000", "통신비 자동이체"),
            bankTransaction("20260801", "090000", "3500000", "0", "6696000", "8월 급여"),
            bankTransaction("20260805", "081500", "0", "412700", "6283300", "KB카드 결제"),
            bankTransaction("20260812", "121000", "0", "200000", "6083300", "여행 월렛 저축")
    );

    private static final List<Map<String, Object>> GENERAL_CARD_TRANSACTIONS = List.of(
            cardTransaction("20260702", "081500", "5500", "QA1001", "메가커피 강남점", "커피전문점"),
            cardTransaction("20260705", "194000", "32000", "QA1002", "오늘의식탁", "일반음식점"),
            cardTransaction("20260708", "202000", "58000", "QA1003", "라이프마트", "대형마트"),
            cardTransaction("20260712", "133000", "14500", "QA1004", "교보문고", "서점"),
            cardTransaction("20260716", "074000", "1500", "QA1005", "서울교통공사", "대중교통"),
            cardTransaction("20260721", "211000", "12900", "QA1006", "넷플릭스", "온라인서비스"),
            cardTransaction("20260803", "122000", "11000", "QA1007", "오늘의식탁", "일반음식점"),
            cardTransaction("20260807", "091000", "4800", "QA1008", "카페모먼트", "커피전문점"),
            cardTransaction("20260811", "181500", "74000", "QA1009", "올리브영", "생활용품")
    );

    private static final List<Map<String, Object>> TRAVEL_CARD_TRANSACTIONS = List.of(
            cardTransaction("20260718", "101000", "42000", "QT2001", "대한항공", "항공사"),
            cardTransaction("20260719", "143000", "185000", "QT2002", "호텔스닷컴", "숙박"),
            cardTransaction("20260809", "160000", "36000", "QT2003", "JR EAST", "해외교통"),
            cardTransaction("20260810", "193000", "52000", "QT2004", "SUSHI TOKYO", "해외음식점")
    );

    @Override
    public String encodePassword(String plainPassword) {
        // Mock 구현에서는 요청 검증을 위해 평문을 내부 메모리에서만 사용한다.
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

    private Map<String, Object> connect(Map<String, Object> body) {
        Object rawAccounts = body.get("accountList");
        if (!(rawAccounts instanceof List<?> accounts) || accounts.isEmpty()
                || !(accounts.get(0) instanceof Map<?, ?> account)) {
            return failure("CF-01002", "연동 계정 정보가 없습니다.");
        }
        if (!LOGIN_ID.equals(String.valueOf(account.get("id")))
                || !PASSWORD.equals(String.valueOf(account.get("password")))) {
            return failure("CF-01002", "Mock 금융기관 아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        String organization = String.valueOf(account.get("organization"));
        if (!BANK_ORGANIZATION.equals(organization) && !CARD_ORGANIZATION.equals(organization)) {
            return failure("CF-01003", "Mock에서 지원하지 않는 금융기관입니다.");
        }
        return success(Map.of("connectedId", CONNECTED_ID));
    }

    private Map<String, Object> bankAccounts(Map<String, Object> body) {
        if (!isConnected(body) || !BANK_ORGANIZATION.equals(String.valueOf(body.get("organization")))) {
            return failure("CF-01004", "연동되지 않은 Mock 은행입니다.");
        }
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
                        "resAccountBalance", "6083300",
                        "resWithdrawableAmount", "6083300"
                )
        ));
        return success(data);
    }

    private Map<String, Object> cards(Map<String, Object> body) {
        if (!isConnected(body) || !CARD_ORGANIZATION.equals(String.valueOf(body.get("organization")))) {
            return failure("CF-01004", "연동되지 않은 Mock 카드사입니다.");
        }
        return success(List.of(
                mapOf(
                        "resCardName", "KB QA 체크카드",
                        "resCardNo", "5412-****-****-2710",
                        "resCardType", "02"
                ),
                mapOf(
                        "resCardName", "트래블러스 체크카드",
                        "resCardNo", "5412-****-****-2711",
                        "resCardType", "02"
                )
        ));
    }

    private Map<String, Object> bankTransactions(Map<String, Object> body) {
        if (!isConnected(body) || !BANK_ORGANIZATION.equals(String.valueOf(body.get("organization")))) {
            return failure("CF-01004", "연동되지 않은 Mock 은행입니다.");
        }
        String account = String.valueOf(body.get("account"));
        List<Map<String, Object>> transactions = "12345678901234".equals(account)
                ? filterByDate(BANK_TRANSACTIONS, body, "resAccountTrDate")
                : List.of();
        return success(Map.of("resTrHistoryList", transactions));
    }

    private Map<String, Object> cardTransactions(Map<String, Object> body) {
        if (!isConnected(body) || !CARD_ORGANIZATION.equals(String.valueOf(body.get("organization")))) {
            return failure("CF-01004", "연동되지 않은 Mock 카드사입니다.");
        }
        String cardNo = String.valueOf(body.get("cardNo"));
        List<Map<String, Object>> source = switch (cardNo) {
            case "5412-****-****-2710" -> GENERAL_CARD_TRANSACTIONS;
            case "5412-****-****-2711" -> TRAVEL_CARD_TRANSACTIONS;
            default -> List.of();
        };
        return success(filterByDate(source, body, "resUsedDate"));
    }

    private boolean isConnected(Map<String, Object> body) {
        return CONNECTED_ID.equals(String.valueOf(body.get("connectedId")));
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

    private static Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
