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
    void parsesEnglishReceipt() {
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

        ParsedReceiptData result =
                parse(rawText, "en");

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
                2,
                result.getItems().size()
        );

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
    void parsesEuropeanDecimalAmount() {
        String rawText =
                """
                PARIS CAFE
                13/08/2026 09:15
                COFFEE 3,50
                SANDWICH 7,80
                VAT 1,13
                TOTAL 12,43 EUR
                """;

        ParsedReceiptData result =
                parse(rawText, "fr");

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

        assertEquals(
                2,
                result.getItems().size()
        );
    }

    @Test
    void parsesKoreanReceiptMainFields() {
        String rawText =
                """
                교화권 번호 : 19
                [영수증]
                세종대점 매머드익스프레스
                서울 광진구 광나루로 385 1층 3호
                6451402564/ 구호석 / TEL: 02-5555-55555
                주문번호 : 202600100041568
                추문일시: 2026-08-13 08:44:29
                ===
                상품명
                (ICE) 아메리카노
                - ICE ONLY(아이스만 가능)
                - (ICE)SIZE M
                수량
                1
                공급가액
                쿠가 세
                1,637
                163
                1,800
                1,800
                결제금액
                합 | 신승할승승카발매기
                || 용인부인인드급입맹
                :
                : 1,800
                00
                : 30037455
                : 20260813084429
                가맹점번호 : 00112462079
                """;

        ParsedReceiptData result =
                parse(rawText, "ko");

        assertEquals(
                "세종대점 매머드익스프레스",
                result.getOriginalMerchantName()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        8,
                        13,
                        8,
                        44,
                        29
                ),
                result.getPaymentDateTime()
        );

        assertEquals(
                "KRW",
                result.getCurrencyCode()
        );

        assertEquals(
                new BigDecimal("1800"),
                result.getTotalAmount()
        );

        // 품목과 가격의 위치 관계가 손상된 경우
        // 잘못된 품목을 생성하지 않는지 확인한다.
        assertEquals(
                0,
                result.getItems().size()
        );
    }

    @Test
    void parsesJapaneseReceiptDateAndFinalTotal() {
        String rawText =
                """
                7 セブン-イレブン
                長崎銅座町店
                長崎県長崎市銅座町9-14
                電話 : 095-822-2377
                レジ#2
                2016年09月18日 (日) 19:14 責028
                領収書
                ウェットティッシュ 携帯用 10枚
                毎日飲むヤクルト100ML
                ¥108
                ¥118
                高千穂牧場カフェ・オ・レ220ML
                合計
                ¥149
                ¥375
                (内消費税等
                ¥27)
                お預り
                ¥1,000
                お
                釣
                ¥625
                """;

        ParsedReceiptData result =
                parse(rawText, "ja");

        assertEquals(
                "7 セブン-イレブン",
                result.getOriginalMerchantName()
        );

        assertEquals(
                LocalDateTime.of(
                        2016,
                        9,
                        18,
                        19,
                        14
                ),
                result.getPaymentDateTime()
        );

        assertEquals(
                "JPY",
                result.getCurrencyCode()
        );

        assertEquals(
                new BigDecimal("375"),
                result.getTotalAmount()
        );

        // OCR 줄 순서가 손상되어 품목과 가격 관계가 불확실하다.
        assertEquals(
                0,
                result.getItems().size()
        );
    }

    @Test
    void parsesFrenchReceiptItemsAndTotal() {
        String rawText =
                """
                Sourire
                restaurant & bar
                Le Sourire
                27 rue lande
                2005 Parts
                Tel 01 42 01 06 43
                RCS 804 251 577
                CAIDSET 3164 Bar
                mar. 14jul 1. 15
                Direction
                Table:11 Cv:2
                1 Verre de Trepala
                55
                5.00
                1 Eau de Perrter 50cl
                4.00
                1 Burger
                9.00
                1 Croquettes Conta
                7.00
                1 Frites au Parsesan
                7.00
                1 Risotto
                6.00
                1 Moelleux Nutella
                7.00
                1 Cheesecake NY
                6.00
                2 Expresso
                2.50
                5.00
                Tot Euro
                56.00
                TVAS Total
                HT
                Tva
                20%
                5.00
                4.17
                0.83
                """;

        ParsedReceiptData result =
                parse(rawText, "fr");

        assertEquals(
                "Sourire",
                result.getOriginalMerchantName()
        );

        // OCR 원문에서 날짜와 시간을 확정할 수 없다.
        assertNull(
                result.getPaymentDateTime()
        );

        assertEquals(
                "EUR",
                result.getCurrencyCode()
        );

        assertEquals(
                new BigDecimal("56.00"),
                result.getTotalAmount()
        );

        assertEquals(
                9,
                result.getItems().size()
        );

        ParsedReceiptItem nutellaItem =
                result.getItems().get(6);

        assertEquals(
                "Moelleux Nutella",
                nutellaItem.getOriginalName()
        );

        assertEquals(
                1,
                nutellaItem.getQuantity()
        );

        assertEquals(
                new BigDecimal("7.00"),
                nutellaItem.getAmount()
        );

        ParsedReceiptItem expressoItem =
                result.getItems().get(8);

        assertEquals(
                "Expresso",
                expressoItem.getOriginalName()
        );

        assertEquals(
                2,
                expressoItem.getQuantity()
        );

        assertEquals(
                new BigDecimal("5.00"),
                expressoItem.getAmount()
        );
    }

    @Test
    void detectsJapaneseYenWhenVisionDetectsChinese() {
        String rawText =
                """
                FamilyMart
                北梅田店
                大阪府大阪市北区芝田2-3-23
                電話:06-6485-4782
                領取証
                2017年2月26日(日) 20:46
                求加工卜500P ¥151
                ろは
                小
                計
                ¥130
                * 281
                1
                合
                計
                ¥281 o
                (内消費税等
                ¥20)
                お預り
                ¥500
                お
                釣
                ¥219
                2-1962
                No. 016
                """;

        ParsedReceiptData result =
                parse(rawText, "zh");

        assertEquals(
                "FamilyMart",
                result.getOriginalMerchantName()
        );

        assertEquals(
                LocalDateTime.of(
                        2017,
                        2,
                        26,
                        20,
                        46
                ),
                result.getPaymentDateTime()
        );

        // Vision이 zh로 감지해도 일본 영수증 문맥을 우선한다.
        assertEquals(
                "JPY",
                result.getCurrencyCode()
        );

        assertEquals(
                new BigDecimal("281"),
                result.getTotalAmount()
        );

        assertEquals(
                1,
                result.getItems().size()
        );

        ParsedReceiptItem firstItem =
                result.getItems().get(0);

        assertEquals(
                new BigDecimal("151"),
                firstItem.getAmount()
        );
    }

    @Test
    void stopsItemAmountSearchBeforeSubtotal() {
        String rawText =
                """
                21/01/2023
                London,
                SW19 7NH
                56
                13:28
                1x Original Turkey Hot Dog
                £5.10
                1x Melon Soda 350m1
                £2.20
                1x The Original Pork Hotdog
                £4.90
                1x Cheese Tteokbokki
                £6.50
                1x Rose Tteokbokki
                £6.95
                1x Spicy Fishcake and Sausage Mini
                £4.20
                Subtotal
                £29.85
                Total
                £29.85
                Payment Sense [AUTH 53]
                £29.85
                Change Due
                £0.00
                Eat In
                Net
                £24.87
                VAT
                Gross
                £4.98
                £29.85
                Printed: 21/01/2023 13:28
                Device: Wimbledon - Till - 00001 (1869)
                """;

        ParsedReceiptData result =
                parse(rawText, "en");

        assertEquals(
                LocalDateTime.of(
                        2023,
                        1,
                        21,
                        13,
                        28
                ),
                result.getPaymentDateTime()
        );

        assertEquals(
                "GBP",
                result.getCurrencyCode()
        );

        assertEquals(
                new BigDecimal("29.85"),
                result.getTotalAmount()
        );

        assertEquals(
                6,
                result.getItems().size()
        );

        ParsedReceiptItem lastItem =
                result.getItems().get(5);

        assertEquals(
                "Spicy Fishcake and Sausage Mini",
                lastItem.getOriginalName()
        );

        assertEquals(
                1,
                lastItem.getQuantity()
        );

        assertEquals(
                new BigDecimal("4.20"),
                lastItem.getAmount()
        );
    }

    @Test
    void returnsNullForUnrecognizedValues() {
        ParsedReceiptData result =
                parse(
                        "UNKNOWN STORE",
                        null
                );

        assertEquals(
                "UNKNOWN STORE",
                result.getOriginalMerchantName()
        );

        assertNull(
                result.getPaymentDateTime()
        );

        assertNull(
                result.getCurrencyCode()
        );

        assertNull(
                result.getTotalAmount()
        );

        assertEquals(
                0,
                result.getItems().size()
        );
    }

    @Test
    void parsesTotalBeforeJapaneseCashPaymentAmount() {
        String rawText =
                """
                7-ELEVEN
                合計
                ¥375
                現金
                ¥1,000
                お釣
                ¥625
                """;

        ParsedReceiptData result =
                parse(rawText, "ja");

        assertEquals(
                new BigDecimal("375"),
                result.getTotalAmount()
        );
    }

    private ParsedReceiptData parse(
            String rawText,
            String languageCode
    ) {
        VisionOcrResult ocrResult =
                new VisionOcrResult(
                        rawText,
                        languageCode,
                        Collections.emptyList()
                );

        return parser.parse(ocrResult);
    }


}