package com.tripass.saving.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 소비 집계 계산에 필요한 최소 정보만 담은 값 객체.
 * 계좌·카드 거래를 병합·중복제거한 뒤 이 형태로 변환해서 넘긴다.
 */
public record CategorizedSpending(Long categoryId, LocalDate transactionDate, BigDecimal amount) {
}
