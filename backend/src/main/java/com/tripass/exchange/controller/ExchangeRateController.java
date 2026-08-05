package com.tripass.exchange.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.exchange.dto.ExchangeRateHistoryResponseDto;
import com.tripass.exchange.dto.LatestExchangeRateDto;
import com.tripass.exchange.dto.SyncResultDto;
import com.tripass.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/sync")
    public ApiResponse<SyncResultDto> manualSync(@RequestParam String date) {
        SyncResultDto result = exchangeRateService.syncExchangeRates(date);
        return ApiResponse.success("환율 동기화 완료", result);
    }
}
