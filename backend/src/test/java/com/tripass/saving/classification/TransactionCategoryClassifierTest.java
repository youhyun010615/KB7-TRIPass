package com.tripass.saving.classification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionCategoryClassifierTest {

    private TransactionCategoryClassifier classifier;

    @BeforeEach
    void setUp() {
        MerchantNameClassifier merchantNameClassifier =
                new MerchantNameClassifier(new MerchantNameNormalizer());
        classifier = new TransactionCategoryClassifier(
                new CodefMerchantTypeClassifier(),
                merchantNameClassifier
        );
    }

    @Test
    void 사용자_지정_카테고리를_가장_우선한다() {
        CategoryClassificationResult result = classifier.classify(
                "스타벅스 서울역",
                "커피전문점",
                ConsumptionCategoryCode.LEISURE
        );

        assertEquals(ConsumptionCategoryCode.LEISURE, result.categoryCode());
        assertEquals(CategorySource.USER, result.source());
        assertEquals("1.0000", result.confidence().toPlainString());
    }

    @Test
    void 명확한_CODEF_업종은_AI보다_우선한다() {
        CategoryClassificationResult result = classifier.classify(
                "스타벅스 서울역",
                "택시"
        );

        assertEquals(ConsumptionCategoryCode.TRANSPORT, result.categoryCode());
        assertEquals(CategorySource.CODEF_TYPE, result.source());
    }

    @Test
    void PG일반은_가맹점명_AI로_분류한다() {
        CategoryClassificationResult result = classifier.classify(
                "쿠팡이츠",
                "PG일반(비인증)"
        );

        assertEquals(ConsumptionCategoryCode.FOOD, result.categoryCode());
        assertEquals(CategorySource.AI_MODEL, result.source());
    }

    @Test
    void 업종이_비어있으면_가맹점명_AI로_분류한다() {
        CategoryClassificationResult result = classifier.classify(
                "티머니 지하철",
                null
        );

        assertEquals(ConsumptionCategoryCode.TRANSPORT, result.categoryCode());
        assertEquals(CategorySource.AI_MODEL, result.source());
    }

    @Test
    void 업종과_가맹점명이_모두_없으면_기타로_처리한다() {
        CategoryClassificationResult result = classifier.classify(null, null);

        assertEquals(ConsumptionCategoryCode.OTHER, result.categoryCode());
        assertEquals(CategorySource.FALLBACK, result.source());
        assertEquals("0.0000", result.confidence().toPlainString());
    }
}
