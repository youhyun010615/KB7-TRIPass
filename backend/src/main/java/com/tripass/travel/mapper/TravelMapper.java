package com.tripass.travel.mapper;

import com.tripass.checklist.dto.ChecklistResponseDto;
import com.tripass.checklist.dto.ChecklistSummaryResponseDto;
import com.tripass.travel.domain.Trip;
import com.tripass.travel.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

@Mapper
public interface TravelMapper {
    TravelStatusResponseDto getTripDashboard(@Param("tripId") Long tripId);
    
    List<BudgetCheckResponseDto> getTripBudget(@Param("tripId") Long tripId);

    // 여행 존재 여부 확인
    boolean existsByTripId(@Param("tripId") Long tripId);

    // 여행 소유자 ID 조회 (권한 검증용)
    Long selectUserIdByTripId(@Param("tripId") Long tripId);

    Trip selectTripById(@Param("tripId") Long tripId);

    Trip selectLatestTripByUserId(@Param("userId") Long userId);

    int archiveTrip(@Param("tripId") Long tripId, @Param("userId") Long userId);

    int acknowledgeStartReport(@Param("tripId") Long tripId, @Param("userId") Long userId);

    // 유저의 current_view_mode 상태 변경
    int updateUserCurrentViewMode(@Param("userId") Long userId, @Param("currentViewMode") String currentViewMode);

    String selectUserCurrentViewMode(@Param("userId") Long currentUserId);


    boolean existsActiveTripByUserId(@Param("userId") Long userId);

    void insertTripGoal(TripGoalCommandDto command);

    void insertTripCountry(TripCountryCommandDto command);

    TripGoalResponseDto findTripGoalById(@Param("tripId") Long tripId);

    List<TripCountryGoalResponseDto> findTripGoalCountries(@Param("tripId") Long tripId);

    TripGoalResponseDto findActiveTripGoalByUserId(@Param("userId") Long userId);

    List<TripListItemResponseDto> findTripsByUserId(@Param("userId") Long userId);

    List<TripCountryCatalogResponseDto> findCountries(@Param("keyword") String keyword);

    boolean existsCountryById(@Param("countryId") Long countryId);

    boolean existsBudgetBaselineByCountryId(@Param("countryId") Long countryId);

    String findTripStatus(@Param("tripId") Long tripId);

    // 여행/계좌 온보딩 및 여행 저축 집계 관련
    int countActiveAccountsByUserId(@Param("userId") Long userId);

    int countActiveCardsByUserId(@Param("userId") Long userId);

    int activateSavingsTracking(@Param("tripId") Long tripId);

    int markWalletReflectResolved(@Param("tripId") Long tripId);

    BigDecimal findWalletBalanceByUserId(@Param("userId") Long userId);

    java.time.LocalDateTime findOnboardingShownAt(@Param("userId") Long userId);

    int markOnboardingShown(@Param("userId") Long userId);

    void updateTripGoal(TripGoalCommandDto command);

    void softDeleteTripCountries(@Param("tripId") Long tripId);

    List<TripCountryBudgetContextDto> findTripCountryBudgetContexts(@Param("tripId") Long tripId);

    List<TravelTransactionDto> findTravelTransactions(@Param("tripId") Long tripId, @Param("countryId") Long countryId, @Param("categoryName") String categoryName);

    CountryBudgetBaselineDto findCountryBudgetBaseline(@Param("countryId") Long countryId);

    void upsertTripBudgetRecommendation(TripBudgetRecommendationCommandDto command);

    List<CountryBudgetRecommendationResponseDto> findBudgetRecommendationsByTripId(@Param("tripId") Long tripId);

    int updateConfirmedTripBudget(TripBudgetConfirmCommandDto command);

    void updateTripCountryTargetBudget(TripBudgetConfirmCommandDto command);

    void updateTripTargetAmount(@Param("tripId") Long tripId, @Param("totalTargetAmount") BigDecimal totalTargetAmount);

    void insertTripWalletIfAbsent(@Param("userId") Long userId);

    BigDecimal findTripWalletBalanceByUserId(@Param("userId") Long userId);

    BigDecimal findTravelCardKrwBalanceByUserId(@Param("userId") Long userId);

    BigDecimal findCurrentMonthWalletSaving(@Param("userId") Long userId);

    BigDecimal findPrepaidExpenseTotalByTripId(@Param("tripId") Long tripId);

    BigDecimal findMonthlySavingAmountByTripId(@Param("tripId") Long tripId);

    Long findSavingPlanIdByTripId(@Param("tripId") Long tripId);

    void insertSavingPlan(@Param("tripId") Long tripId, @Param("monthlyAmount") BigDecimal monthlyAmount);

    void updateSavingPlanMonthlyAmount(@Param("id") Long id, @Param("monthlyAmount") BigDecimal monthlyAmount);

    // 종료일이 지난 여행 중 상태(TRAVELING)를 완료(ENDED)로 일괄 전환
    int updateEndedTrips(@Param("today") LocalDate today);

    // 시작일이 된 여행 중 상태(PLANNING)를 여행 중(TRAVELING)으로 일괄 전환
    int updateTravelingTrips(@Param("today") LocalDate today);
}
