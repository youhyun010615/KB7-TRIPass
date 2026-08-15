package com.tripass.asset.duplicate;

import com.tripass.asset.dto.TransactionDto;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 체크카드 결제가 연결 계좌에서 즉시 출금되어 계좌·카드 양쪽에 중복 저장되는 거래를 식별한다.
 *
 * 순수 매칭 로직만 담당한다. 카드 유형(체크/신용), 사용자, 삭제 여부, 거래 유형(출금) 필터링은
 * 호출하는 서비스 계층에서 미리 끝내고, 이미 필터링된 목록만 이 클래스에 전달해야 한다.
 *
 * 매칭 규칙:
 * - (거래일, 금액)이 같고 시간 차이가 5분 이내인 조합만 후보로 본다.
 * - transactionTime이 null인 거래는 매칭 후보에서 제외한다(원본 거래는 정상 집계 대상으로 남는다).
 * - 계좌 거래 쪽 후보가 정확히 1건이고, 그 카드 거래 쪽에서 본 계좌 후보도 정확히 1건일 때만
 *   (상호 유일 매칭) 중복으로 확정한다. 후보가 여러 건이면 애매하므로 매칭하지 않는다.
 */
@Component
public class DuplicateTransactionMatcher {

    private static final long MAX_TIME_DIFF_SECONDS = 300; // 5분

    public List<MatchCandidate> findMatches(
            List<TransactionDto> accountTransactions,
            List<TransactionDto> checkCardTransactions
    ) {
        List<TransactionDto> accounts = withTransactionTime(accountTransactions);
        List<TransactionDto> cards = withTransactionTime(checkCardTransactions);

        Map<TransactionMatchKey, List<TransactionDto>> accountsByKey = groupByKey(accounts);
        Map<TransactionMatchKey, List<TransactionDto>> cardsByKey = groupByKey(cards);

        List<MatchCandidate> matches = new ArrayList<>();
        for (TransactionDto account : accounts) {
            TransactionMatchKey key = new TransactionMatchKey(account.getTransactionDate(), account.getAmount());

            List<TransactionDto> cardCandidates = cardsByKey.getOrDefault(key, List.of()).stream()
                    .filter(card -> withinTimeWindow(account, card))
                    .toList();
            if (cardCandidates.size() != 1) {
                continue;
            }
            TransactionDto matchedCard = cardCandidates.get(0);

            List<TransactionDto> accountCandidates = accountsByKey.getOrDefault(key, List.of()).stream()
                    .filter(candidate -> withinTimeWindow(candidate, matchedCard))
                    .toList();
            if (accountCandidates.size() != 1) {
                continue;
            }

            matches.add(new MatchCandidate(account.getId(), matchedCard.getId()));
        }
        return matches;
    }

    public Set<Long> findDuplicateAccountTransactionIds(
            List<TransactionDto> accountTransactions,
            List<TransactionDto> checkCardTransactions
    ) {
        return findMatches(accountTransactions, checkCardTransactions).stream()
                .map(MatchCandidate::accountTransactionId)
                .collect(Collectors.toSet());
    }

    private List<TransactionDto> withTransactionTime(List<TransactionDto> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.getTransactionTime() != null)
                .toList();
    }

    private Map<TransactionMatchKey, List<TransactionDto>> groupByKey(List<TransactionDto> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> new TransactionMatchKey(transaction.getTransactionDate(), transaction.getAmount())
                ));
    }

    private boolean withinTimeWindow(TransactionDto a, TransactionDto b) {
        long diffSeconds = Math.abs(Duration.between(a.getTransactionTime(), b.getTransactionTime()).getSeconds());
        return diffSeconds <= MAX_TIME_DIFF_SECONDS;
    }
}
