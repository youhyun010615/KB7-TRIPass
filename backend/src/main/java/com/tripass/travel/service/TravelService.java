package com.tripass.travel.service;

import com.tripass.common.exception.CustomException;
import com.tripass.travel.dto.BudgetCheckResponseDto;
import com.tripass.travel.dto.TravelStatusResponseDto;
import com.tripass.travel.mapper.TravelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TravelService {
    private final TravelMapper travelMapper;

    public BudgetCheckResponseDto getTripBudget(Long tripId, String scope, Long countryId) {
        if (tripId == null || tripId <= 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "여행 ID는 1 이상이어야 합니다.");
        }
        if ("COUNTRY".equals(scope) && countryId == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "scope가 COUNTRY인 경우 countryId는 필수입니다.");
        }
        return travelMapper.getTripBudget(tripId, scope, countryId);
    }
    
    public TravelStatusResponseDto getTravelStatus(Long tripId) {
        if (tripId == null || tripId <= 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "여행 ID는 1 이상이어야 합니다.");
        }
        TravelStatusResponseDto result = travelMapper.getTripDashboard(tripId);
        if (result == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "해당 여행 정보를 찾을 수 없습니다. (id: " + tripId + ")");
        }
        return result;
    }
}
