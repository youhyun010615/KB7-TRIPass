package com.tripass.exchange.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.exchange.dto.*;
import com.tripass.exchange.exception.ExchangeErrorCode;
import com.tripass.exchange.exception.ExchangeException;
import com.tripass.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    @GetMapping
    public ApiResponse<List<LatestExchangeRateDto>> getLatestRates() {
        return ApiResponse.success("환율 정보 조회 성공", exchangeRateService.getLatestRates());
    }

    @GetMapping("/countries")
    public ApiResponse<List<CountryExchangeRateDto>> getLatestRatesByCountry() {
        return ApiResponse.success("국가별 환율 정보 조회 성공", exchangeRateService.getLatestRatesByCountry());
    }

    @GetMapping("/convert")
    public ApiResponse<ExchangeRateConvertResponseDto> convert(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam double amount) {
        return ApiResponse.success("환율 계산 성공", 
                                  exchangeRateService.convertCurrency(fromCurrency, toCurrency, amount));
    }

    @GetMapping("/history")
    public ApiResponse<ExchangeRateHistoryResponseDto> getHistoryRates(
            @RequestParam String currencyCode,
            @RequestParam(defaultValue = "7") int days) {
        if (currencyCode == null || currencyCode.isEmpty()) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
        }
        return ApiResponse.success("최근 " + days + "일 환율 추이 조회 성공", 
                                  exchangeRateService.getHistoryRates(currencyCode, days));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<ExchangeRateAlertResponseDto>> getAlerts(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        List<ExchangeRateAlertResponseDto> alerts = exchangeRateService.getAlertsByUserId(userId);
        return ApiResponse.success("관심 환율 알림 목록 조회 성공", alerts);
    }

    @PostMapping("/sync")
    public ApiResponse<SyncResultDto> manualSync() {
        SyncResultDto result = exchangeRateService.syncExchangeRates();
        return ApiResponse.success("환율 동기화 완료", result);
    }

    /**
     * [초기 데이터 적재/복구용] 과거 90일치 환율 일괄 수집 (1회 실행)
     */
    @PostMapping("/init-history")
    public ApiResponse<SyncResultDto> initPast90Days() {
        SyncResultDto result = exchangeRateService.initPast90DaysRates();
        return ApiResponse.success("과거 90일치 환율 초기화 및 복구 완료", result);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/alerts")
    public ApiResponse<Map<String, Long>> registerAlert(@RequestBody ExchangeRateAlertRequestDto request, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Long alertId = exchangeRateService.registerAlert(userId, request);
        return ApiResponse.success("환율 알림이 등록되었습니다.", Map.of("id", alertId));
    }

    @PutMapping("/alerts/{id}")
    public ApiResponse<ExchangeRateAlertUpdateResponseDto> updateAlert(
            @PathVariable Long id,
            @RequestBody ExchangeRateAlertUpdateRequestDto request,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        ExchangeRateAlertUpdateResponseDto updatedAlert = exchangeRateService.updateAlert(id, userId, request);
        return ApiResponse.success("환율 알림이 수정되었습니다.", updatedAlert);
    }

    @DeleteMapping("/alerts/{id}")
    public ApiResponse<Void> deleteAlert(@PathVariable Long id, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        exchangeRateService.deleteAlert(id, userId);
        return ApiResponse.success("환율 알림이 삭제되었습니다.", null);
    }
}

