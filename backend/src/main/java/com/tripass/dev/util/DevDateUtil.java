package com.tripass.dev.util;

import com.tripass.dev.mapper.DevDateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
}
