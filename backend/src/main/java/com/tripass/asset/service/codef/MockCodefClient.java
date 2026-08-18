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

    private static final List<Map<String, Object>> BANK_TRANSACTIONS = buildBankTransactions();

    /**
     * 2026년 4~6월 비교 데이터, 7월 분석 데이터, 8월 미션 진행 데이터를 모두 포함한다.
     * 사용자가 Mock 카드를 연결한 뒤 승인내역을 동기화해야 서비스 DB에 적재된다.
     */
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
                        "resAccountBalance", "14379300",
                        "resWithdrawableAmount", "14379300"
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

        // 8월에는 주차별 금액을 다르게 두어 미션 성공·실패·진행 중 상태를 함께 검증한다.
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

    private static Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
