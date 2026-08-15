package com.tripass.saving.analysis;

import java.math.BigDecimal;

public record ScoredCandidate(
        Long categoryId,
        BigDecimal spendingShareScore,
        BigDecimal increaseScore,
        BigDecimal amountRankScore,
        BigDecimal recommendationScore,
        BigDecimal actualIncreaseRate // 최근 3개월 평균 대비 실제 증가율(0~1 캡핑 없음), 이전 데이터 없으면 null
) {
}
