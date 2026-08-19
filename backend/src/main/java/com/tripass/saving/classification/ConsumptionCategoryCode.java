package com.tripass.saving.classification;

/**
 * AI 저축 미션에서 사용하는 소비 카테고리 코드.
 *
 * spending_categories.category_code 값과 일치해야 한다.
 */
public enum ConsumptionCategoryCode {

    FOOD("식비"),
    CAFE("카페"),
    SHOPPING("쇼핑"),
    LIVING("생활비"),
    TRANSPORT("교통비"),
    LEISURE("취미여가"),
    LODGING("숙박"),
    SIGHTSEEING("관광"),
    OTHER("기타");

    private final String displayName;

    ConsumptionCategoryCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ConsumptionCategoryCode fromDisplayName(String displayName) {
        for (ConsumptionCategoryCode category : values()) {
            if (category.displayName.equals(displayName)) {
                return category;
            }
        }

        throw new IllegalArgumentException(
                "지원하지 않는 소비 카테고리입니다: " + displayName
        );
    }
}
