package com.tripass.saving.classification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodefMerchantTypeClassifierTest {

    private CodefMerchantTypeClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new CodefMerchantTypeClassifier();
    }

    @Test
    void CODEF_업종을_소비_카테고리로_분류한다() {
        Map<String, ConsumptionCategoryCode> cases = Map.ofEntries(
                Map.entry("일반음식점", ConsumptionCategoryCode.FOOD),
                Map.entry("일식전문점", ConsumptionCategoryCode.FOOD),
                Map.entry("커피전문점", ConsumptionCategoryCode.CAFE),
                Map.entry("제과.제빵", ConsumptionCategoryCode.CAFE),
                Map.entry("편의점", ConsumptionCategoryCode.LIVING),
                Map.entry("공과금", ConsumptionCategoryCode.LIVING),
                Map.entry("택시", ConsumptionCategoryCode.TRANSPORT),
                Map.entry("시내버스", ConsumptionCategoryCode.TRANSPORT),
                Map.entry("지하철", ConsumptionCategoryCode.TRANSPORT),
                Map.entry("일반의류", ConsumptionCategoryCode.SHOPPING),
                Map.entry("백화점", ConsumptionCategoryCode.SHOPPING),
                Map.entry("영화.공연장", ConsumptionCategoryCode.LEISURE),
                Map.entry("기타오락.휴식시", ConsumptionCategoryCode.LEISURE)
        );

        cases.forEach((merchantType, expectedCategory) -> {
            CategoryClassificationResult result =
                    classifier.classify(merchantType).orElseThrow();

            assertEquals(expectedCategory, result.categoryCode());
            assertEquals(CategorySource.CODEF_TYPE, result.source());
        });
    }

    @Test
    void 특수문자가_포함된_CODEF_업종도_정규화하여_분류한다() {
        CategoryClassificationResult result =
                classifier.classify("문구,사무용품").orElseThrow();

        assertEquals(
                ConsumptionCategoryCode.SHOPPING,
                result.categoryCode()
        );
    }

    @Test
    void 잘린_CODEF_업종도_부분_일치로_분류한다() {
        CategoryClassificationResult result =
                classifier.classify("아이스크림전문?").orElseThrow();

        assertEquals(
                ConsumptionCategoryCode.CAFE,
                result.categoryCode()
        );
    }

    @Test
    void PG일반_업종은_가맹점명_분류를_위해_빈_결과를_반환한다() {
        Optional<CategoryClassificationResult> result =
                classifier.classify("PG일반(비인증)");

        assertTrue(result.isEmpty());
    }

    @Test
    void 업종이_없으면_빈_결과를_반환한다() {
        assertTrue(classifier.classify(null).isEmpty());
        assertTrue(classifier.classify("").isEmpty());
        assertTrue(classifier.classify("   ").isEmpty());
    }

    @Test
    void 등록되지_않은_업종이면_빈_결과를_반환한다() {
        Optional<CategoryClassificationResult> result =
                classifier.classify("알수없는업종");

        assertTrue(result.isEmpty());
    }

    @Test
    void 실제_CODEF_업종_추가_사례를_분류한다() {
        Map<String, ConsumptionCategoryCode> cases = Map.ofEntries(
                Map.entry("제과.제빵", ConsumptionCategoryCode.CAFE),
                Map.entry("안경.광학제품", ConsumptionCategoryCode.LIVING),
                Map.entry("피부.체형미관리", ConsumptionCategoryCode.LIVING),
                Map.entry("축산물,정육점", ConsumptionCategoryCode.FOOD),
                Map.entry("문구,사무용품", ConsumptionCategoryCode.SHOPPING),
                Map.entry("사진관.현상소", ConsumptionCategoryCode.LEISURE),
                Map.entry("골프장", ConsumptionCategoryCode.LEISURE),
                Map.entry("노래방", ConsumptionCategoryCode.LEISURE)
        );

        cases.forEach((merchantType, expectedCategory) -> {
            CategoryClassificationResult result =
                    classifier.classify(merchantType).orElseThrow();

            assertEquals(expectedCategory, result.categoryCode());
        });

        assertTrue(classifier.classify("PG일반(인증)").isEmpty());
        assertTrue(classifier.classify("PG일반(비인증)").isEmpty());
    }
}
