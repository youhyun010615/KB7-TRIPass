package com.tripass.asset.controller;

import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.service.CardTransactionService;
import com.tripass.common.response.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "카드 거래 Mock API")
@RestController
@RequestMapping("/api/v1/cards")
public class CardTransactionController {

    private final CardTransactionService cardTransactionService;

    public CardTransactionController(CardTransactionService cardTransactionService) {
        this.cardTransactionService = cardTransactionService;
    }

    @ApiOperation("로그인 회원의 연동 카드 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardDto>>> getCards(
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(
                ApiResponse.success("연동 카드 목록 조회 성공", cardTransactionService.getCards(userId))
        );
    }

    @ApiOperation("로그인 회원의 카드 거래내역 조회")
    @GetMapping("/{cardId}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getCardTransactions(
            Authentication authentication,
            @ApiParam(value = "카드 ID", required = true)
            @PathVariable("cardId") Long cardId,
            @ApiParam(value = "조회 연월(yyyy-MM). 날짜 범위와 동시 사용 불가")
            @RequestParam(required = false) String yearMonth,
            @ApiParam(value = "시작일(yyyy-MM-dd)")
            @RequestParam(required = false) String startDate,
            @ApiParam(value = "종료일(yyyy-MM-dd)")
            @RequestParam(required = false) String endDate
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        List<TransactionDto> transactions = cardTransactionService.getTransactions(
                userId,
                cardId,
                yearMonth,
                startDate,
                endDate
        );
        return ResponseEntity.ok(
                ApiResponse.success("카드 거래내역 조회 성공", transactions)
        );
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
