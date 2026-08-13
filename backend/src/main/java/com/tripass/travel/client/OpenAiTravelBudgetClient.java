package com.tripass.travel.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripass.travel.dto.AiBudgetResultDto;
import com.tripass.travel.dto.TripCountryBudgetContextDto;
import com.tripass.travel.exception.TravelErrorCode;
import com.tripass.travel.exception.TravelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiTravelBudgetClient implements TravelBudgetAiClient {

    private static final BigDecimal MAX_BUDGET_AMOUNT = new BigDecimal("100000000");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String chatCompletionsUrl;

    public OpenAiTravelBudgetClient(
            RestTemplate restTemplate,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.chat-completions-url:https://api.openai.com/v1/chat/completions}") String chatCompletionsUrl
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
        this.chatCompletionsUrl = chatCompletionsUrl;
    }

    @Override
    public AiBudgetResultDto recommend(TripCountryBudgetContextDto context, long tripDays) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API 키가 설정되지 않았습니다.");
            throw new TravelException(TravelErrorCode.AI_BUDGET_UNAVAILABLE,
                    "AI 여행 예산 추천 키가 설정되지 않았습니다.");
        }

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    chatCompletionsUrl,
                    new HttpEntity<>(buildRequest(context, tripDays), buildHeaders()),
                    Map.class
            );
            return parseRecommendation(response.getBody());
        } catch (TravelException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException exception) {
            log.error("OpenAI 여행 예산 추천 호출 실패: country={}, message={}",
                    context.getCountryName(), exception.getMessage());
            throw new TravelException(TravelErrorCode.AI_BUDGET_UNAVAILABLE);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> buildRequest(TripCountryBudgetContextDto context, long tripDays) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("temperature", 0.2);
        request.put("response_format", Map.of("type", "json_object"));
        request.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 한국인 1인 자유여행 예산을 제안하는 금융 여행 플래너입니다. 숫자는 모두 KRW 정수로 제시하세요. 반드시 JSON 객체만 반환하세요."),
                Map.of("role", "user", "content", """
                        다음 여행의 중간 수준(MID_RANGE) 성인 1인 예산을 제안하세요.
                        국가: %s
                        현지 통화: %s
                        일정: %s ~ %s (%d일)

                        반환 형식:
                        {
                          "airfareAmount": 0,
                          "lodgingAmount": 0,
                          "activityAmount": 0,
                          "foodAmount": 0,
                          "otherAmount": 0,
                          "reason": "추천 근거를 2문장 이내 한국어로 작성"
                        }

                        airfareAmount와 lodgingAmount는 사전지출이고,
                        activityAmount, foodAmount, otherAmount만 현지 여행 목표 자금에 포함됩니다.
                        음수, 소수, 설명용 문자열 금액은 허용되지 않습니다.
                        """.formatted(
                        context.getCountryName(),
                        context.getCurrencyCode(),
                        context.getArrivalDate(),
                        context.getDepartureDate(),
                        tripDays
                ))
        ));
        return request;
    }

    private AiBudgetResultDto parseRecommendation(Map<String, Object> response) {
        try {
            JsonNode root = objectMapper.valueToTree(response);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new IllegalArgumentException("choices가 없습니다.");
            }
            JsonNode node = objectMapper.readTree(contentNode.asText());

            AiBudgetResultDto result = AiBudgetResultDto.builder()
                    .airfareAmount(readAmount(node, "airfareAmount"))
                    .lodgingAmount(readAmount(node, "lodgingAmount"))
                    .activityAmount(readAmount(node, "activityAmount"))
                    .foodAmount(readAmount(node, "foodAmount"))
                    .otherAmount(readAmount(node, "otherAmount"))
                    .reason(readReason(node))
                    .model(model)
                    .build();
            validateAmounts(result);
            return result;
        } catch (Exception exception) {
            log.error("OpenAI 여행 예산 추천 응답 파싱 실패", exception);
            throw new TravelException(TravelErrorCode.AI_BUDGET_UNAVAILABLE,
                    "AI 여행 예산 추천 결과 형식이 올바르지 않습니다.");
        }
    }

    private BigDecimal readAmount(JsonNode node, String fieldName) {
        if (!node.hasNonNull(fieldName) || !node.get(fieldName).isIntegralNumber()) {
            throw new IllegalArgumentException(fieldName + " 값이 올바르지 않습니다.");
        }
        return node.get(fieldName).decimalValue();
    }

    private String readReason(JsonNode node) {
        if (!node.hasNonNull("reason")) {
            return "여행 기간과 국가별 일반적인 소비 수준을 기준으로 추천했습니다.";
        }
        return node.get("reason").asText().trim();
    }

    private void validateAmounts(AiBudgetResultDto result) {
        List<BigDecimal> amounts = List.of(
                result.getAirfareAmount(), result.getLodgingAmount(), result.getActivityAmount(),
                result.getFoodAmount(), result.getOtherAmount()
        );
        if (amounts.stream().anyMatch(amount -> amount.signum() < 0 || amount.compareTo(MAX_BUDGET_AMOUNT) > 0)) {
            throw new TravelException(TravelErrorCode.INVALID_BUDGET_AMOUNT);
        }
    }
}
