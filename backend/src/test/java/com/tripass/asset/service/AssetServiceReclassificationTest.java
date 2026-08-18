package com.tripass.asset.service;

import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.dto.TransactionReclassificationResponseDto;
import com.tripass.asset.mapper.AssetMapper;
import com.tripass.asset.duplicate.DuplicateTransactionMatcher;
import com.tripass.asset.service.codef.CodefClient;
import com.tripass.saving.classification.CategoryClassificationResult;
import com.tripass.saving.classification.ConsumptionCategoryCode;
import com.tripass.saving.classification.TransactionCategoryClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceReclassificationTest {

    @Mock
    private AssetMapper assetMapper;

    @Mock
    private TransactionCategoryClassifier transactionCategoryClassifier;

    @Mock
    private CodefClient codefClient;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(
                assetMapper, transactionCategoryClassifier, new DuplicateTransactionMatcher(), codefClient);
    }

    @Test
    void 기존_미분류_카드_거래를_자동분류한다() {
        TransactionDto food = transaction(1L, "쿠팡이츠", "PG일반(비인증)");
        TransactionDto unknown = transaction(2L, null, null);
        when(assetMapper.findUnclassifiedCardTransactionsByUserId(3L))
                .thenReturn(List.of(food, unknown));
        when(transactionCategoryClassifier.classify("쿠팡이츠", "PG일반(비인증)"))
                .thenReturn(CategoryClassificationResult.fromAiModel(
                        ConsumptionCategoryCode.FOOD,
                        new BigDecimal("0.9500")
                ));
        when(transactionCategoryClassifier.classify(null, null))
                .thenReturn(CategoryClassificationResult.fallback(new BigDecimal("0.1000")));
        when(assetMapper.findCategoryIdByCode("FOOD")).thenReturn(1L);
        when(assetMapper.findCategoryIdByCode("OTHER")).thenReturn(6L);
        when(assetMapper.updateAutoClassification(any(TransactionDto.class))).thenReturn(1);

        TransactionReclassificationResponseDto result =
                assetService.reclassifyCardTransactions(3L);

        assertEquals(2, result.processedCount());
        assertEquals(1, result.classifiedCount());
        assertEquals(1, result.fallbackCount());
        assertEquals(2, result.updatedCount());
        assertEquals(1L, food.getCategoryId());
        assertEquals("AI_MODEL", food.getCategorySource());
        assertEquals(6L, unknown.getCategoryId());
        assertEquals("FALLBACK", unknown.getCategorySource());
        verify(assetMapper).updateAutoClassification(food);
        verify(assetMapper).updateAutoClassification(unknown);
    }

    @Test
    void 동시_수정으로_업데이트되지_않은_거래는_결과_건수에서_제외한다() {
        TransactionDto target = transaction(1L, "컴포즈커피", "커피전문점");
        when(assetMapper.findUnclassifiedCardTransactionsByUserId(3L))
                .thenReturn(List.of(target));
        when(transactionCategoryClassifier.classify("컴포즈커피", "커피전문점"))
                .thenReturn(CategoryClassificationResult.fromCodefType(
                        ConsumptionCategoryCode.CAFE
                ));
        when(assetMapper.findCategoryIdByCode("CAFE")).thenReturn(7L);
        when(assetMapper.updateAutoClassification(target)).thenReturn(0);

        TransactionReclassificationResponseDto result =
                assetService.reclassifyCardTransactions(3L);

        assertEquals(1, result.processedCount());
        assertEquals(0, result.classifiedCount());
        assertEquals(0, result.fallbackCount());
        assertEquals(0, result.updatedCount());
    }

    private TransactionDto transaction(Long id, String merchantName, String merchantType) {
        TransactionDto transaction = new TransactionDto();
        transaction.setId(id);
        transaction.setMerchantName(merchantName);
        transaction.setMerchantType(merchantType);
        return transaction;
    }
}
