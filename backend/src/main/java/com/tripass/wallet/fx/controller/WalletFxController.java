package com.tripass.wallet.fx.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.wallet.fx.dto.request.WalletExchangeEstimateRequestDto;
import com.tripass.wallet.fx.dto.request.WalletExchangeSellRequestDto;
import com.tripass.wallet.fx.dto.response.WalletCurrencyResponseDto;
import com.tripass.wallet.fx.dto.response.WalletExchangeEstimateResponseDto;
import com.tripass.wallet.fx.dto.response.WalletExchangeResponseDto;
import com.tripass.wallet.fx.service.WalletFxService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/** 월렛 환전 예상 계산과 트래블카드 외화 재환전 API를 제공하는 컨트롤러입니다. */

@Api(tags = "월렛 환전")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet/fx")
public class WalletFxController {

    private final WalletFxService walletFxService;

    @ApiOperation(value = "환전 가능 통화 목록 조회", notes = "외화 충전 화면에서 선택할 수 있는 통화 목록을 조회합니다.")
    @GetMapping("/currencies")
    public ApiResponse<List<WalletCurrencyResponseDto>> getCurrencies() {
        return ApiResponse.success(walletFxService.getCurrencies());
    }

    @ApiOperation(value = "환전 예상 금액 계산", notes = "원화→외화 또는 외화→원화 환전 예상 금액을 계산합니다.")
    @GetMapping("/estimate")
    public ApiResponse<WalletExchangeEstimateResponseDto> estimate(
            @Valid @ModelAttribute WalletExchangeEstimateRequestDto request
    ) {
        return ApiResponse.success(walletFxService.estimate(request));
    }

    @ApiOperation(value = "외화 재환전", notes = "트래블카드 외화 잔액을 원화 월렛 잔액으로 재환전합니다.")
    @PostMapping("/sell")
    public ApiResponse<WalletExchangeResponseDto> sell(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WalletExchangeSellRequestDto request
    ) {
        return ApiResponse.success("외화 재환전이 완료되었습니다.", walletFxService.sell(userId, request));
    }
}
