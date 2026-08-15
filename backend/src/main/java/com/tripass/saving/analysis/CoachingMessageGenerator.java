package com.tripass.saving.analysis;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * 실제 생성형 AI 호출 없이, 점수 구성 요소를 기반으로 한 템플릿 코칭 문구를 생성한다.
 */
@Component
public class CoachingMessageGenerator {

    public String generateSummary(List<String> categoryNames) {
        if (categoryNames.isEmpty()) {
            return "지난달에는 절감이 필요한 소비 카테고리가 발견되지 않았어요.";
        }
        return "이번 달에는 " + String.join(", ", categoryNames) + " 소비를 줄여보세요.";
    }

    /**
     * 추천 사유 문구를 생성한다.
     * 어떤 사유 템플릿을 고를지는 정규화된 점수의 가중 기여도로 판단하되,
     * 문구에 실제로 표시하는 수치는 {@code evidence}의 캡핑되지 않은 실제 분석값을 사용한다.
     */
    public String generateReason(ScoredCandidate candidate, RecommendationEvidence evidence) {
        BigDecimal increase = candidate.increaseScore();

        // 최종 점수는 가중합이므로, 어떤 요소가 실제로 가장 크게 기여했는지는
        // 가중치를 곱한 기여도로 비교해야 최종 점수 계산과 일관된다.
        BigDecimal shareContribution = candidate.spendingShareScore()
                .multiply(RecommendationScoreCalculator.SPENDING_SHARE_WEIGHT);
        BigDecimal increaseContribution = increase
                .multiply(RecommendationScoreCalculator.INCREASE_WEIGHT);
        BigDecimal rankContribution = candidate.amountRankScore()
                .multiply(RecommendationScoreCalculator.AMOUNT_RANK_WEIGHT);

        if (increase.compareTo(BigDecimal.ZERO) > 0
                && increaseContribution.compareTo(shareContribution) >= 0
                && increaseContribution.compareTo(rankContribution) >= 0) {
            BigDecimal percent = evidence.actualIncreaseRate().setScale(0, RoundingMode.HALF_UP);
            return "최근 3개월 평균보다 소비가 " + percent.toPlainString() + "% 증가했어요.";
        }
        if (shareContribution.compareTo(rankContribution) >= 0) {
            BigDecimal ratio = evidence.spendingRatio().setScale(0, RoundingMode.HALF_UP);
            return "전체 소비에서 " + ratio.toPlainString() + "%를 차지할 만큼 비중이 높아 조금만 줄여도 절감 효과가 클 것으로 보여요.";
        }
        String amount = formatAmount(evidence.missionPeriodSpending());
        return "이번 달 " + amount + "원을 지출해서, 절감 시 확보할 수 있는 금액이 많아요.";
    }

    // DecimalFormat은 thread-safe하지 않으므로 싱글톤 빈에 static 필드로 두지 않고 호출마다 생성한다.
    private String formatAmount(BigDecimal amount) {
        return new DecimalFormat("#,##0").format(amount);
    }
}
