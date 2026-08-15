package com.tripass.wallet.travelcard.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.wallet.travelcard.dto.request.WalletTravelCardLinkRequestDto;
import com.tripass.wallet.travelcard.dto.request.WalletTravelCardTopupRequestDto;
import com.tripass.wallet.travelcard.dto.response.TravelCardCurrencyBalanceResponseDto;
import com.tripass.wallet.travelcard.dto.response.TravelCardLedgerResponseDto;
import com.tripass.wallet.travelcard.dto.response.UserTravelCardOptionResponseDto;
import com.tripass.wallet.travelcard.dto.response.WalletCardTopupResponseDto;
import com.tripass.wallet.travelcard.dto.response.WalletTravelCardResponseDto;
import com.tripass.wallet.travelcard.service.WalletTravelCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/** 월렛 트래블카드 연동, 외화 충전, 보유 외화 조회 API를 제공하는 컨트롤러입니다. */

@Api(tags = "월렛 트래블카드")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet/travel-card")
public class WalletTravelCardController {

    private final WalletTravelCardService walletTravelCardService;

    @ApiOperation(value = "연동 트래블카드 조회", notes = "월렛에 연동된 트래블카드 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<WalletTravelCardResponseDto> getTravelCard(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(walletTravelCardService.getTravelCard(userId));
    }

    @ApiOperation(value = "사용자 보유 트래블카드 목록 조회", notes = "사용자가 보유한 트래블카드 목록을 조회합니다.")
    @GetMapping("/options")
    public ApiResponse<List<UserTravelCardOptionResponseDto>> getUserTravelCardOptions(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(walletTravelCardService.getUserTravelCardOptions(userId));
    }

    @ApiOperation(value = "트래블카드 연동", notes = "월렛에 사용할 트래블카드를 연동합니다.")
    @PostMapping
    public ApiResponse<Void> linkTravelCard(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WalletTravelCardLinkRequestDto request
    ) {
        walletTravelCardService.linkTravelCard(userId, request);

        return ApiResponse.success("트래블카드 연동이 완료되었습니다.", null);
    }

    @ApiOperation(value = "트래블카드 연동 해제", notes = "월렛에 연동된 트래블카드를 해제합니다.")
    @DeleteMapping
    public ApiResponse<Void> unlinkTravelCard(@AuthenticationPrincipal Long userId) {
        walletTravelCardService.unlinkTravelCard(userId);

        return ApiResponse.success("트래블카드 연동이 해제되었습니다.", null);
    }

    @ApiOperation(value = "트래블카드 외화 충전", notes = "월렛 원화를 환전해 트래블카드 외화 잔액으로 충전합니다.")
    @PostMapping("/topup")
    public ApiResponse<WalletCardTopupResponseDto> topup(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WalletTravelCardTopupRequestDto request
    ) {
        WalletCardTopupResponseDto data = walletTravelCardService.topup(
                userId,
                request
        );

        return ApiResponse.success("트래블카드 외화 충전이 완료되었습니다.", data);
    }

    @ApiOperation(value = "트래블카드 보유 외화 조회", notes = "연동된 트래블카드의 통화별 외화 잔액을 조회합니다.")
    @GetMapping("/balances")
    public ApiResponse<List<TravelCardCurrencyBalanceResponseDto>> getBalances(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(walletTravelCardService.getBalances(userId));
    }

    @ApiOperation(value = "트래블카드 외화 원장 조회", notes = "연동된 트래블카드의 외화 충전, 차감, 환불, 보정 내역을 조회합니다.")
    @GetMapping("/ledgers")
    public ApiResponse<List<TravelCardLedgerResponseDto>> getLedgers(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(walletTravelCardService.getLedgers(userId));
    }
}
