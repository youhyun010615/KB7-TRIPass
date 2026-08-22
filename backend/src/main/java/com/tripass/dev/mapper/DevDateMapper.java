package com.tripass.dev.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface DevDateMapper {

    LocalDate selectOverrideDate(@Param("userId") Long userId);

    void updateOverrideDate(@Param("userId") Long userId, @Param("overrideDate") LocalDate overrideDate);

    void clearOverrideDate(@Param("userId") Long userId);
}
