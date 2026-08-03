package com.tripass.travel.service;

import com.tripass.common.exception.CustomException;
import com.tripass.travel.dto.TravelStatusResponseDto;
import com.tripass.travel.mapper.TravelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TravelService {
    private final TravelMapper travelMapper;

    public TravelStatusResponseDto getTravelStatus(Long tripId) {
        try {
            TravelStatusResponseDto result = travelMapper.getTripDashboard(tripId);
            if (result == null) {
                throw new CustomException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "해당 여행 정보를 찾을 수 없습니다. (id: " + tripId + ")");
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace(); // 서버 콘솔에 상세 에러 출력
            throw e;
        }
    }
}
