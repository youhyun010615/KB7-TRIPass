package com.tripass.asset.service.codef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockCodefClientTest {

    private MockCodefClient client;
    private String token;

    @BeforeEach
    void setUp() {
        client = new MockCodefClient();
        token = client.getAccessToken();
    }

    @Test
    void 올바른_목계정으로_CODEF_연결아이디를_발급한다() {
        Map<String, Object> result = client.callApi(
                token,
                "/v1/account/create",
                connectionBody(MockCodefClient.LOGIN_ID, MockCodefClient.PASSWORD,
                        MockCodefClient.BANK_ORGANIZATION, "BK")
        );

        assertSuccess(result);
        assertEquals(MockCodefClient.CONNECTED_ID, data(result).get("connectedId"));
    }

    @Test
    void 잘못된_비밀번호는_실제연동처럼_실패응답을_반환한다() {
        Map<String, Object> result = client.callApi(
                token,
                "/v1/account/create",
                connectionBody(MockCodefClient.LOGIN_ID, "wrong-password",
                        MockCodefClient.BANK_ORGANIZATION, "BK")
        );

        assertEquals("CF-01002", responseResult(result).get("code"));
    }

    @Test
    void 계좌와_선택기간의_거래내역을_CODEF_응답형태로_반환한다() {
        Map<String, Object> accounts = client.callApi(
                token,
                "/v1/kr/bank/p/account/account-list",
                connectedBody(MockCodefClient.BANK_ORGANIZATION)
        );
        assertSuccess(accounts);
        List<?> accountList = (List<?>) data(accounts).get("resDepositTrust");
        assertEquals(2, accountList.size());

        Map<String, Object> body = connectedBody(MockCodefClient.BANK_ORGANIZATION);
        body.put("account", "12345678901234");
        body.put("startDate", "20260701");
        body.put("endDate", "20260731");
        Map<String, Object> transactions = client.callApi(
                token,
                "/v1/kr/bank/p/account/transaction-list",
                body
        );
        assertSuccess(transactions);
        assertEquals(4, ((List<?>) data(transactions).get("resTrHistoryList")).size());
    }

    @Test
    void 일반카드와_트래블카드_및_각각의_거래내역을_반환한다() {
        Map<String, Object> cards = client.callApi(
                token,
                "/v1/kr/card/p/account/card-list",
                connectedBody(MockCodefClient.CARD_ORGANIZATION)
        );
        assertSuccess(cards);
        List<?> cardList = (List<?>) cards.get("data");
        assertEquals(2, cardList.size());
        assertTrue(cardList.stream().map(String::valueOf).anyMatch(value -> value.contains("트래블러스")));

        Map<String, Object> body = connectedBody(MockCodefClient.CARD_ORGANIZATION);
        body.put("cardNo", "5412-****-****-2710");
        body.put("startDate", "20260701");
        body.put("endDate", "20260831");
        Map<String, Object> approvals = client.callApi(
                token,
                "/v1/kr/card/p/account/approval-list",
                body
        );
        assertSuccess(approvals);
        List<?> approvalList = (List<?>) approvals.get("data");
        assertFalse(approvalList.isEmpty());
        assertEquals(69, approvalList.size());
    }

    @Test
    void 월간분석과_미션QA에_필요한_비교월과_분석월_거래를_반환한다() {
        Map<String, Object> comparisonBody = connectedBody(MockCodefClient.CARD_ORGANIZATION);
        comparisonBody.put("cardNo", "5412-****-****-2710");
        comparisonBody.put("startDate", "20260401");
        comparisonBody.put("endDate", "20260630");

        Map<String, Object> comparisonResponse = client.callApi(
                token,
                "/v1/kr/card/p/account/approval-list",
                comparisonBody
        );
        assertSuccess(comparisonResponse);
        assertEquals(90, ((List<?>) comparisonResponse.get("data")).size());

        Map<String, Object> analysisBody = connectedBody(MockCodefClient.CARD_ORGANIZATION);
        analysisBody.put("cardNo", "5412-****-****-2710");
        analysisBody.put("startDate", "20260701");
        analysisBody.put("endDate", "20260731");

        Map<String, Object> analysisResponse = client.callApi(
                token,
                "/v1/kr/card/p/account/approval-list",
                analysisBody
        );
        assertSuccess(analysisResponse);
        List<?> analysisTransactions = (List<?>) analysisResponse.get("data");
        assertEquals(39, analysisTransactions.size());
        assertEquals(6, countMerchantType(analysisTransactions, "일반음식점"));
        assertEquals(6, countMerchantType(analysisTransactions, "커피전문점"));
        assertEquals(6, countMerchantType(analysisTransactions, "일반의류"));
        assertEquals(6, countMerchantType(analysisTransactions, "편의점"));
        assertEquals(6, countMerchantType(analysisTransactions, "택시"));
        assertEquals(6, countMerchantType(analysisTransactions, "영화공연장"));
    }

    private Map<String, Object> connectionBody(
            String id, String password, String organization, String businessType
    ) {
        return new java.util.HashMap<>(Map.of(
                "accountList", List.of(Map.of(
                        "id", id,
                        "password", password,
                        "organization", organization,
                        "businessType", businessType
                ))
        ));
    }

    private Map<String, Object> connectedBody(String organization) {
        return new java.util.HashMap<>(Map.of(
                "connectedId", MockCodefClient.CONNECTED_ID,
                "organization", organization
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> responseResult(Map<String, Object> response) {
        return (Map<String, Object>) response.get("result");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> response) {
        return (Map<String, Object>) response.get("data");
    }

    private void assertSuccess(Map<String, Object> response) {
        assertEquals("CF-00000", responseResult(response).get("code"));
    }

    private long countMerchantType(List<?> transactions, String merchantType) {
        return transactions.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(transaction -> merchantType.equals(transaction.get("resMemberStoreType")))
                .count();
    }
}
