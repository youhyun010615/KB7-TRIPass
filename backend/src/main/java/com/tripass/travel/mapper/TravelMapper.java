package com.tripass.travel.mapper;

import com.tripass.checklist.dto.ChecklistResponseDto;
import com.tripass.checklist.dto.ChecklistSummaryResponseDto;
import com.tripass.travel.domain.Trip;
import com.tripass.travel.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface TravelMapper {
    TravelStatusResponseDto getTripDashboard(@Param("tripId") Long tripId);
    
    BudgetCheckResponseDto getTripBudget(@Param("tripId") Long tripId, @Param("scope") String scope, @Param("countryId") Long countryId);

    // 여행 존재 여부 확인
    boolean existsByTripId(@Param("tripId") Long tripId);

    // 여행 소유자 ID 조회 (권한 검증용)
    Long selectUserIdByTripId(@Param("tripId") Long tripId);

    boolean existsActiveTripByUserId(@Param("userId") Long userId);

    void insertTripGoal(TripGoalCommandDto command);

    void insertTripCountry(TripCountryCommandDto command);

    TripGoalResponseDto findTripGoalById(@Param("tripId") Long tripId);

    List<TripCountryGoalResponseDto> findTripGoalCountries(@Param("tripId") Long tripId);

    TripGoalResponseDto findActiveTripGoalByUserId(@Param("userId") Long userId);

    List<TripCountryCatalogResponseDto> findCountries(@Param("keyword") String keyword);

    boolean existsCountryById(@Param("countryId") Long countryId);

    String findTripStatus(@Param("tripId") Long tripId);

    void updateTripGoal(TripGoalCommandDto command);

    void softDeleteTripCountries(@Param("tripId") Long tripId);

    List<TripCountryBudgetContextDto> findTripCountryBudgetContexts(@Param("tripId") Long tripId);

    CountryBudgetBaselineDto findCountryBudgetBaseline(@Param("countryId") Long countryId);

    void upsertTripBudgetRecommendation(TripBudgetRecommendationCommandDto command);

    List<CountryBudgetRecommendationResponseDto> findBudgetRecommendationsByTripId(@Param("tripId") Long tripId);

    int updateConfirmedTripBudget(TripBudgetConfirmCommandDto command);

    void updateTripCountryTargetBudget(TripBudgetConfirmCommandDto command);

    void updateTripTargetAmount(@Param("tripId") Long tripId, @Param("totalTargetAmount") BigDecimal totalTargetAmount);

    void insertTripWalletIfAbsent(@Param("userId") Long userId);

    BigDecimal findTripWalletBalanceByUserId(@Param("userId") Long userId);

    Long findSavingPlanIdByTripId(@Param("tripId") Long tripId);

    void insertSavingPlan(@Param("tripId") Long tripId, @Param("monthlyAmount") BigDecimal monthlyAmount);

    void updateSavingPlanMonthlyAmount(@Param("id") Long id, @Param("monthlyAmount") BigDecimal monthlyAmount);
}
