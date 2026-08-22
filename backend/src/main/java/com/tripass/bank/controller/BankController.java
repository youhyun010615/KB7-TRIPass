package com.tripass.bank.controller;

import com.tripass.bank.dto.BankBranchDto;
import com.tripass.bank.dto.ExchangeEstimateDto;
import com.tripass.bank.service.BankService;
import com.tripass.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @PostMapping("/sync")
    public ApiResponse<Map<String, Integer>> syncBankBranches() {
        int processedCount = bankService.syncBankBranches();
        return ApiResponse.success("총 " + processedCount + "건의 은행 데이터 동기화 완료", Map.of("processedCount", processedCount));
    }

    // 1 & 4. 근처 환전 은행 목록 조회 (keyword가 있으면 검색)
    @GetMapping
    public ApiResponse<List<BankBranchDto>> getBanks(
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) String keyword) {
        
        List<BankBranchDto> banks;
        if (latitude != null && longitude != null) {
            banks = bankService.findNearbyBanks(latitude, longitude, radius);
        } else {
            banks = bankService.findAll(keyword);
        }

        return ApiResponse.success("은행 목록 조회 성공", banks);
    }

    // 2. 환전 예상 금액 계산
    @GetMapping("/estimate")
    public ApiResponse<ExchangeEstimateDto> getEstimate(
            @RequestParam BigDecimal amount,
            @RequestParam String currencyCode) {
        
        ExchangeEstimateDto estimate = bankService.getEstimate(amount, currencyCode);
        return ApiResponse.success("환전 예상 금액 계산 성공", estimate);
    }

    // 3. 특정 은행 지점 상세정보 조회
    @GetMapping("/{bankId}")
    public ApiResponse<BankBranchDto> getBankDetail(@PathVariable Long bankId) {
        BankBranchDto bank = bankService.findById(bankId);
        return ApiResponse.success("은행 지점 상세 조회 성공", bank);
    }
}
