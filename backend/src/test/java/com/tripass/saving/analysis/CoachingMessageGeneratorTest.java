package com.tripass.saving.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoachingMessageGeneratorTest {

    private final CoachingMessageGenerator generator = new CoachingMessageGenerator();

    @Test
    void 카테고리명을_나열해서_요약_문구를_생성한다() {
        String summary = generator.generateSummary(List.of("카페", "쇼핑", "식비"));

        assertEquals("이번 달에는 카페, 쇼핑, 식비 소비를 줄여보세요.", summary);
    }

    @Test
    void 추천_카테고리가_없으면_안내_문구를_반환한다() {
        String summary = generator.generateSummary(List.of());

        assertEquals("지난달에는 절감이 필요한 소비 카테고리가 발견되지 않았어요.", summary);
    }

    @Test
    void 증가_점수가_가장_높으면_실제_증가율로_증가율_문구를_생성한다() {
        // 정규화 점수(0.35)는 판단에만 쓰이고, 문구에는 evidence의 실제 증가율(250%)을 그대로 표시해야 한다.
        ScoredCandidate candidate = new ScoredCandidate(
                1L, new BigDecimal("0.2"), new BigDecimal("0.35"), new BigDecimal("0.1"), new BigDecimal("0.5"),
                new BigDecimal("2.5"));
        RecommendationEvidence evidence = new RecommendationEvidence(
                new BigDecimal("20"), new BigDecimal("250"), 10, new BigDecimal("100000"));

        String reason = generator.generateReason(candidate, evidence);

        assertTrue(reason.contains("250%"));
        assertTrue(reason.contains("증가했어요"));
    }

    @Test
    void 지출_비율_점수가_가장_높으면_실제_비중으로_비중_문구를_생성한다() {
        ScoredCandidate candidate = new ScoredCandidate(
                1L, new BigDecimal("0.6"), new BigDecimal("0.1"), new BigDecimal("0.1"), new BigDecimal("0.5"),
                new BigDecimal("0.1"));
        RecommendationEvidence evidence = new RecommendationEvidence(
                new BigDecimal("60"), new BigDecimal("10"), 10, new BigDecimal("100000"));

        String reason = generator.generateReason(candidate, evidence);

        assertTrue(reason.contains("60%"));
        assertTrue(reason.contains("비중이 높아"));
    }

    @Test
    void 순위_점수가_가장_높으면_실제_지출액으로_금액_문구를_생성한다() {
        ScoredCandidate candidate = new ScoredCandidate(
                1L, new BigDecimal("0.1"), new BigDecimal("0.0"), new BigDecimal("0.6"), new BigDecimal("0.4"),
                null);
        RecommendationEvidence evidence = new RecommendationEvidence(
                new BigDecimal("10"), null, 10, new BigDecimal("150000"));

        String reason = generator.generateReason(candidate, evidence);

        assertTrue(reason.contains("150,000원"));
        assertTrue(reason.contains("지출해서"));
    }
}
