package com.tripass.saving.analysis;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Component
public class MissionStartWeekPolicy {

    public MissionStartResult resolve(YearMonth targetYearMonth, LocalDate selectedDate) {
        if (!YearMonth.from(selectedDate).equals(targetYearMonth)) {
            throw new IllegalArgumentException("미션 적용월에서만 미션을 시작할 수 있습니다.");
        }

        int day = selectedDate.getDayOfMonth();
        if (day > 28) {
            throw new IllegalArgumentException("29일 이후에는 이번 달 미션을 시작할 수 없습니다.");
        }

        int currentWeek = (day - 1) / 7 + 1;
        int weekEndDay = currentWeek * 7;
        int remainingDays = weekEndDay - day + 1;

        if (remainingDays >= 4) {
            return new MissionStartResult(currentWeek, remainingDays, selectedDate);
        }

        int nextWeek = currentWeek + 1;
        if (nextWeek > 4) {
            throw new IllegalArgumentException("이번 달 남은 기간이 부족하여 미션을 시작할 수 없습니다.");
        }
        int nextWeekStartDay = (nextWeek - 1) * 7 + 1;
        return new MissionStartResult(nextWeek, 7, targetYearMonth.atDay(nextWeekStartDay));
    }

    public record MissionStartResult(int startWeek, int eligibleDayCount, LocalDate missionStartDate) {
    }
}
