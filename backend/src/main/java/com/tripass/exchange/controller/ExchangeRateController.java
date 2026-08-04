package com.tripass.exchange.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ApiResponse<?> getLatestRates(@RequestParam(required = false) String currencyCodes) {
        List<String> codeList = (currencyCodes != null && !currencyCodes.isEmpty()) 
                                ? Arrays.asList(currencyCodes.split(",")) : null;
        return ApiResponse.success("환율 정보 조회 성공", exchangeRateService.getLatestRates(codeList));
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
    public ApiResponse<?> manualSync(@RequestParam String date) {
        exchangeRateService.syncExchangeRates(date);
        return ApiResponse.success("수동 환율 동기화 요청 완료", null);
    }
}
