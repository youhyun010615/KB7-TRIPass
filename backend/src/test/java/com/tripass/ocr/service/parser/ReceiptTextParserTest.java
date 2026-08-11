package com.tripass.ocr.service.parser;

import com.tripass.ocr.dto.internal.ParsedReceiptData;
import com.tripass.ocr.dto.internal.ParsedReceiptItem;
import com.tripass.ocr.dto.internal.VisionOcrResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiptTextParserTest {

    private final ReceiptTextParser parser =
            new ReceiptTextParser();

    @Test
    void 영문_영수증을_구조화한다() {
        String rawText =
                """
                TOKYO MART
                2026-08-11 14:30
                COFFEE 450
                SANDWICH 780
                SUBTOTAL 1230
                TAX 123
                TOTAL 1353 JPY
                """;

        VisionOcrResult ocrResult =
                new VisionOcrResult(
                        rawText,
                        "en",
                        Collections.emptyList()
                );

        ParsedReceiptData result =
                parser.parse(ocrResult);

        assertEquals(
                "TOKYO MART",
                result.getOriginalMerchantName()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        8,
                        11,
                        14,
                        30
                ),
                result.getPaymentDateTime()
        );

        assertEquals(
                "JPY",
                result.getCurrencyCode()
        );

        assertEquals(
                new BigDecimal("1353"),
                result.getTotalAmount()
        );

        assertEquals(
                new BigDecimal("123"),
                result.getTaxAmount()
        );

        assertEquals(2, result.getItems().size());

        ParsedReceiptItem firstItem =
                result.getItems().get(0);

        assertEquals(
                "COFFEE",
                firstItem.getOriginalName()
        );

        assertEquals(
                new BigDecimal("450"),
                firstItem.getAmount()
        );
    }

    @Test
    void 유럽식_소수점_금액을_변환한다() {
        String rawText =
                """
                PARIS CAFE
                13/08/2026 09:15
                COFFEE 3,50
                SANDWICH 7,80
                VAT 1,13
                TOTAL 12,43 EUR
                """;

        VisionOcrResult ocrResult =
                new VisionOcrResult(
                        rawText,
                        "fr",
                        Collections.emptyList()
                );

        ParsedReceiptData result =
                parser.parse(ocrResult);

        assertEquals(
                LocalDateTime.of(
                        2026,
                        8,
                        13,
                        9,
                        15
                ),
                result.getPaymentDateTime()
        );

        assertEquals(
                "EUR",
                result.getCurrencyCode()
        );

        assertEquals(
                new BigDecimal("12.43"),
                result.getTotalAmount()
        );

        assertEquals(2, result.getItems().size());
    }

    @Test
    void 인식할_수_없는_값은_null로_반환한다() {
        VisionOcrResult ocrResult =
                new VisionOcrResult(
                        "UNKNOWN STORE",
                        null,
                        Collections.emptyList()
                );

        ParsedReceiptData result =
                parser.parse(ocrResult);

        assertEquals(
                "UNKNOWN STORE",
                result.getOriginalMerchantName()
        );

        assertNull(result.getPaymentDateTime());
        assertNull(result.getCurrencyCode());
        assertNull(result.getTotalAmount());
        assertNull(result.getTaxAmount());
        assertEquals(0, result.getItems().size());
    }
}