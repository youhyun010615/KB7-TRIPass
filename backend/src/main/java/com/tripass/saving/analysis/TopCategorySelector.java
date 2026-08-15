package com.tripass.saving.analysis;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 추천 점수가 높은 순으로 최대 3개를 선정한다.
 * 동점 처리: 지출액(1~28일) 큰 순 → 거래 횟수(1~28일) 많은 순 → 카테고리 ID 오름차순
 */
@Component
public class TopCategorySelector {

    private static final int TOP_COUNT = 3;

    public List<Long> selectTopCategoryIds(
            List<ScoredCandidate> scoredCandidates,
            Map<Long, CategorySpendingStats> statsByCategoryId
    ) {
        return scoredCandidates.stream()
                .sorted((a, b) -> compare(a, b, statsByCategoryId))
                .limit(TOP_COUNT)
                .map(ScoredCandidate::categoryId)
                .toList();
    }

    private int compare(ScoredCandidate a, ScoredCandidate b, Map<Long, CategorySpendingStats> statsByCategoryId) {
        int scoreCompare = b.recommendationScore().compareTo(a.recommendationScore());
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        CategorySpendingStats statsA = statsByCategoryId.get(a.categoryId());
        CategorySpendingStats statsB = statsByCategoryId.get(b.categoryId());

        int amountCompare = statsB.missionPeriodSpending().compareTo(statsA.missionPeriodSpending());
        if (amountCompare != 0) {
            return amountCompare;
        }

        int countCompare = Integer.compare(statsB.missionTransactionCount(), statsA.missionTransactionCount());
        if (countCompare != 0) {
            return countCompare;
        }

        return Long.compare(a.categoryId(), b.categoryId());
    }
}
