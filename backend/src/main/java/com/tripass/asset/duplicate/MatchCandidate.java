package com.tripass.asset.duplicate;

/**
 * 상호 유일 매칭으로 확정된 계좌·카드 거래 쌍.
 */
public record MatchCandidate(Long accountTransactionId, Long cardTransactionId) {
}
