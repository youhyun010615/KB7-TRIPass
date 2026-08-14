package com.tripass.batch.controller;

import com.tripass.batch.config.RootConfig;
import com.tripass.batch.config.SchedulerConfig;
import com.tripass.batch.dto.TransactionMockDto;
import com.tripass.batch.mapper.MockTransactionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {RootConfig.class, SchedulerConfig.class})
class TestBatchControllerTest {

    @Autowired
    MockTransactionMapper mockTransactionMapper;

    @Test
    @DisplayName("TransactionDto 기반 단건 Insert 기초 테스트")
    @Transactional
    @Rollback(false) // 실제 DB 테이블에 들어가는지 확인용
    void testTransactionInsert() {
        // 1. DTO에 직접 가상 데이터 수동 세팅
        TransactionMockDto dto = TransactionMockDto.builder()
                .accountId(1L) // DB에 미리 넣어둔 accounts.id
                .transactionDate(LocalDate.now())
                .transactionTime(LocalTime.now())
                .transactionType("WITHDRAWAL")
                .transactionRegion("DOMESTIC")
                .amount(new BigDecimal("15000.00"))
                .merchantName("AI 분석 대상 가맹점 (수동 테스트)")
                .paymentMethod("CHECK_CARD")
                .externalKey("TEST_INIT_" + System.currentTimeMillis())
                .isPreExpense(false)
                .isDeleted((byte) 0)
                .build();


        // 2. DB Insert 수행 및 성공 확인
        int result = mockTransactionMapper.insertMockTransaction(dto);

        // 3. 단정문 검증
        assertEquals(1, result, "1건의 거래 데이터가 성공적으로 INSERT 되어야 합니다.");
    }

}