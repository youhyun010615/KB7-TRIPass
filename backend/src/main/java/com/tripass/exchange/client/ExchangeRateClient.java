package com.tripass.exchange.client;

import com.tripass.exchange.dto.ExternalExchangeRateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateClient {

    private final RestTemplate restTemplate;

    @Value("${exchange.api-key}")
    private String apiKey;

    private static final String API_URL = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON";

    public List<ExternalExchangeRateDto> fetchExchangeRates(String searchDate) {
        URI uri = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("authkey", apiKey)
                .queryParam("searchdate", searchDate)
                .queryParam("data", "AP01")
                .build()
                .toUri();

        log.info("Fetching exchange rates for date: {} from URL: {}", searchDate, uri);

        try {
            ExternalExchangeRateDto[] response = restTemplate.getForObject(uri, ExternalExchangeRateDto[].class);
            if (response != null && response.length > 0) {
                if (response[0].getResult() != 1) {
                    log.error("API 응답 오류 (Result Code: {})", response[0].getResult());
                    return Collections.emptyList();
                }
                return Arrays.asList(response);
            }
        } catch (Exception e) {
            log.error("환율 API 호출 중 예외 발생: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }
}
