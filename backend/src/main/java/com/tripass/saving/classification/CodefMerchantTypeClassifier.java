package com.tripass.saving.classification;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import static com.tripass.saving.classification.ConsumptionCategoryCode.CAFE;
import static com.tripass.saving.classification.ConsumptionCategoryCode.FOOD;
import static com.tripass.saving.classification.ConsumptionCategoryCode.LEISURE;
import static com.tripass.saving.classification.ConsumptionCategoryCode.LIVING;
import static com.tripass.saving.classification.ConsumptionCategoryCode.LODGING;
import static com.tripass.saving.classification.ConsumptionCategoryCode.OTHER;
import static com.tripass.saving.classification.ConsumptionCategoryCode.SHOPPING;
import static com.tripass.saving.classification.ConsumptionCategoryCode.SIGHTSEEING;
import static com.tripass.saving.classification.ConsumptionCategoryCode.TRANSPORT;

@Component
public class CodefMerchantTypeClassifier {

    private static final Map<String, ConsumptionCategoryCode> TYPE_MAPPING =
            Map.ofEntries(
                    // 식비
                    Map.entry("일반음식점", FOOD),
                    Map.entry("일식전문점", FOOD),
                    Map.entry("기타음식점", FOOD),
                    Map.entry("패스트푸드점", FOOD),
                    Map.entry("서양식전문점", FOOD),
                    Map.entry("기타식음료품", FOOD),
                    Map.entry("일반주점", FOOD),
                    Map.entry("축산물정육점", FOOD),
                    Map.entry("치킨전문점", FOOD),

                    // 카페
                    Map.entry("커피전문점", CAFE),
                    Map.entry("제과제빵", CAFE),
                    Map.entry("아이스크림전문", CAFE),

                    // 쇼핑
                    Map.entry("체인스토어", SHOPPING),
                    Map.entry("온라인상품권", SHOPPING),
                    Map.entry("일반상품권", SHOPPING),
                    Map.entry("화장품", SHOPPING),
                    Map.entry("기타잡화", SHOPPING),
                    Map.entry("문구사무용품", SHOPPING),
                    Map.entry("기타상설할인", SHOPPING),
                    Map.entry("일반의류", SHOPPING),
                    Map.entry("백화점", SHOPPING),

                    // 생활비
                    Map.entry("편의점", LIVING),
                    Map.entry("공과금", LIVING),
                    Map.entry("약국", LIVING),
                    Map.entry("내과", LIVING),
                    Map.entry("한의원", LIVING),
                    Map.entry("안과", LIVING),
                    Map.entry("안경광학제품", LIVING),
                    Map.entry("피부체형미관리", LIVING),
                    Map.entry("미용실두발전문", LIVING),
                    Map.entry("성형외과", LIVING),
                    Map.entry("피부과", LIVING),
                    Map.entry("슈퍼마켓마트", LIVING),

                    // 교통비
                    Map.entry("택시", TRANSPORT),
                    Map.entry("시내버스", TRANSPORT),
                    Map.entry("지하철", TRANSPORT),

                    // 취미여가
                    Map.entry("영화공연장", LEISURE),
                    Map.entry("기타오락휴식시", LEISURE),
                    Map.entry("기타교육교습", LEISURE),
                    Map.entry("사진관현상소", LEISURE),
                    Map.entry("헬스클럽", LEISURE),
                    Map.entry("골프장", LEISURE),
                    Map.entry("노래방", LEISURE),

                    // 기타
                    Map.entry("기타서비스", OTHER),

                    // 해외 업종
                    Map.entry("해외음식점", FOOD),
                    Map.entry("해외카페", CAFE),
                    Map.entry("해외쇼핑", SHOPPING),
                    Map.entry("해외교통", TRANSPORT),
                    Map.entry("해외관광", SIGHTSEEING),
                    Map.entry("숙박", LODGING),
                    Map.entry("항공사", TRANSPORT)
            );

    /**
     * CODEF 가맹점 업종으로 소비 카테고리를 분류한다.
     *
     * PG일반 또는 알 수 없는 업종은 가맹점명 모델이 처리할 수 있도록
     * Optional.empty()를 반환한다.
     */
    public Optional<CategoryClassificationResult> classify(String merchantType) {
        String normalizedType = normalize(merchantType);

        if (normalizedType.isEmpty() || isGenericPaymentType(normalizedType)) {
            return Optional.empty();
        }

        ConsumptionCategoryCode categoryCode = TYPE_MAPPING.get(normalizedType);

        if (categoryCode == null) {
            categoryCode = findByPartialMatch(normalizedType);
        }

        if (categoryCode == null) {
            return Optional.empty();
        }

        return Optional.of(
                CategoryClassificationResult.fromCodefType(categoryCode)
        );
    }

    // 짧은 접두어(예: "기타", "일반")는 여러 카테고리와 동시에 겹쳐서 모호하므로 부분 일치 대상에서 제외한다.
    private static final int MIN_PARTIAL_MATCH_LENGTH = 4;

    private ConsumptionCategoryCode findByPartialMatch(String merchantType) {
        if (merchantType.length() < MIN_PARTIAL_MATCH_LENGTH) {
            return null;
        }

        return TYPE_MAPPING.entrySet()
                .stream()
                .filter(entry -> entry.getKey().length() >= MIN_PARTIAL_MATCH_LENGTH)
                .filter(entry ->
                        merchantType.startsWith(entry.getKey())
                                || entry.getKey().startsWith(merchantType)
                )
                // 후보가 여러 개면 가장 구체적인(가장 긴) 키를 우선하고, 길이가 같으면 사전순으로 정렬해
                // Map 반복 순서에 좌우되지 않는 결정적인 결과를 보장한다.
                .max(Comparator
                        .comparingInt((Map.Entry<String, ConsumptionCategoryCode> entry) -> entry.getKey().length())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    /**
     * CODEF 응답에 포함된 공백, 점, 쉼표 및 깨진 문자 등을 제거한다.
     *
     * 예:
     * "제과.제빵"       → "제과제빵"
     * "문구,사무용품"   → "문구사무용품"
     * "기타교육.교습.?" → "기타교육교습"
     */
    private String normalize(String merchantType) {
        if (merchantType == null) {
            return "";
        }

        return merchantType
                .trim()
                .replaceAll("[^0-9A-Za-z가-힣]", "");
    }

    private boolean isGenericPaymentType(String merchantType) {
        return merchantType.startsWith("PG일반");
    }
}
