package com.tripass.wallet.scheduler;

import com.tripass.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 매일 월 목표 자동 송금 대상 월렛을 찾아 자동 충전 원장을 생성하는 스케줄러입니다. */

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletAutoSavingScheduler {

    private final WalletService walletService;

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void executeDailyAutoSaving() {
        LocalDate today = LocalDate.now();
        log.info("월렛 자동 송금 스케줄러 시작 - 기준일: {}", today);

        try {
            int successCount = walletService.executeDueAutoSavingRules(today);
            log.info("월렛 자동 송금 스케줄러 완료 - 처리 건수: {}", successCount);
        } catch (Exception e) {
            log.error("월렛 자동 송금 스케줄러 실패 - 기준일: {}, 사유: {}", today, e.getMessage(), e);
        }
    }
}
