package com.tripass.schedule.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class TripSchedule {
    private Long id;
    private Long tripId;
    private Long userId; // JOIN을 통해 필요할 수도 있음
    private String scheduleName;
    private LocalDateTime scheduledAt;
}
