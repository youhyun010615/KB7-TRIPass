package com.tripass.travel.service;

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
import java.math.RoundingMode;
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

    /**
     * 여행/저축 모드 전환
     */
    @Transactional
    public TravelModeResponseDto toggleTravelMode(Long tripId, TravelModeRequestDto request, Long currentUserId) {
        // 1. Request Body 기본 검증 (400 Bad Request)
        if (request == null || request.getIsTravelMode() == null) {
            throw new TravelException(TravelErrorCode.INVALID_INPUT_VALUE);
        }

        // 2. 여행 존재 여부 및 소유권 검증 (400, 404, 403)
        Trip trip = validateTripOwnerAndGetTrip(tripId, currentUserId);

        // 3. 여행 기간 검증 (start_date <= Today <= end_date)
        LocalDate today = LocalDate.now();
        if (today.isBefore(trip.getStartDate()) || today.isAfter(trip.getEndDate())) {
            throw new TravelException(TravelErrorCode.INVALID_TRIP_PERIOD);
        }

        // 4. [추가] 이미 현재 요청된 모드와 동일한 상태인지 검증
        String targetViewMode = Boolean.TRUE.equals(request.getIsTravelMode()) ? "TRAVEL" : "SAVING";

        // 유저의 현재 모드 조회 (User 테이블 조회 또는 Auth 토큰/유저 객체 기반)
        String currentViewMode = travelMapper.selectUserCurrentViewMode(currentUserId);

        if (targetViewMode.equals(currentViewMode)) {
            throw new TravelException(TravelErrorCode.ALREADY_IN_TARGET_MODE,
                    "이미 " + (Boolean.TRUE.equals(request.getIsTravelMode()) ? "여행" : "저축") + " 모드 상태입니다.");
        }

        // 5. 회원(users.current_view_mode) 화면 모드 컬럼 업데이트
        travelMapper.updateUserCurrentViewMode(currentUserId, targetViewMode);

        // 6. Response 반환
        return TravelModeResponseDto.builder()
                .id(tripId)
                .isTravelMode(request.getIsTravelMode())
                .currentViewMode(targetViewMode)
                .build();
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
        travelMapper.insertTripWalletIfAbsent(currentUserId);

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

    /** 여행 등록 후 저축 홈에서 사용하는 활성 여행 요약 정보를 조회합니다. */
    public TripHomeDashboardResponseDto getActiveTripHome(Long currentUserId) {
        TripGoalResponseDto trip = getActiveTripGoal(currentUserId);
        BigDecimal totalTarget = defaultZero(trip.getTotalTargetAmount());
        BigDecimal walletBalance = defaultZero(travelMapper.findTripWalletBalanceByUserId(currentUserId));
        BigDecimal remainingTarget = totalTarget.subtract(walletBalance).max(BigDecimal.ZERO);
        BigDecimal progressPercent = totalTarget.signum() == 0
                ? BigDecimal.ZERO
                : walletBalance.multiply(BigDecimal.valueOf(100))
                        .divide(totalTarget, 2, RoundingMode.HALF_UP)
                        .min(BigDecimal.valueOf(100));
        int remainingMonths = TripSavingCalculator.calculateRemainingMonths(trip.getStartDate(), LocalDate.now());
        BigDecimal monthlySavingTarget = defaultZero(travelMapper.findMonthlySavingAmountByTripId(trip.getTripId()));

        List<TripHomeCountryResponseDto> countries = trip.getCountries().stream()
                .map(country -> TripHomeCountryResponseDto.builder()
                        .tripCountryId(country.getTripCountryId())
                        .countryId(country.getCountryId())
                        .countryName(country.getCountryName())
                        .currencyCode(country.getCurrencyCode())
                        .arrivalDate(country.getArrivalDate())
                        .departureDate(country.getDepartureDate())
                        .displayOrder(country.getDisplayOrder())
                        .targetBudget(defaultZero(country.getTargetBudget()))
                        .targetSharePercent(calculateTargetShare(country.getTargetBudget(), totalTarget))
                        .build())
                .collect(Collectors.toList());

        return TripHomeDashboardResponseDto.builder()
                .tripId(trip.getTripId())
                .tripName(trip.getTripName())
                .status(trip.getStatus())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .daysUntilDeparture(Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), trip.getStartDate())))
                .totalTargetAmount(totalTarget)
                .prepaidExpenseTotal(defaultZero(travelMapper.findPrepaidExpenseTotalByTripId(trip.getTripId())))
                .walletBalance(walletBalance)
                .remainingTargetAmount(remainingTarget)
                .savingProgressPercent(progressPercent)
                .remainingMonths(remainingMonths)
                .monthlySavingTarget(monthlySavingTarget)
                .countries(countries)
                .build();
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

    /**
     * 국가별 조사 기준 단가와 일정으로 예산을 계산합니다.
     * 계산값은 재현 가능하며, 사용자가 확정하기 전에는 여행 목표 금액에 반영하지 않습니다.
     */
    @Transactional
    public TripBudgetRecommendationResponseDto generateBudgetRecommendations(Long tripId, Long currentUserId) {
        validateTripOwner(tripId, currentUserId);
        validateTripIsPlanning(tripId);

        List<TripCountryBudgetContextDto> contexts = travelMapper.findTripCountryBudgetContexts(tripId);
        if (contexts.isEmpty()) {
            throw new TravelException(TravelErrorCode.COUNTRY_NOT_FOUND);
        }

        for (TripCountryBudgetContextDto context : contexts) {
            long tripDays = ChronoUnit.DAYS.between(context.getArrivalDate(), context.getDepartureDate()) + 1;
            long stayNights = ChronoUnit.DAYS.between(context.getArrivalDate(), context.getDepartureDate());
            CountryBudgetBaselineDto baseline = travelMapper.findCountryBudgetBaseline(context.getCountryId());
            if (baseline == null) {
                throw new TravelException(TravelErrorCode.COUNTRY_BUDGET_BASELINE_NOT_FOUND,
                        context.getCountryName() + "의 여행 예산 기준값이 아직 준비되지 않았습니다.");
            }
            AiBudgetResultDto aiResult = CountryBudgetCalculator.calculate(baseline, tripDays, stayNights);
            travelMapper.upsertTripBudgetRecommendation(TripBudgetRecommendationCommandDto.builder()
                    .tripCountryId(context.getTripCountryId())
                    .travelerCount(1)
                    .travelStyle("MID_RANGE")
                    .airfareAmount(aiResult.getAirfareAmount())
                    .lodgingAmount(aiResult.getLodgingAmount())
                    .activityAmount(aiResult.getActivityAmount())
                    .transportAmount(aiResult.getTransportAmount())
                    .foodAmount(aiResult.getFoodAmount())
                    .otherAmount(aiResult.getOtherAmount())
                    .aiReason(limitReason(aiResult.getReason()))
                    .aiModel(aiResult.getModel())
                    .build());
        }

        return toRecommendationResponse(tripId, travelMapper.findBudgetRecommendationsByTripId(tripId));
    }

    /** 여행 목표 등록 화면에서 저장된 AI 추천 예산을 다시 조회합니다. */
    public TripBudgetRecommendationResponseDto getBudgetRecommendations(Long tripId, Long currentUserId) {
        validateTripOwner(tripId, currentUserId);
        return toRecommendationResponse(tripId, travelMapper.findBudgetRecommendationsByTripId(tripId));
    }

    /**
     * 사용자가 AI 추천을 수정·확정하면 현지 사용 금액만 여행 목표와 월 저축 계획에 반영합니다.
     * 비행기·숙소는 사전지출로 남겨 여행 리포트에는 기록하지만, 여행 저축 목표에는 포함하지 않습니다.
     */
    @Transactional
    public TripGoalCompletionResponseDto confirmBudgetRecommendations(
            Long tripId,
            Long currentUserId,
            TripBudgetConfirmRequestDto request
    ) {
        validateTripOwner(tripId, currentUserId);
        validateTripIsPlanning(tripId);

        List<TripCountryBudgetContextDto> contexts = travelMapper.findTripCountryBudgetContexts(tripId);
        Set<Long> savedCountryIds = contexts.stream()
                .map(TripCountryBudgetContextDto::getTripCountryId)
                .collect(Collectors.toSet());
        Set<Long> requestCountryIds = request.getCountries().stream()
                .map(CountryBudgetConfirmRequestDto::getTripCountryId)
                .collect(Collectors.toSet());

        if (requestCountryIds.size() != request.getCountries().size() || !savedCountryIds.equals(requestCountryIds)) {
            throw new TravelException(TravelErrorCode.BUDGET_NOT_READY,
                    "모든 여행 국가의 예산을 한 번씩 확정해 주세요.");
        }

        BigDecimal prepaidExpenseTotal = BigDecimal.ZERO;
        BigDecimal localTravelTargetTotal = BigDecimal.ZERO;

        for (CountryBudgetConfirmRequestDto country : request.getCountries()) {
            BigDecimal localTravelTarget = country.getActivityAmount()
                    .add(country.getTransportAmount())
                    .add(country.getFoodAmount())
                    .add(country.getOtherAmount());
            TripBudgetConfirmCommandDto command = TripBudgetConfirmCommandDto.builder()
                    .tripCountryId(country.getTripCountryId())
                    .airfareAmount(country.getAirfareAmount())
                    .lodgingAmount(country.getLodgingAmount())
                    .activityAmount(country.getActivityAmount())
                    .transportAmount(country.getTransportAmount())
                    .foodAmount(country.getFoodAmount())
                    .otherAmount(country.getOtherAmount())
                    .localTravelTarget(localTravelTarget)
                    .build();
            if (travelMapper.updateConfirmedTripBudget(command) == 0) {
                throw new TravelException(TravelErrorCode.BUDGET_NOT_READY);
            }
            travelMapper.updateTripCountryTargetBudget(command);
            prepaidExpenseTotal = prepaidExpenseTotal
                    .add(country.getAirfareAmount())
                    .add(country.getLodgingAmount());
            localTravelTargetTotal = localTravelTargetTotal.add(localTravelTarget);
        }

        travelMapper.updateTripTargetAmount(tripId, localTravelTargetTotal);
        travelMapper.insertTripWalletIfAbsent(currentUserId);
        BigDecimal walletBalance = defaultZero(travelMapper.findTripWalletBalanceByUserId(currentUserId));
        TripGoalResponseDto trip = travelMapper.findTripGoalById(tripId);
        int remainingMonths = TripSavingCalculator.calculateRemainingMonths(trip.getStartDate(), LocalDate.now());
        BigDecimal monthlySavingTarget = TripSavingCalculator.calculateMonthlySavingTarget(
                localTravelTargetTotal, walletBalance, remainingMonths
        );
        saveMonthlySavingPlan(tripId, monthlySavingTarget);

        return TripGoalCompletionResponseDto.builder()
                .tripId(tripId)
                .prepaidExpenseTotal(prepaidExpenseTotal)
                .localTravelTargetTotal(localTravelTargetTotal)
                .currentWalletBalance(walletBalance)
                .remainingMonths(remainingMonths)
                .monthlySavingTarget(monthlySavingTarget)
                .countries(travelMapper.findBudgetRecommendationsByTripId(tripId))
                .build();
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

    private TripBudgetRecommendationResponseDto toRecommendationResponse(
            Long tripId,
            List<CountryBudgetRecommendationResponseDto> countries
    ) {
        BigDecimal prepaidExpenseTotal = BigDecimal.ZERO;
        BigDecimal localTravelTargetTotal = BigDecimal.ZERO;
        for (CountryBudgetRecommendationResponseDto country : countries) {
            BigDecimal airfare = selectedAmount(country.getConfirmedAirfareAmount(), country.getRecommendedAirfareAmount());
            BigDecimal lodging = selectedAmount(country.getConfirmedLodgingAmount(), country.getRecommendedLodgingAmount());
            BigDecimal activity = selectedAmount(country.getConfirmedActivityAmount(), country.getRecommendedActivityAmount());
            BigDecimal transport = selectedAmount(country.getConfirmedTransportAmount(), country.getRecommendedTransportAmount());
            BigDecimal food = selectedAmount(country.getConfirmedFoodAmount(), country.getRecommendedFoodAmount());
            BigDecimal other = selectedAmount(country.getConfirmedOtherAmount(), country.getRecommendedOtherAmount());
            prepaidExpenseTotal = prepaidExpenseTotal.add(airfare).add(lodging);
            localTravelTargetTotal = localTravelTargetTotal.add(activity).add(transport).add(food).add(other);
        }
        return TripBudgetRecommendationResponseDto.builder()
                .tripId(tripId)
                .prepaidExpenseTotal(prepaidExpenseTotal)
                .localTravelTargetTotal(localTravelTargetTotal)
                .countries(countries)
                .build();
    }

    private void validateTripIsPlanning(Long tripId) {
        if (!"PLANNING".equals(travelMapper.findTripStatus(tripId))) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_EDITABLE);
        }
    }

    private void saveMonthlySavingPlan(Long tripId, BigDecimal monthlySavingTarget) {
        Long savingPlanId = travelMapper.findSavingPlanIdByTripId(tripId);
        if (savingPlanId == null) {
            travelMapper.insertSavingPlan(tripId, monthlySavingTarget);
            return;
        }
        travelMapper.updateSavingPlanMonthlyAmount(savingPlanId, monthlySavingTarget);
    }

    private BigDecimal selectedAmount(BigDecimal confirmedAmount, BigDecimal recommendedAmount) {
        return confirmedAmount != null ? confirmedAmount : defaultZero(recommendedAmount);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calculateTargetShare(BigDecimal countryTarget, BigDecimal totalTarget) {
        if (totalTarget.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return defaultZero(countryTarget).multiply(BigDecimal.valueOf(100))
                .divide(totalTarget, 2, RoundingMode.HALF_UP);
    }

    private String limitReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "여행 기간과 국가별 일반적인 소비 수준을 기준으로 추천했습니다.";
        }
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
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

    /**
     * 여행 소유권 검증 후 Trip 엔티티 반환 헬퍼 메서드
     */
    private Trip validateTripOwnerAndGetTrip(Long tripId, Long currentUserId) {
        // 1. Path Variable 기본 유효성 검증 (400 Bad Request)
        if (tripId == null || tripId <= 0) {
            throw new TravelException(TravelErrorCode.INVALID_PATH_VARIABLE);
        }

        // 2. 여행 정보 조회 (404 Not Found)
        Trip trip = travelMapper.selectTripById(tripId);
        if (trip == null) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_FOUND);
        }

        // 3. 타인의 여행 정보에 접근하려는 경우 검증 (403 Forbidden)
        if (!trip.getUserId().equals(currentUserId)) {
            throw new TravelException(TravelErrorCode.FORBIDDEN_TRIP_ACCESS);
        }

        return trip;
    }

    private record TripDateRange(LocalDate startDate, LocalDate endDate) {
    }

}
