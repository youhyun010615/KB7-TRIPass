package com.tripass.asset.controller;

import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.CardLinkRequestDto;
import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.service.AssetService;
import com.tripass.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final AssetService assetService;

    public CardController(AssetService assetService) {
        this.assetService = assetService;
    }

    // 카드 연동
    @PostMapping("/codef/connect")
    public ResponseEntity<ApiResponse<List<CardDto>>> linkCard(
            Authentication authentication,
            @RequestBody CardLinkRequestDto req) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(assetService.linkCard(userId, req)));
    }

    // CODEF 카드 거래내역 조회 및 저장
    @PostMapping("/{cardId}/transactions/fetch")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> fetchCardTransactions(
            Authentication authentication,
            @PathVariable Long cardId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(assetService.fetchCardTransactions(userId, cardId, startDate, endDate)));
    }

    // 카드 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardDto>>> getCards(
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(assetService.getCards(userId)));
    }

    // 카드별 거래내역 조회
    @GetMapping("/{cardId}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getCardTransactions(
            Authentication authentication,
            @PathVariable Long cardId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(assetService.getCardTransactions(userId, cardId, startDate, endDate)));
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
