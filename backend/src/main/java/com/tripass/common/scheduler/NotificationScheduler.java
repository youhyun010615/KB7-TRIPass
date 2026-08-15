package com.tripass.common.scheduler;

import com.tripass.common.util.FcmService;
import com.tripass.mypage.domain.NotificationSetting;
import com.tripass.mypage.mapper.NotificationSettingMapper;
import com.tripass.mypage.mapper.NotificationTargetMapper;
import com.tripass.mypage.service.NotificationService;
import com.tripass.schedule.domain.TripSchedule;
import com.tripass.schedule.mapper.ScheduleMapper;
import com.tripass.travel.domain.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    // 매일 자정
    @Scheduled(cron = "0 0 0 * * *")
    public void sendDailyNotifications() {
        log.info("일일 알림 발송 스케줄러 시작");
        
        sendChecklistNotification(30, "여행까지 한 달!", "준비를 시작할 때예요.");
        sendChecklistNotification(7, "여행 일주일 전!", "꼼꼼하게 준비하고 계신가요?");
        sendChecklistNotification(1, "여행 D-1", "마지막 준비는 완벽한가요?");
        sendReturnNotification(1, "여행의 마무리", "안전한 귀국을 위해 체크리스트를 확인하세요.");
        
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
        if (setting != null && (setting.isAllEnabled() || setting.isChecklistEnabled())) {
            if (!notificationService.existsNotification(trip.getUserId(), type, url, body)) {
                fcmService.sendNotification(trip.getUserId(), title, body);
                notificationService.insertNotification(trip.getUserId(), type, title, body, url);
            }
        }
    }

    // 10분마다 실행
    @Scheduled(cron = "0 */10 * * * *")
    public void sendScheduleReminders() {
        log.info("여행 일정 리마인더 스케줄러 시작");

        List<TripSchedule> schedules = scheduleMapper.findUpcomingSchedules();

        for (TripSchedule schedule : schedules) {
            NotificationSetting setting = notificationSettingMapper.getSettingByUserId(schedule.getUserId());
            if (setting != null && (setting.isAllEnabled() || setting.isTravelScheduleEnabled())) {
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

