package com.tripass.travel.scheduler;

import com.tripass.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 매일 자정, 여행 상태를 시작일·종료일 기준으로 PLANNING → TRAVELING → ENDED 순서로 전환하는 스케줄러입니다. */

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelStatusScheduler {

    private final TravelService travelService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void updateTripStatuses() {
        LocalDate today = LocalDate.now();
        log.info("여행 상태 자동 전환 스케줄러 시작 - 기준일: {}", today);

        try {
            int startedCount = travelService.updateTravelingTrips(today);
            log.info("여행 상태 자동 전환(PLANNING→TRAVELING) 완료 - 전환 건수: {}", startedCount);
        } catch (Exception e) {
            log.error("여행 상태 자동 전환(PLANNING→TRAVELING) 실패 - 기준일: {}, 사유: {}", today, e.getMessage(), e);
        }

        try {
            int endedCount = travelService.updateEndedTrips(today);
            log.info("여행 상태 자동 전환(TRAVELING→ENDED) 완료 - 전환 건수: {}", endedCount);
        } catch (Exception e) {
            log.error("여행 상태 자동 전환(TRAVELING→ENDED) 실패 - 기준일: {}, 사유: {}", today, e.getMessage(), e);
        }
    }
}
