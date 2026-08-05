package com.tripass.exchange.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.exchange.dto.*;
import com.tripass.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ApiResponse<List<LatestExchangeRateDto>> getLatestRates() {
        return ApiResponse.success("환율 정보 조회 성공", exchangeRateService.getLatestRates());
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
    public ApiResponse<?> getHistoryRates(
            @RequestParam String currencyCode,
            @RequestParam(defaultValue = "7") int days) {
        if (currencyCode == null || currencyCode.isEmpty()) {
            return ApiResponse.error("INVALID_INPUT_VALUE", "통화 코드는 필수입니다.");
        }
        return ApiResponse.success("최근 " + days + "일 환율 추이 조회 성공", 
                                  exchangeRateService.getHistoryRates(currencyCode, days));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<ExchangeRateAlertResponseDto>> getAlerts() {
        Long userId = 101L; // TODO: 실제 로그인한 유저 ID로 대체해야 함
        List<ExchangeRateAlertResponseDto> alerts = exchangeRateService.getAlertsByUserId(userId);
        return ApiResponse.success("관심 환율 알림 목록 조회 성공", alerts);
    }

    @PostMapping("/sync")
    public ApiResponse<SyncResultDto> manualSync(@RequestParam String date) {
        SyncResultDto result = exchangeRateService.syncExchangeRates(date);
        return ApiResponse.success("환율 동기화 완료", result);
    }

    @PostMapping("/alerts")
    public ApiResponse<Map<String, Long>> registerAlert(@RequestBody ExchangeRateAlertRequestDto request) {
        Long alertId = exchangeRateService.registerAlert(101L, request); // TODO: 실제 유저 ID
        return ApiResponse.success("환율 알림이 등록되었습니다.", Map.of("id", alertId));
    }

    @PutMapping("/alerts/{id}")
    public ApiResponse<ExchangeRateAlertUpdateResponseDto> updateAlert(
            @PathVariable Long id,
            @RequestBody ExchangeRateAlertUpdateRequestDto request) {
        Long userId = 101L; // TODO: 실제 로그인한 유저 ID로 대체
        ExchangeRateAlertUpdateResponseDto updatedAlert = exchangeRateService.updateAlert(id, userId, request);
        return ApiResponse.success("환율 알림이 수정되었습니다.", updatedAlert);
    }

    @DeleteMapping("/alerts/{id}")
    public ApiResponse<Map<String, Long>> deleteAlert(@PathVariable Long id) {
        Long userId = 101L; // TODO: 실제 로그인한 유저 ID로 대체
        exchangeRateService.deleteAlert(id, userId);
        return ApiResponse.success("환율 알림이 삭제되었습니다.", Map.of("id", id));
    }
}
