package com.tripass.asset.service;

import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.service.provider.CardTransactionProvider;
import com.tripass.asset.service.provider.CardTransactionProviderRouter;
import com.tripass.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CardTransactionService {

    private final CardTransactionProviderRouter providerRouter;

    public CardTransactionService(CardTransactionProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }

    public List<CardDto> getCards(Long userId) {
        return getProvider().getCards(userId);
    }

    /**
     * 연월 또는 시작일·종료일로 카드 거래을 조회합니다.
     *
     * yearMonth와 날짜 범위를 동시에 받으면 해석이 모호해지므로 400을 반환합니다.
     * 조회 조건이 없으면 현재 월을 기본값으로 사용합니다.
     */
    public List<TransactionDto> getTransactions(
            Long userId,
            Long cardId,
            String yearMonth,
            String startDate,
            String endDate
    ) {
        DateRange range = resolveDateRange(yearMonth, startDate, endDate);
        return getProvider().getTransactions(
                userId,
                cardId,
                range.startDate,
                range.endDate
        );
    }

    private CardTransactionProvider getProvider() {
        return providerRouter.getProvider();
    }

    private DateRange resolveDateRange(String yearMonth, String startDate, String endDate) {
        boolean hasYearMonth = hasText(yearMonth);
        boolean hasStartDate = hasText(startDate);
        boolean hasEndDate = hasText(endDate);

        if (hasYearMonth && (hasStartDate || hasEndDate)) {
            throw invalidPeriod("yearMonth와 시작일·종료일은 동시에 사용할 수 없습니다.");
        }
        if (hasStartDate != hasEndDate) {
            throw invalidPeriod("시작일과 종료일을 모두 입력해 주세요.");
        }

        try {
            if (hasYearMonth) {
                YearMonth month = YearMonth.parse(yearMonth);
                return new DateRange(month.atDay(1), month.atEndOfMonth());
            }
            if (hasStartDate) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                if (start.isAfter(end)) {
                    throw invalidPeriod("시작일은 종료일보다 늦을 수 없습니다.");
                }
                return new DateRange(start, end);
            }

            YearMonth currentMonth = YearMonth.now();
            return new DateRange(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        } catch (DateTimeParseException exception) {
            throw invalidPeriod("날짜는 yearMonth=yyyy-MM 또는 yyyy-MM-dd 형식으로 입력해 주세요.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private CustomException invalidPeriod(String message) {
        return new CustomException(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_PERIOD", message);
    }

    private static class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        private DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}
