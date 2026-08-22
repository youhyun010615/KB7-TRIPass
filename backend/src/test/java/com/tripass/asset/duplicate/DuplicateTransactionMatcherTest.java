package com.tripass.asset.duplicate;

import com.tripass.asset.dto.TransactionDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateTransactionMatcherTest {

    private final DuplicateTransactionMatcher matcher = new DuplicateTransactionMatcher();
    private static final LocalDate DATE = LocalDate.of(2026, 8, 1);

    @Test
    void 정상_1대1_매칭이면_계좌_거래를_중복으로_판단한다() {
        TransactionDto account = transaction(1L, LocalTime.of(12, 0), "10000");
        TransactionDto card = transaction(2L, LocalTime.of(12, 0), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertEquals(Set.of(1L), duplicates);
    }

    @Test
    void 시간차이가_4분이면_매칭된다() {
        TransactionDto account = transaction(1L, LocalTime.of(12, 0, 0), "10000");
        TransactionDto card = transaction(2L, LocalTime.of(12, 4, 0), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertEquals(Set.of(1L), duplicates);
    }

    @Test
    void 시간차이가_정확히_5분이면_경계값으로_매칭된다() {
        TransactionDto account = transaction(1L, LocalTime.of(12, 0, 0), "10000");
        TransactionDto card = transaction(2L, LocalTime.of(12, 5, 0), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertEquals(Set.of(1L), duplicates);
    }

    @Test
    void 시간차이가_5분을_초과하면_매칭하지_않는다() {
        TransactionDto account = transaction(1L, LocalTime.of(12, 0, 0), "10000");
        TransactionDto card = transaction(2L, LocalTime.of(12, 5, 1), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertTrue(duplicates.isEmpty());
    }

    @Test
    void 날짜와_금액이_같아도_시간대가_많이_다르면_매칭하지_않는다() {
        TransactionDto account = transaction(1L, LocalTime.of(10, 0), "10000");
        TransactionDto card = transaction(2L, LocalTime.of(20, 0), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertTrue(duplicates.isEmpty());
    }

    @Test
    void 계좌_후보가_2건이고_카드가_1건이면_애매하므로_매칭하지_않는다() {
        TransactionDto accountA = transaction(1L, LocalTime.of(12, 0), "10000");
        TransactionDto accountB = transaction(2L, LocalTime.of(12, 1), "10000");
        TransactionDto card = transaction(3L, LocalTime.of(12, 0, 30), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(accountA, accountB), List.of(card));

        assertTrue(duplicates.isEmpty());
    }

    @Test
    void 카드_후보가_2건이고_계좌가_1건이면_애매하므로_매칭하지_않는다() {
        TransactionDto account = transaction(1L, LocalTime.of(12, 0), "10000");
        TransactionDto cardX = transaction(2L, LocalTime.of(12, 0, 30), "10000");
        TransactionDto cardY = transaction(3L, LocalTime.of(12, 1), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(cardX, cardY));

        assertTrue(duplicates.isEmpty());
    }

    @Test
    void 여러_계좌_거래_중_상호_유일한_쌍만_매칭된다() {
        // A-X는 상호 유일 매칭, B는 카드 후보가 없어 매칭되지 않는다.
        TransactionDto accountA = transaction(1L, LocalTime.of(9, 0), "5000");
        TransactionDto accountB = transaction(2L, LocalTime.of(15, 0), "7000");
        TransactionDto cardX = transaction(3L, LocalTime.of(9, 1), "5000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(accountA, accountB), List.of(cardX));

        assertEquals(Set.of(1L), duplicates);
    }

    @Test
    void transactionTime이_null이면_매칭_후보에서_제외되고_원본은_그대로_남는다() {
        TransactionDto account = transaction(1L, null, "10000");
        TransactionDto card = transaction(2L, LocalTime.of(12, 0), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertTrue(duplicates.isEmpty());
    }

    @Test
    void 카드_시간이_null이면_매칭_후보에서_제외된다() {
        TransactionDto account = transaction(1L, LocalTime.of(12, 0), "10000");
        TransactionDto card = transaction(2L, null, "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertTrue(duplicates.isEmpty());
    }

    @Test
    void 양쪽_모두_자정_기본값이면_매칭된다_오매칭_가능성이_있으므로_문서화용_테스트() {
        // CODEF 시간 누락 시 애플리케이션이 00:00:00으로 기본값 처리한 거래와
        // 실제 자정 거래를 구분할 수 없어 현재는 매칭을 허용한다. (알려진 한계)
        TransactionDto account = transaction(1L, LocalTime.of(0, 0), "10000");
        TransactionDto card = transaction(2L, LocalTime.of(0, 0), "10000");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertEquals(Set.of(1L), duplicates);
    }

    @Test
    void 금액_스케일이_달라도_같은_값이면_매칭된다() {
        TransactionDto account = transaction(1L, LocalTime.of(12, 0), "10000");
        TransactionDto card = transaction(2L, LocalTime.of(12, 0), "10000.00");

        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(
                List.of(account), List.of(card));

        assertEquals(Set.of(1L), duplicates);
    }

    @Test
    void 후보가_없으면_빈_결과를_반환한다() {
        Set<Long> duplicates = matcher.findDuplicateAccountTransactionIds(List.of(), List.of());

        assertTrue(duplicates.isEmpty());
    }

    private TransactionDto transaction(Long id, LocalTime time, String amount) {
        TransactionDto dto = new TransactionDto();
        dto.setId(id);
        dto.setTransactionDate(DATE);
        dto.setTransactionTime(time);
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }
}
