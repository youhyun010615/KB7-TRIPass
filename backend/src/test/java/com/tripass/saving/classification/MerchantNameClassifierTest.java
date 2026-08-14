package com.tripass.saving.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantNameClassifierTest {

    private MerchantNameClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new MerchantNameClassifier(
                new ObjectMapper(),
                new MerchantNameNormalizer()
        );
    }

    @Test
    void 실제_학습_가맹점명을_분류한다() {
        assertCategory("비엣포(viet pho)", ConsumptionCategoryCode.FOOD);
        assertCategory("컴포즈커피 세종대학교점", ConsumptionCategoryCode.CAFE);
        assertCategory("이마트24 용산삼일", ConsumptionCategoryCode.LIVING);
        assertCategory("메가박스중앙(주)강남지점", ConsumptionCategoryCode.LEISURE);
        assertCategory("카카오스타일", ConsumptionCategoryCode.SHOPPING);
        assertCategory("티머니 지하철", ConsumptionCategoryCode.TRANSPORT);
    }

    @Test
    void PG결제_가맹점명을_분류한다() {
        assertCategory("쿠팡이츠", ConsumptionCategoryCode.FOOD);
        assertCategory("쿠팡(쿠페이)", ConsumptionCategoryCode.SHOPPING);
    }

    @Test
    void 빈_가맹점명은_기타로_처리한다() {
        CategoryClassificationResult result = classifier.classify(null);
        assertEquals(ConsumptionCategoryCode.OTHER, result.categoryCode());
        assertEquals(CategorySource.FALLBACK, result.source());
    }

    @Test
    void 모델_분류_결과에는_신뢰도가_포함된다() {
        CategoryClassificationResult result = classifier.classify("컴포즈커피 세종대학교점");
        assertNotNull(result.confidence());
        assertTrue(result.confidence().doubleValue() >= 0);
        assertTrue(result.confidence().doubleValue() <= 1);
    }

    private void assertCategory(String merchantName, ConsumptionCategoryCode expected) {
        CategoryClassificationResult result = classifier.classify(merchantName);
        assertEquals(expected, result.categoryCode(), merchantName);
        assertEquals(CategorySource.AI_MODEL, result.source(), merchantName);
    }
}
