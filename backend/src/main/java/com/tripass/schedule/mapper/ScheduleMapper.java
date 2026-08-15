package com.tripass.schedule.mapper;


import com.tripass.schedule.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScheduleMapper {

    Long findTripUserId(@Param("tripId") Long tripId);

    List<ScheduleListRowDto> findAllByTripId(
            @Param("tripId") Long tripId
    );

    ScheduleDetailRowDto findDetailByTripIdAndScheduleId(
            @Param("tripId") Long tripId,
            @Param("scheduleId") Long scheduleId
    );

    TripCountryContextRowDto findTripCountryContext(
            @Param("tripId") Long tripId,
            @Param("tripCountryId") Long tripCountryId
    );

    Long findCurrencyIdByCode(
            @Param("currencyCode") String currencyCode
    );

    int insertSchedule(
            ScheduleCreateCommandDto command
    );

    boolean existsScheduleByTripIdAndScheduleId(
            @Param("tripId") Long tripId,
            @Param("scheduleId") Long scheduleId
    );

    int updateSchedule(
            ScheduleUpdateCommandDto command
    );

    int softDeleteSchedule(
            @Param("tripId") Long tripId,
            @Param("scheduleId") Long scheduleId
    );
}
