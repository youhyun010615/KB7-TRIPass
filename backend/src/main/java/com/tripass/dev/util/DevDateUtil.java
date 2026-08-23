package com.tripass.dev.util;

import com.tripass.dev.mapper.DevDateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class DevDateUtil {

    private final DevDateMapper devDateMapper;

    public LocalDate today(Long userId) {
        if (userId == null) {
            return LocalDate.now();
        }
        LocalDate override = devDateMapper.selectOverrideDate(userId);
        return override != null ? override : LocalDate.now();
    }

    public LocalDateTime getEffectiveDateTime(Long userId) {
        LocalDate effectiveDate = today(userId);
        LocalTime currentTime = LocalTime.now();
        return LocalDateTime.of(effectiveDate, currentTime);
    }

    public boolean isOverridden(Long userId) {
        return userId != null && devDateMapper.selectOverrideDate(userId) != null;
    }
}
