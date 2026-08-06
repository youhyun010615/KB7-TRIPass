package com.tripass.schedule.mapper;


import com.tripass.schedule.dto.ScheduleCreateCommandDto;
import com.tripass.schedule.dto.ScheduleDetailRowDto;
import com.tripass.schedule.dto.ScheduleListRowDto;
import com.tripass.schedule.dto.TripCountryContextRowDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScheduleMapper {

    boolean existsTripById(@Param("tripId") Long tripId);

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
}
