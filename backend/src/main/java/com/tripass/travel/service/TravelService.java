package com.tripass.travel.service;

import com.tripass.checklist.dto.ChecklistGroupResponseDto;
import com.tripass.checklist.dto.ChecklistResponseDto;
import com.tripass.checklist.dto.ChecklistSummaryResponseDto;
import com.tripass.travel.domain.Trip;
import com.tripass.travel.domain.TripCountry;
import com.tripass.travel.domain.TripStatus;
import com.tripass.travel.dto.*;
import com.tripass.travel.exception.TravelErrorCode;
import com.tripass.travel.exception.TravelException;
import com.tripass.travel.mapper.TravelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelService {

    private final TravelMapper travelMapper;

    /**
     * 여행 대시보드 상태 조회
     */
    public TravelStatusResponseDto getTravelStatus(Long tripId, Long currentUserId) {
        validateTripOwner(tripId, currentUserId);

        TravelStatusResponseDto result = travelMapper.getTripDashboard(tripId);
        if (result == null) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_FOUND, "해당 여행 정보를 찾을 수 없습니다. (id: " + tripId + ")");
        }
        return result;
    }

    /**
     * 여행 자금 체크 조회
     */
    public BudgetCheckResponseDto getTripBudget(Long tripId, String scope, Long countryId, Long currentUserId) {
        if ("COUNTRY".equals(scope) && countryId == null) {
            throw new TravelException(TravelErrorCode.MISSING_COUNTRY_ID);
        }

        validateTripOwner(tripId, currentUserId);

        return travelMapper.getTripBudget(tripId, scope, countryId);
    }

    /**
     * 여행 등록: 여행 이름과 국가별 방문 기간·목표 예산을 받아 trips, trip_countries를 생성한다.
     * 여행 시작/종료일과 목표 총액은 국가별 입력값으로부터 계산한다.
     */
    @Transactional
    public TripCreateResponseDto createTrip(Long userId, TripCreateRequestDto request) {
        List<TripCountryCreateRequestDto> countries = request.getCountries();

        for (TripCountryCreateRequestDto country : countries) {
            if (country.getEndDate().isBefore(country.getStartDate())) {
                throw new TravelException(TravelErrorCode.INVALID_TRIP_COUNTRY_DATES);
            }
        }

        LocalDate tripStartDate = countries.stream()
                .map(TripCountryCreateRequestDto::getStartDate)
                .min(Comparator.naturalOrder())
                .orElseThrow();

        LocalDate tripEndDate = countries.stream()
                .map(TripCountryCreateRequestDto::getEndDate)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        BigDecimal totalTargetAmount = countries.stream()
                .map(TripCountryCreateRequestDto::getTargetBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Trip trip = Trip.builder()
                .userId(userId)
                .tripName(request.getTripName())
                .status(TripStatus.PLANNING.name())
                .startDate(tripStartDate)
                .endDate(tripEndDate)
                .totalTargetAmount(totalTargetAmount)
                .build();

        travelMapper.insertTrip(trip);

        List<TripCountrySummaryDto> countrySummaries = new ArrayList<>();
        int displayOrder = 1;
        for (TripCountryCreateRequestDto countryRequest : countries) {
            Long countryId = travelMapper.findCountryIdByName(countryRequest.getCountryName());

            if (countryId == null) {
                throw new TravelException(
                        TravelErrorCode.COUNTRY_NOT_FOUND,
                        "국가 정보를 찾을 수 없습니다: " + countryRequest.getCountryName()
                );
            }

            TripCountry tripCountry = TripCountry.builder()
                    .tripId(trip.getId())
                    .countryId(countryId)
                    .arrivalDate(countryRequest.getStartDate())
                    .departureDate(countryRequest.getEndDate())
                    .targetBudget(countryRequest.getTargetBudget())
                    .displayOrder(displayOrder++)
                    .build();

            travelMapper.insertTripCountry(tripCountry);

            countrySummaries.add(
                    TripCountrySummaryDto.builder()
                            .tripCountryId(tripCountry.getId())
                            .countryName(countryRequest.getCountryName())
                            .build()
            );
        }

        return TripCreateResponseDto.builder()
                .countries(countrySummaries)
                .tripId(trip.getId())
                .build();
    }

    /**
     * 로그인 사용자의 가장 최근 여행을 조회한다. 프론트엔드는 이 API로 서버에 이미 등록된
     * 여행(tripId, 국가별 tripCountryId 등)을 부팅 시점에 알아내 로컬 상태를 채운다.
     * 등록된 여행이 없으면 null을 반환한다.
     */
    public TripCurrentResponseDto getCurrentTrip(Long userId) {
        Trip trip = travelMapper.findLatestTripByUserId(userId);

        if (trip == null) {
            return null;
        }

        List<TripCountryDetailDto> countries = travelMapper.findTripCountryDetailsByTripId(trip.getId());

        return TripCurrentResponseDto.builder()
                .tripId(trip.getId())
                .tripName(trip.getTripName())
                .status(trip.getStatus())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .totalTargetAmount(trip.getTotalTargetAmount())
                .countries(countries)
                .build();
    }

    /**
     * 공통 여행 ID 유효성 & 작성자(소유자) 권한 검증 메서드
     */
    private void validateTripOwner(Long tripId, Long currentUserId) {
        // 1. Path Variable 기본 유효성 검증
        if (tripId == null || tripId <= 0) {
            throw new TravelException(TravelErrorCode.INVALID_PATH_VARIABLE);
        }

        // 2. 여행 소유자(user_id) DB 조회
        Long ownerId = travelMapper.selectUserIdByTripId(tripId);

        // 2-1. 존재하지 않는 여행인 경우 (404 Not Found)
        if (ownerId == null) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_FOUND);
        }

        // 2-2. 타인의 여행 정보에 접근하려는 경우 (403 Forbidden)
        if (!ownerId.equals(currentUserId)) {
            throw new TravelException(TravelErrorCode.FORBIDDEN_TRIP_ACCESS);
        }
    }

}