package com.tripass.batch.controller;

import com.tripass.batch.service.MockTransactionGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test/batch")
@RequiredArgsConstructor
public class TestBatchController {

    private final MockTransactionGenerator mockTransactionGenerator;

    @PostMapping("/transactions")
    public ResponseEntity<String> triggerManualBatch(@RequestParam(defaultValue = "5") int count) {
        mockTransactionGenerator.generateAndInsertMockTransactions(count);
        return ResponseEntity.ok(count + "건 수동 배치 요청 완료");
    }
}