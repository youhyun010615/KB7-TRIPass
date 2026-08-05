package com.tripass.schedule.mapper;


import com.tripass.schedule.dto.ScheduleDetailRowDto;
import com.tripass.schedule.dto.ScheduleListRowDto;
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
}
