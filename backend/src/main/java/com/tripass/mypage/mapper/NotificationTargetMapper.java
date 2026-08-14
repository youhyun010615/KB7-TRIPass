package com.tripass.mypage.mapper;

import com.tripass.travel.domain.Trip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NotificationTargetMapper {
    // start_date와 D-Day가 일치하는 여행 목록 조회
    List<Trip> findTripsByStartDateOffset(@Param("offsetDays") int offsetDays);
    
    // end_date(귀국일)와 일치하는 여행 목록 조회 (귀국 전날은 end_date - 1)
    List<Trip> findTripsByEndDateOffset(@Param("offsetDays") int offsetDays);
}
