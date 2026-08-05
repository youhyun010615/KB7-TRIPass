package com.tripass.asset.controller;

import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.service.AssetService;
import com.tripass.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")

public class TransactionController {

    private final AssetService assetService;

    public TransactionController(AssetService assetService) {
        this.assetService = assetService;
    }

    //AST-005: 전체 계좌 거래내역 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getAllTransactions(
            @RequestAttribute("userId") Long userId) {
        return
                ResponseEntity.ok(ApiResponse.success(assetService.getAllTransactions(userId)));
    }

    //AST-006: 거래내역 단건 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransactionDetail(
            @RequestAttribute("userId") Long userId,
            @PathVariable("id") Long transactionId) {
        return
                ResponseEntity.ok(ApiResponse.success(assetService.getTransactionDetail(userId, transactionId)));
    }
}
