package com.tripass.travel.mapper;

import com.tripass.travel.dto.TravelStatusResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TravelMapper {
    TravelStatusResponseDto getTripDashboard(@Param("tripId") Long tripId);
}
