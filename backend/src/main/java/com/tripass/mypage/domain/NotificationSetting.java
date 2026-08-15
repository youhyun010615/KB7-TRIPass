package com.tripass.mypage.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationSetting {
    private Long id;
    private Long userId;
    private boolean allEnabled;
    private boolean travelScheduleEnabled;
    private boolean exchangeRateEnabled;
    private boolean checklistEnabled;
    private boolean travelReportEnabled;
}
