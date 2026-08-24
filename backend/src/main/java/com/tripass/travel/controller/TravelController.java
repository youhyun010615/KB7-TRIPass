package com.tripass.travel.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.travel.dto.*;
import com.tripass.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /**
     * 1. 여행 대시보드 상태 조회
     */
    @GetMapping("/{id}/travel-status")
    public ResponseEntity<ApiResponse<TravelStatusResponseDto>> getTravelStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Long countryId,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        TravelStatusResponseDto data = travelService.getTravelStatus(id, userId, countryId);
        return ResponseEntity.ok(ApiResponse.success("여행 대시보드 조회 성공", data));
    }

    @GetMapping("/current-lifecycle")
    public ResponseEntity<ApiResponse<TripLifecycleResponseDto>> getCurrentLifecycle(
            Authentication authentication) {
        TripLifecycleResponseDto data = travelService.getCurrentLifecycle(getAuthenticatedUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success("여행 라이프사이클 조회 성공", data));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveTrip(
            @PathVariable("id") Long tripId,
            Authentication authentication) {
        travelService.archiveTrip(tripId, getAuthenticatedUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success("여행을 지난 여행으로 보관했습니다.", null));
    }

    @PostMapping("/{id}/start-report/acknowledge")
    public ResponseEntity<ApiResponse<Void>> acknowledgeStartReport(
            @PathVariable("id") Long tripId,
            Authentication authentication) {
        travelService.acknowledgeStartReport(tripId, getAuthenticatedUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success("여행 시작 리포트를 확인했습니다.", null));
    }

    @PostMapping("/onboarding/ack")
    public ResponseEntity<ApiResponse<Void>> acknowledgeOnboarding(
            Authentication authentication) {
        travelService.acknowledgeOnboarding(getAuthenticatedUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success("온보딩을 확인했습니다.", null));
    }

    @PostMapping("/{id}/wallet-reflect")
    public ResponseEntity<ApiResponse<WalletReflectResponseDto>> resolveWalletReflect(
            @PathVariable("id") Long tripId,
            @RequestBody WalletReflectRequestDto request,
            Authentication authentication) {
        WalletReflectResponseDto result = travelService.resolveWalletReflect(
                tripId,
                getAuthenticatedUserId(authentication),
                request.isReflect(),
                request.getTargetAccountId()
        );
        return ResponseEntity.ok(ApiResponse.success("월렛 잔액 반영 여부를 처리했습니다.", result));
    }

    /**
     * 2. 여행 자금 체크 조회
     */
    @GetMapping("/{id}/budget-check")
    public ResponseEntity<ApiResponse<List<BudgetCheckResponseDto>>> getTripBudget(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);


        List<BudgetCheckResponseDto> data = travelService.getTripBudget(id, userId);
        return ResponseEntity.ok(ApiResponse.success("여행 자금 체크 조회 성공", data));
    }

    /**
     * 3. 여행 상세 거래 내역 조회 (필터링 가능)
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<TravelTransactionDto>>> getTripTransactions(
            @PathVariable Long id,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) String categoryName,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        List<TravelTransactionDto> data = travelService.getTripTransactions(id, userId, countryId, categoryName);
        return ResponseEntity.ok(ApiResponse.success("거래 내역 조회 성공", data));
    }


    /**
     * 여행/저축 모드 전환 (토글)
     */
    @PatchMapping("/{id}/start")
    public ResponseEntity<ApiResponse<TravelModeResponseDto>> toggleTravelMode(
            @PathVariable Long id,
            @RequestBody TravelModeRequestDto request,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        TravelModeResponseDto data = travelService.toggleTravelMode(id, request, userId);

        String responseMessage = Boolean.TRUE.equals(data.getIsTravelMode())
                ? "여행 모드로 전환되었습니다."
                : "저축 모드로 전환되었습니다.";

        return ResponseEntity.ok(ApiResponse.success(responseMessage, data));
    }



    /** 여행 목표 등록: 여행명, 방문 국가 순서, 국가별 일정을 먼저 저장합니다. */
    @PostMapping
    public ResponseEntity<ApiResponse<TripGoalCreateResponseDto>> createTripGoal(
            @Valid @RequestBody TripGoalCreateRequestDto request,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripGoalCreateResponseDto data = travelService.createTripGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("여행 목표 등록 성공", data));
    }

    /** 로그인 사용자가 등록한 전체 여행 목록을 조회합니다. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TripListItemResponseDto>>> getMyTrips(
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        List<TripListItemResponseDto> data = travelService.getMyTrips(userId);
        return ResponseEntity.ok(ApiResponse.success("여행 목록 조회 성공", data));
    }

    /** 로그인 사용자의 진행 중 여행 목표를 조회합니다. */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<TripGoalResponseDto>> getActiveTripGoal(
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripGoalResponseDto data = travelService.getActiveTripGoal(userId);
        return ResponseEntity.ok(ApiResponse.success("진행 중인 여행 목표 조회 성공", data));
    }

    /** 로그인 사용자의 활성 여행과 홈 저축 현황을 조회합니다. */
    @GetMapping("/active/home")
    public ResponseEntity<ApiResponse<TripHomeDashboardResponseDto>> getActiveTripHome(
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripHomeDashboardResponseDto data = travelService.getActiveTripHome(userId);
        return ResponseEntity.ok(ApiResponse.success("여행 저축 홈 조회 성공", data));
    }

    /** 국가·통화 선택 모달용 국가 목록을 검색합니다. */
    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<TripCountryCatalogResponseDto>>> getCountries(
            @RequestParam(required = false) String keyword
    ) {
        List<TripCountryCatalogResponseDto> data = travelService.getCountries(keyword);
        return ResponseEntity.ok(ApiResponse.success("여행 국가 목록 조회 성공", data));
    }

    /** 여행 시작 전, 여행명과 국가 방문 순서·일정을 수정합니다. */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TripGoalResponseDto>> updateTripGoal(
            @PathVariable("id") Long tripId,
            @Valid @RequestBody TripGoalUpdateRequestDto request,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripGoalResponseDto data = travelService.updateTripGoal(tripId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("여행 목표 수정 성공", data));
    }

    /** 로그인 사용자가 소유한 여행을 삭제합니다. 연결 데이터 보존을 위해 소프트 삭제합니다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(
            @PathVariable("id") Long tripId,
            Authentication authentication
    ) {
        travelService.deleteTrip(tripId, getAuthenticatedUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success("여행 삭제 성공", null));
    }

    /** 특정 여행 목표를 조회합니다. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripGoalResponseDto>> getTripGoal(
            @PathVariable("id") Long tripId,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripGoalResponseDto data = travelService.getTripGoal(tripId, userId);
        return ResponseEntity.ok(ApiResponse.success("여행 목표 조회 성공", data));
    }

    /** 국가별 일정에 맞춘 AI 여행 예산을 생성합니다. */
    @PostMapping("/{id}/budget-recommendations")
    public ResponseEntity<ApiResponse<TripBudgetRecommendationResponseDto>> generateBudgetRecommendations(
            @PathVariable("id") Long tripId,
            Authentication authentication
    ) {
        TripBudgetRecommendationResponseDto data = travelService.generateBudgetRecommendations(
                tripId, getAuthenticatedUserId(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success("AI 여행 예산 추천 생성 성공", data));
    }

    /** 저장된 AI 여행 예산 추천을 조회합니다. */
    @GetMapping("/{id}/budget-recommendations")
    public ResponseEntity<ApiResponse<TripBudgetRecommendationResponseDto>> getBudgetRecommendations(
            @PathVariable("id") Long tripId,
            Authentication authentication
    ) {
        TripBudgetRecommendationResponseDto data = travelService.getBudgetRecommendations(
                tripId, getAuthenticatedUserId(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success("AI 여행 예산 추천 조회 성공", data));
    }

    /** 사용자가 수정한 국가별 예산을 확정하고 월 저축 목표를 계산합니다. */
    @PutMapping("/{id}/budget-recommendations")
    public ResponseEntity<ApiResponse<TripGoalCompletionResponseDto>> confirmBudgetRecommendations(
            @PathVariable("id") Long tripId,
            @Valid @RequestBody TripBudgetConfirmRequestDto request,
            Authentication authentication
    ) {
        TripGoalCompletionResponseDto data = travelService.confirmBudgetRecommendations(
                tripId, getAuthenticatedUserId(authentication), request
        );
        return ResponseEntity.ok(ApiResponse.success("여행 목표 금액 확정 성공", data));
    }
}
