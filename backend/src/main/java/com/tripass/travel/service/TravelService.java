package com.tripass.travel.service;

import com.tripass.checklist.dto.ChecklistGroupResponseDto;
import com.tripass.checklist.dto.ChecklistResponseDto;
import com.tripass.checklist.dto.ChecklistSummaryResponseDto;
import com.tripass.travel.domain.Trip;
import com.tripass.travel.dto.*;
import com.tripass.travel.exception.TravelErrorCode;
import com.tripass.travel.exception.TravelException;
import com.tripass.travel.mapper.TravelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    /** 여행 목표를 생성하고, 국가별 방문 일정만 먼저 저장합니다. */
    @Transactional
    public TripGoalCreateResponseDto createTripGoal(
            Long currentUserId,
            TripGoalCreateRequestDto request
    ) {
        if (travelMapper.existsActiveTripByUserId(currentUserId)) {
            throw new TravelException(TravelErrorCode.TRIP_ALREADY_EXISTS);
        }

        TripDateRange dateRange = validateCountries(request.getCountries());

        TripGoalCommandDto command = TripGoalCommandDto.builder()
                .userId(currentUserId)
                .tripName(request.getTripName().trim())
                .startDate(dateRange.startDate())
                .endDate(dateRange.endDate())
                .build();
        travelMapper.insertTripGoal(command);
        insertTripCountries(command.getId(), request.getCountries());

        return TripGoalCreateResponseDto.builder()
                .tripId(command.getId())
                .build();
    }

    /** 여행 목표와 국가 방문 순서를 수정합니다. 여행 시작 전 계획 단계에서만 허용합니다. */
    @Transactional
    public TripGoalResponseDto updateTripGoal(
            Long tripId,
            Long currentUserId,
            TripGoalUpdateRequestDto request
    ) {
        validateTripOwner(tripId, currentUserId);
        if (!"PLANNING".equals(travelMapper.findTripStatus(tripId))) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_EDITABLE);
        }

        TripDateRange dateRange = validateCountries(request.getCountries());
        travelMapper.updateTripGoal(TripGoalCommandDto.builder()
                .id(tripId)
                .tripName(request.getTripName().trim())
                .startDate(dateRange.startDate())
                .endDate(dateRange.endDate())
                .build());
        travelMapper.softDeleteTripCountries(tripId);
        insertTripCountries(tripId, request.getCountries());

        return getTripGoal(tripId, currentUserId);
    }

    /** 로그인 사용자의 진행 중 여행 목표를 조회합니다. */
    public TripGoalResponseDto getActiveTripGoal(Long currentUserId) {
        TripGoalResponseDto response = travelMapper.findActiveTripGoalByUserId(currentUserId);
        if (response == null) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_FOUND, "진행 중인 여행 목표가 없습니다.");
        }
        response.setCountries(travelMapper.findTripGoalCountries(response.getTripId()));
        return response;
    }

    /** 특정 여행 목표와 선택 국가 목록을 조회합니다. */
    public TripGoalResponseDto getTripGoal(Long tripId, Long currentUserId) {
        validateTripOwner(tripId, currentUserId);
        TripGoalResponseDto response = travelMapper.findTripGoalById(tripId);
        if (response == null) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_FOUND);
        }
        response.setCountries(travelMapper.findTripGoalCountries(tripId));
        return response;
    }

    /** 국가·통화 선택 모달에서 사용하는 국가 목록을 조회합니다. */
    public List<TripCountryCatalogResponseDto> getCountries(String keyword) {
        return travelMapper.findCountries(keyword == null ? null : keyword.trim());
    }

    private void insertTripCountries(Long tripId, List<TripCountryRequestDto> countries) {
        countries.stream()
                .sorted(Comparator.comparing(TripCountryRequestDto::getDisplayOrder))
                .forEach(country -> travelMapper.insertTripCountry(TripCountryCommandDto.builder()
                        .tripId(tripId)
                        .countryId(country.getCountryId())
                        .arrivalDate(country.getArrivalDate())
                        .departureDate(country.getDepartureDate())
                        .targetBudget(BigDecimal.ZERO)
                        .displayOrder(country.getDisplayOrder())
                        .build()));
    }

    /**
     * 국가 중복, 방문 순서, 날짜 겹침을 서버에서 검증합니다.
     * 프론트에서 날짜를 강조 표시하더라도, 저장 시점에 다시 검증해야 잘못된 일정이 남지 않습니다.
     */
    private TripDateRange validateCountries(List<TripCountryRequestDto> countries) {
        List<TripCountryRequestDto> sortedCountries = countries.stream()
                .sorted(Comparator.comparing(TripCountryRequestDto::getDisplayOrder))
                .collect(Collectors.toList());
        Set<Long> countryIds = new HashSet<>();
        LocalDate previousDepartureDate = null;

        for (int index = 0; index < sortedCountries.size(); index++) {
            TripCountryRequestDto country = sortedCountries.get(index);
            if (!countryIds.add(country.getCountryId())) {
                throw new TravelException(TravelErrorCode.DUPLICATE_TRIP_COUNTRY);
            }
            if (!travelMapper.existsCountryById(country.getCountryId())) {
                throw new TravelException(TravelErrorCode.COUNTRY_NOT_FOUND);
            }
            if (!country.getArrivalDate().isBefore(country.getDepartureDate())) {
                throw new TravelException(TravelErrorCode.INVALID_TRIP_DATE_RANGE);
            }
            if (country.getDisplayOrder() != index + 1 ||
                    (previousDepartureDate != null && country.getArrivalDate().isBefore(previousDepartureDate))) {
                throw new TravelException(TravelErrorCode.INVALID_TRIP_COUNTRY_ORDER);
            }
            previousDepartureDate = country.getDepartureDate();
        }

        return new TripDateRange(
                sortedCountries.get(0).getArrivalDate(),
                sortedCountries.get(sortedCountries.size() - 1).getDepartureDate()
        );
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

    private record TripDateRange(LocalDate startDate, LocalDate endDate) {
    }

}
