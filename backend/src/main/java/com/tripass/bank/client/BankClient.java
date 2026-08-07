package com.tripass.bank.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
public class BankClient {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public BankClient(RestTemplate restTemplate, @Value("${kakao.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    private static final String API_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    // 키워드 검색 (기존)
    public Map<String, Object> fetchBankBranches(String keyword, int page) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + apiKey.trim());

        URI uri = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("query", keyword)
                .queryParam("category_group_code", "BK9")
                .queryParam("page", page)
                .queryParam("size", 15)
                .encode()
                .build()
                .toUri();

        log.info("Fetching bank branches for keyword: {} from URL: {}", keyword, uri);

        return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
    }

    // 좌표 반경 검색 (추가)
    public Map<String, Object> fetchBankBranchesByLocation(double lat, double lon, int radius, int page) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + apiKey.trim());

        URI uri = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("query", "국민은행")
                .queryParam("category_group_code", "BK9")
                .queryParam("x", lon)
                .queryParam("y", lat)
                .queryParam("radius", radius)
                .queryParam("page", page)
                .queryParam("size", 15)
                .encode()
                .build()
                .toUri();

        log.info("Fetching bank branches by location: ({}, {}) radius: {}m, URL: {}", lat, lon, radius, uri);

        try {
            return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
        } catch (Exception e) {
            log.error("Kakao API 호출 중 예외 발생: {}", e.getMessage(), e);
            return null;
        }
    }
}
