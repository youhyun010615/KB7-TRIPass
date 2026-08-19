package com.tripass.common.scheduler;

import com.tripass.asset.mapper.AssetMapper;
import com.tripass.common.util.FcmService;
import com.tripass.exchange.service.ExchangeRateService;
import com.tripass.mypage.domain.NotificationSetting;
import com.tripass.mypage.mapper.NotificationSettingMapper;
import com.tripass.mypage.mapper.NotificationTargetMapper;
import com.tripass.mypage.service.NotificationService;
import com.tripass.saving.service.MonthlySpendingAnalysisService;
import com.tripass.schedule.domain.TripSchedule;
import com.tripass.schedule.mapper.ScheduleMapper;
import com.tripass.travel.domain.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class NotificationScheduler {

    private final FcmService fcmService;
    private final NotificationService notificationService;
    private final NotificationTargetMapper notificationTargetMapper;
    private final NotificationSettingMapper notificationSettingMapper;
    private final ScheduleMapper scheduleMapper;
    private final ExchangeRateService exchangeRateService;
    private final MonthlySpendingAnalysisService monthlySpendingAnalysisService;
    private final AssetMapper assetMapper;

    // 매일 자정
    @Scheduled(cron = "0 0 0 * * *")
    public void sendDailyNotifications() {
        log.info("일일 알림 발송 스케줄러 시작");
        
        sendChecklistNotification(30, "여행까지 한 달!", "준비를 시작할 때예요.");
        sendChecklistNotification(7, "여행 일주일 전!", "꼼꼼하게 준비하고 계신가요?");
        sendChecklistNotification(1, "여행 D-1", "마지막 준비는 완벽한가요?");
        sendReturnNotification(1, "여행의 마무리", "안전한 귀국을 위해 체크리스트를 확인하세요.");
        
        // 여행 7일 전 — 여행 대비 리포트
        List<Trip> upcomingTrips = notificationTargetMapper.findTripsByStartDateOffset(7);
        for (Trip trip : upcomingTrips) {
            String title = "여행 대비 리포트";
            String body = "출발까지 일주일! 목표 달성 현황과 준비 상태를 확인해보세요.";
            String url = "/mypage/reports?tripId=" + trip.getId();
            sendToTrip(trip, "REPORT", title, body, url);
        }

        // 여행 시작 당일 (D-DAY)
        List<Trip> startingTrips = notificationTargetMapper.findTripsStartingToday();
        for (Trip trip : startingTrips) {
            String title = "여행의 시작, 목표 달성 확인!";
            String body = "여행을 위해 그동안 얼마나 모으셨나요? 목표 달성 현황을 확인해보세요.";
            String url = "/mypage/reports?tripId=" + trip.getId();
            sendToTrip(trip, "REPORT", title, body, url);
        }

        // 여행 종료 다음날 (귀국 다음날)
        List<Trip> endedTrips = notificationTargetMapper.findTripsEndedYesterday();
        for (Trip trip : endedTrips) {
            String title = "여행 리포트";
            String body = "여행은 어떠셨나요? 리포트를 확인해보세요.";
            String url = "/mypage/reports?tripId=" + trip.getId();
            sendToTrip(trip, "REPORT", title, body, url);
        }
    }

    // 매일 12시 5분에 실행 (주중/주말 관계없이 최근 영업일 기준)
    @Scheduled(cron = "0 5 12 * * *", zone = "Asia/Seoul")
    public void sendExchangeRateAlerts() {
        log.info("[Notification] 환율 알림 통합 스케줄러 시작");

        java.util.Map<Long, java.math.BigDecimal> latestRates = exchangeRateService.getLatestRatesMap();
        java.util.List<com.tripass.exchange.domain.ExchangeRateAlert> alerts = exchangeRateService.findAllActiveAlerts();
        
        for (com.tripass.exchange.domain.ExchangeRateAlert alert : alerts) {
            java.math.BigDecimal rate = latestRates.get(alert.getCurrencyId());
            
            if (rate != null && rate.compareTo(java.math.BigDecimal.valueOf(alert.getTargetRate())) <= 0) {
                sendExchangeRateNotification(alert, rate);
            }
        }
    }

    private void sendExchangeRateNotification(com.tripass.exchange.domain.ExchangeRateAlert alert, java.math.BigDecimal rate) {
        NotificationSetting setting = notificationSettingMapper.getSettingByUserId(alert.getUserId());
        // isExchangeRateEnabled 필드가 있다고 가정
        if (setting == null || !setting.isAllEnabled() || !setting.isExchangeRateEnabled()) return; 

        // 통화 코드 가져오기
        String currencyCode = exchangeRateService.getCurrencyCodeById(alert.getCurrencyId());

        String title = "관심 환율 알림";
        String body = String.format("[%s] 설정하신 환율(%s원) 이하로 도달했습니다: %s원", currencyCode, alert.getTargetRate(), rate);
        String url = "/exchange";

        if (!notificationService.existsNotification(alert.getUserId(), "EXCHANGE", url, body)) {
            fcmService.sendNotification(alert.getUserId(), title, body);
            notificationService.insertNotification(alert.getUserId(), "EXCHANGE", title, body, url);
        }
    }

    private void sendChecklistNotification(int dDay, String title, String body) {
        List<Trip> trips = notificationTargetMapper.findTripsByStartDateOffset(dDay);
        for (Trip trip : trips) {
            String url = "/mypage/checklists/preparation?tripId=" + trip.getId();
            sendToTrip(trip, "CHECKLIST", title, body, url);
        }
    }

    private void sendReturnNotification(int offsetDays, String title, String body) {
        List<Trip> trips = notificationTargetMapper.findTripsByEndDateOffset(offsetDays);
        for (Trip trip : trips) {
            String url = "/mypage/checklists/return?tripId=" + trip.getId();
            sendToTrip(trip, "CHECKLIST", title, body, url);
        }
    }

    private void sendToTrip(Trip trip, String type, String title, String body, String url) {
        NotificationSetting setting = notificationSettingMapper.getSettingByUserId(trip.getUserId());
        if (setting == null || !setting.isAllEnabled()) return;
        if (!isWithinAllowedTime(setting)) return;

        boolean enabled = false;
        if ("CHECKLIST".equals(type) && setting.isChecklistEnabled()) enabled = true;
        else if ("REPORT".equals(type) && setting.isTravelReportEnabled()) enabled = true;
        else if ("SCHEDULE".equals(type) && setting.isTravelScheduleEnabled()) enabled = true;

        if (enabled) {
            if (!notificationService.existsNotification(trip.getUserId(), type, url, body)) {
                fcmService.sendNotification(trip.getUserId(), title, body);
                notificationService.insertNotification(trip.getUserId(), type, title, body, url);
            }
        }
    }

    private boolean isWithinAllowedTime(NotificationSetting setting) {
        LocalTime start = setting.getQuietStartTime();
        LocalTime end = setting.getQuietEndTime();
        if (start == null || end == null) return true;

        LocalTime now = LocalTime.now();
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    @Scheduled(cron = "0 0 1 1 * *", zone = "Asia/Seoul")
    public void generateMonthlyAnalysisReports() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("월간 소비 분석 리포트 자동 생성 시작: {}월", previousMonth);

        List<Long> userIds = assetMapper.findUserIdsWithLinkedAssets();
        int successCount = 0;
        int failCount = 0;

        for (Long userId : userIds) {
            try {
                monthlySpendingAnalysisService.generateMonthlyAnalysis(userId, previousMonth);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("월간 분석 리포트 생성 실패 - userId: {}, 사유: {}", userId, e.getMessage());
            }
        }

        log.info("월간 소비 분석 리포트 생성 완료: 성공 {}명, 실패 {}명", successCount, failCount);
    }

    // 1분마다 실행
    @Scheduled(cron = "0 */1 * * * *")
    public void sendScheduleReminders() {
        log.info("여행 일정 리마인더 스케줄러 시작");

        List<TripSchedule> schedules = scheduleMapper.findUpcomingSchedules();

        for (TripSchedule schedule : schedules) {
            NotificationSetting setting = notificationSettingMapper.getSettingByUserId(schedule.getUserId());
            if (setting != null && setting.isAllEnabled() && setting.isTravelScheduleEnabled()) {
                String title = schedule.getScheduleName() + " 1시간 전!";
                String body = "곧 시작되는 일정을 위해 미리 준비하세요.";
                String url = "/schedule";

                if (!notificationService.existsNotification(schedule.getUserId(), "SCHEDULE", url, body)) {
                    fcmService.sendNotification(schedule.getUserId(), title, body);
                    notificationService.insertNotification(schedule.getUserId(), "SCHEDULE", title, body, url);
                }
            }
        }
    }
}

