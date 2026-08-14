package com.tripass.saving.classification;

import java.math.BigDecimal;

/**
 * 거래 카테고리 분류 결과.
 *
 * @param categoryCode 분류된 소비 카테고리
 * @param source 분류 출처
 * @param confidence 분류 신뢰도(0~1)
 */
public record CategoryClassificationResult(
        ConsumptionCategoryCode categoryCode,
        CategorySource source,
        BigDecimal confidence
) {

    public static CategoryClassificationResult fromCodefType(
            ConsumptionCategoryCode categoryCode
    ) {
        return new CategoryClassificationResult(
                categoryCode,
                CategorySource.CODEF_TYPE,
                new BigDecimal("0.9800")
        );
    }
}
