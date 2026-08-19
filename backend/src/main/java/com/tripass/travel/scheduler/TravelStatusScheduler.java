package com.tripass.travel.scheduler;

import com.tripass.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 매일 자정, 종료일이 지난 여행의 상태를 TRAVELING에서 ENDED로 전환하는 스케줄러입니다. */

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelStatusScheduler {

    private final TravelService travelService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void updateEndedTrips() {
        LocalDate today = LocalDate.now();
        log.info("여행 상태 자동 전환 스케줄러 시작 - 기준일: {}", today);

        try {
            int updatedCount = travelService.updateEndedTrips(today);
            log.info("여행 상태 자동 전환 스케줄러 완료 - 전환 건수: {}", updatedCount);
        } catch (Exception e) {
            log.error("여행 상태 자동 전환 스케줄러 실패 - 기준일: {}, 사유: {}", today, e.getMessage(), e);
        }
    }
}
