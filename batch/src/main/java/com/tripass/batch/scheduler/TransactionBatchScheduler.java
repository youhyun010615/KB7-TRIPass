package com.tripass.batch.scheduler;

import com.tripass.batch.service.MockTransactionGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionBatchScheduler {

    private final MockTransactionGenerator mockTransactionGenerator;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void runDailyTransactionBatch() {
        log.info("[Batch Engine] 자정 정기 가상 거래 적재 배치 구동");
        mockTransactionGenerator.generateAndInsertMockTransactions(15);
    }
}