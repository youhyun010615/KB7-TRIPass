package com.tripass.exchange;

import com.tripass.common.config.RootConfig;
import com.tripass.exchange.client.ExchangeRateClient;
import com.tripass.exchange.dto.ExternalExchangeRateDto;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
public class ExchangeApiConnectionTest {

    @Autowired
    private ExchangeRateClient exchangeRateClient;

    @Test
    @DisplayName("수출입은행 환율 API 실제 호출 테스트")
    void testExchangeApiFetch() {
        // 평일 날짜로 테스트 (2024년 3월 20일 수요일)
        String testDate = "20240320";

        List<ExternalExchangeRateDto> results = exchangeRateClient.fetchExchangeRates(testDate);

        log.info("조회 결과 개수: {}", results.size());

        if (!results.isEmpty()) {
            results.stream().limit(5).forEach(dto -> log.info("통화 정보: {}", dto));
        }

        // 1. 리스트가 비어있지 않은지 검증 (AssertJ의 isNotEmpty() 대체)
        assertFalse(results.isEmpty(), "결과 리스트가 비어있지 않아야 합니다.");

        // 2. API 응답 결과 코드(result)가 1인지 검증 (AssertJ의 isEqualTo(1) 대체)
        // assertEquals(기대값, 실제값, 에러메시지)
        assertEquals(1, results.get(0).getResult(), "API 응답 결과 코드가 1이어야 합니다.");
    }
}