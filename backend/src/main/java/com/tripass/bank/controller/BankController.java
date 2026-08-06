package com.tripass.bank.controller;

import com.tripass.bank.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncBankBranches() {
        int processedCount = bankService.syncBankBranches();

        Map<String, Object> response = new HashMap<>();
        response.put("code", "SUCCESS");
        response.put("message", "총 " + processedCount + "건의 은행 데이터 동기화 완료");
        
        Map<String, Object> data = new HashMap<>();
        data.put("processedCount", processedCount);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}
