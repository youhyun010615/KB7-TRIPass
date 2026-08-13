package com.tripass.batch.service;

import com.tripass.batch.dto.TransactionMockDto;
import com.tripass.batch.mapper.MockTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockTransactionGenerator {

    private final MockTransactionMapper mockTransactionMapper;
    private final Random random = new Random();

    private static final String[] DOMESTIC_MERCHANTS = {"스타벅스 강남점", "CU 역삼점", "CGV", "올리브영", "배달의민족"};
    private static final String[] OVERSEAS_MERCHANTS = {"7-Eleven Shibuya", "Lawson Shinjuku", "Uber Trip Paris", "McDonalds TimesSquare"};

    @Transactional
    public void generateAndInsertMockTransactions(int count) {
        // 1. DB에서 활성 계좌 목록 조회
        List<Long> activeAccountIds = mockTransactionMapper.selectActiveAccountIds();

        // 계좌가 없을 경우 테스트를 위한 가상 계좌 ID(1L) 임시 지정
        Long targetAccountId = activeAccountIds.isEmpty() ? 1L : activeAccountIds.get(0);

        for (int i = 0; i < count; i++) {
            String region = random.nextDouble() < 0.7 ? "DOMESTIC" : "OVERSEAS";
            String txType = random.nextDouble() < 0.8 ? "WITHDRAWAL" : "DEPOSIT";

            BigDecimal amount = BigDecimal.valueOf((random.nextInt(40) + 1) * 1000L);
            String merchant = "DOMESTIC".equals(region)
                    ? DOMESTIC_MERCHANTS[random.nextInt(DOMESTIC_MERCHANTS.length)]
                    : OVERSEAS_MERCHANTS[random.nextInt(OVERSEAS_MERCHANTS.length)];

            BigDecimal appliedRate = null;
            BigDecimal originalAmount = null;

            if ("OVERSEAS".equals(region)) {
                appliedRate = BigDecimal.valueOf(9.50); // 엔화(JPY) 가상 환율
                originalAmount = amount.divide(appliedRate, 2, RoundingMode.HALF_UP);
            }

            String externalKey = "MOCK_TX_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

            TransactionMockDto dto = TransactionMockDto.builder()
                    .accountId(targetAccountId)
                    .transactionDate(LocalDate.now())
                    .transactionTime(LocalTime.now())
                    .transactionType(txType)
                    .transactionRegion(region)
                    .amount(amount)
                    .merchantName(merchant)
                    .appliedExchangeRate(appliedRate)
                    .originalAmount(originalAmount)
                    .paymentMethod("CHECK_CARD")
                    .isPreExpense(false)
                    .memo("단위 테스트 생성 거래 내역")
                    .externalKey(externalKey)
                    .isDeleted((byte) 0)
                    .build();

            // 2. DB Insert 실행
            mockTransactionMapper.insertMockTransaction(dto);
            log.info("  └ 핑! [{}] {} - {}원 ({}) Insert 완료", region, merchant, amount, externalKey);
        }
        log.info("[Success] 총 {}건의 Mock 거래 데이터가 성공적으로 적재되었습니다.", count);
    }
}