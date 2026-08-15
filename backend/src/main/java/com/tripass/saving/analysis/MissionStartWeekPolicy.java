package com.tripass.saving.analysis;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 미션을 선택한 날짜를 기준으로 시작 가능한 첫 고정 주차를 계산한다.
 * 주차 시작일(1·8·15·22일)에는 해당 주차를 바로 시작하고, 이미 주차가 진행 중이면 다음 온전한 주차부터 시작한다.
 */
@Component
public class MissionStartWeekPolicy {

    public int resolve(YearMonth targetYearMonth, LocalDate selectedDate) {
        if (!YearMonth.from(selectedDate).equals(targetYearMonth)) {
            throw new IllegalArgumentException("미션 적용월에서만 미션을 시작할 수 있습니다.");
        }

        int day = selectedDate.getDayOfMonth();
        if (day == 1) return 1;
        if (day <= 8) return 2;
        if (day <= 15) return 3;
        if (day <= 22) return 4;
        throw new IllegalArgumentException("23일 이후에는 이번 달 미션을 시작할 수 없습니다.");
    }
}
