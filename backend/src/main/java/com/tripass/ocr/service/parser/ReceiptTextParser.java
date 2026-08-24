package com.tripass.ocr.service.parser;

import com.tripass.ocr.dto.internal.ParsedReceiptData;
import com.tripass.ocr.dto.internal.ParsedReceiptItem;
import com.tripass.ocr.dto.internal.VisionOcrResult;
import com.tripass.ocr.dto.internal.OcrTextBlock;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// OCR 원문을 영수증 필드별 데이터로 변환한다.
@Component
public class ReceiptTextParser {

    private static final String CURRENCY_SYMBOLS =
            "$€£¥￥₩￦";

    // 2026-08-13 08:44:29
    private static final Pattern YEAR_FIRST_DATE_TIME_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})"
                            + "[ T/]+"
                            + "(\\d{1,2}):(\\d{2})(?::(\\d{2}))?"
            );

    // 21/01/2023 13:28
    private static final Pattern YEAR_LAST_DATE_TIME_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})"
                            + "[ T/]+"
                            + "(\\d{1,2}):(\\d{2})(?::(\\d{2}))?"
            );

    // 2017年2月26日(日) 20:46
    private static final Pattern EAST_ASIAN_DATE_TIME_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{4})\\s*年\\s*"
                            + "(\\d{1,2})\\s*月\\s*"
                            + "(\\d{1,2})\\s*日"
                            + "(?:\\s*[（(][^）)]*[）)])?"
                            + "\\s*"
                            + "(\\d{1,2})[:：](\\d{2})"
                            + "(?::(\\d{2}))?"
            );

    // 품목명과 금액이 같은 줄에 있는 형식
    private static final Pattern LINE_AMOUNT_PATTERN =
            Pattern.compile(
                    "^(.+?)\\s+"
                            + "((?:["
                            + CURRENCY_SYMBOLS
                            + "]\\s*)?-?\\d[\\d.,]*)"
                            + "\\s*(?:[A-Z]{3})?"
                            + "\\s*$"
            );
    // 스위스 식당 형식:
    // 2xLatte Macchiato à 4.50 CHF 9.00
    private static final Pattern SWISS_AT_PRICE_PATTERN =
            Pattern.compile(
                    "^\\s*(\\d+)\\s*[x×]\\s*"
                            + "(.+?)\\s+"
                            + "[àa@]\\s*"
                            + "(-?\\d+[.,]\\d{1,3})\\s+"
                            + "CHF\\s+"
                            + "(-?\\d+[.,]\\d{1,3})"
                            + "\\s*$",
                    Pattern.CASE_INSENSITIVE
            );
    // 품목명 + 수량 + 단가 + 최종 금액 형식
    private static final Pattern COLUMN_ITEM_PATTERN =
            Pattern.compile(
                    "^(.+?)\\s+"
                            + "(\\d+)\\s+"
                            + "(-?\\d+[.,]\\d{1,3})\\s+"
                            + "(-?\\d+[.,]\\d{1,3})"
                            + "\\s*[A-Z]?$"
            );

    // 홍콩·일본 형식: 수량 품목명 금액
    private static final Pattern QUANTITY_NAME_AMOUNT_PATTERN =
            Pattern.compile(
                    "^\\s*(\\d+)\\s+"
                            + "(.+?)\\s+"
                            + "(["
                            + CURRENCY_SYMBOLS
                            + "]?\\s*-?\\d[\\d.,]*)"
                            + "\\s*[!A-Z]?$"
            );

    // 독일 형식: 품목명 금액 세금분류문자
    private static final Pattern EUROPEAN_ITEM_PATTERN =
            Pattern.compile(
                    "^(.+?)\\s+"
                            + "(?:EUR\\s+)?"
                            + "(-?\\d+[.,]\\d{1,3})"
                            + "\\s*[A-Z]?$",
                    Pattern.CASE_INSENSITIVE
            );
    // 프랑스 형식: 수량 품목명 단가 최종금액
    private static final Pattern QUANTITY_UNIT_TOTAL_PATTERN =
            Pattern.compile(
                    "^\\s*(\\d+)\\s+"
                            + "(.+?)\\s+"
                            + "(-?\\d+[.,]\\d{1,3})\\s+"
                            + "(-?\\d+[.,]\\d{1,3})"
                            + "\\s*[A-Z]?$"
            );

    // 수량 표현: 1x, 2 x, 3×
    private static final Pattern EXPLICIT_QUANTITY_PATTERN =
            Pattern.compile(
                    "(?i)(?:^|\\s)(\\d+)\\s*(?:x|×)(?=\\s|$)"
            );

    // 행 앞의 수량 표현: 1 Burger, 2 Expresso
    private static final Pattern LEADING_QUANTITY_PATTERN =
            Pattern.compile(
                    "^\\s*(\\d+)\\s+(.+)$"
            );

    private static final Pattern CURRENCY_CODE_PATTERN =
            Pattern.compile(
                    "(?i)\\b("
                            + "KRW|AED|AUD|BHD|BND|CAD|CHF|CNH|"
                            + "DKK|EUR|GBP|HKD|IDR|JPY|KWD|MYR|"
                            + "NOK|NZD|SAR|SEK|SGD|THB|USD"
                            + ")\\b"
            );

    private static final Pattern TOTAL_PATTERN =
            Pattern.compile(
                    "(?iu)"
                            + "(grand\\s+total"
                            + "|amount\\s+due"
                            + "|balance\\s+due"
                            + "|\\btotal\\b"
                            + "|\\btot\\b"
                            + "|tot\\s+euro"
                            + "|\\bsumme\\b"
                            + "|\\bendbetrag\\b"
                            + "|결제\\s*금액"
                            + "|총\\s*액"
                            + "|합\\s*계"
                            + "|合\\s*計"
                            + "|总\\s*计"
                            + "|總\\s*計)"
            );

    // 세금은 저장하지 않지만 세금 행을 품목에서 제외하기 위해 사용한다.
    private static final Pattern TAX_PATTERN =
            Pattern.compile(
                    "(?iu)"
                            + "(\\b(?:tax|vat|gst|tva|ht)\\b"
                            + "|소비세"
                            + "|消費税"
                            + "|부가세"
                            + "|부가가치세"
                            + "|공급가액)"
            );

    private static final Pattern PAYMENT_METADATA_PATTERN =
            Pattern.compile(
                    "(?iu)"
                            + "(\\b(?:cash|credit|card|visa|mastercard|"
                            + "change|subtotal|receipt|invoice|payment)\\b"
                            + "|현금"
                            + "|카드"
                            + "|영수증"
                            + "|領収証"
                            + "|領取証"
                            + "|お預り"
                            + "|お釣"
                            + "|現金"
                            + "|カード"
                            + "|クレジット)"
            );

    private static final Pattern NON_ITEM_PATTERN =
            Pattern.compile(
                    "(?iu)"
                            + "(전화|"
                            + "\\btel\\.?\\s*[:#]?|"
                            + "\\bphone\\b|"
                            + "주소|"
                            + "우편번호|"
                            + "번호|"
                            + "주문번호|"
                            + "승인번호|"
                            + "가맹점번호|"
                            + "카드번호|"
                            + "사업자|"
                            + "대표자|"
                            + "발급사|"
                            + "매입사|"
                            + "\\brech\\.?\\s*nr\\.?|"
                            + "\\brechnung\\b|"
                            + "\\brcs\\b|"
                            + "\\bcaidset\\b|"
                            + "\\bdirection\\b|"
                            + "\\btable\\s*[:#]?|"
                            + "\\b(?:lun|mar|mer|jeu|ven|sam|dim)"
                            + "\\.?\\s*\\d|"
                            + "\\b(?:street|road|avenue|rue)\\b|"
                            + "\\b(?:str\\.?|strasse|straße)\\s*\\d+[a-z]?\\b|"
                            + "\\b[A-Z]{1,2}\\d[A-Z\\d]?"
                            + "\\s+\\d[A-Z]{2}\\b|"
                            + "(?:서울|부산|대구|인천|광주|대전|울산|세종|"
                            + "경기|강원|충북|충남|전북|전남|경북|경남|제주)"
                            + ".*(?:시|군|구|로|길)\\s*\\d|"
                            + "\\bno\\.?\\s*\\d+|"
                            + "공급가액|"
                            + "부가세|"
                            + "결제금액|"
                            + "\\b(?:subtotal|change|cash)\\b|"
                            + "お預り|"
                            + "お釣|"
                            + "現金|"
                            + "カード|"
                            + "クレジット|"
                            + "消費税|"
                            + "www\\.|"
                            + "https?://|"
                            + "merci|"
                            + "스탬프|"
                            + "광고|"
                            + "device|"
                            + "printed|"
                            + "allergen|"
                            + "\\bpfand\\b|"
                            + "pfandrückgabe|"
                            + "\\bnettobetrag\\b|"
                            + "\\bmwst\\b|"
                            + "\\bmehrwertsteuer\\b|"
                            + "\\bservice\\s+charge\\b|"
                            + "服務費|"
                            + "人數|"
                            + "人数|"
                            + "인원수|"
                            + "\\bguests?\\b|"
                            + "\\bcovers?\\b|"
                            + "小\\s*計|"
                            + "合\\s*計|"
                            + "内消費税|"
                            + "お預り|"
                            + "お釣り?|"
                            + "\\bpurchase\\b)"
            );

    private static final Pattern MERCHANT_EXCLUSION_PATTERN =
            Pattern.compile(
                    "(?iu)"
                            + "(영수증|receipt|invoice|領収|領取|"
                            + "번호|\\btel\\.?\\s*[:#]?|전화|"
                            + "주소|주문일시|주문번호|"
                            + "사업자|가맹점|"
                            + "www\\.|https?://)"
            );

    public ParsedReceiptData parse(
            VisionOcrResult ocrResult
    ) {
        if (ocrResult == null
                || ocrResult.getRawText() == null
                || ocrResult.getRawText().isBlank()) {

            return emptyResult();
        }

        String rawText =
                ocrResult.getRawText();

        // 상호명·날짜·통화·총액은 Vision 원문의 줄 구조를 사용한다.
        List<String> rawLines =
                normalizeLines(rawText);

        // 좌표 기반 행 재구성은 품목 분석에만 사용한다.
        List<String> itemLines =
                createParsingLines(ocrResult);

        String merchantName =
                findMerchantName(rawLines);

        LocalDateTime paymentDateTime =
                findPaymentDateTime(rawLines);

        String currencyCode =
                findCurrencyCode(
                        rawText,
                        ocrResult.getDetectedLanguageCode()
                );

        BigDecimal totalAmount =
                findTotalAmount(rawLines);

        // 기존 Vision 원문 줄을 기준으로 품목을 분석한다.
        List<ParsedReceiptItem> rawItems =
                findItems(
                        rawLines,
                        merchantName,
                        totalAmount
                );

// 좌표를 이용해 재구성한 줄을 기준으로 품목을 분석한다.
        List<ParsedReceiptItem> coordinateItems =
                findItems(
                        itemLines,
                        merchantName,
                        totalAmount
                );

        /*
         * 기울어진 영수증에서는 좌표 행 재구성이 오히려 줄을
         * 잘못 합칠 수 있으므로, 인식한 정상 품목 수가 많은 결과를 사용한다.
         */
        List<ParsedReceiptItem> items =
                selectBetterItems(
                        rawItems,
                        coordinateItems
                );

        return new ParsedReceiptData(
                merchantName,
                paymentDateTime,
                currencyCode,
                totalAmount,
                items
        );
    }

    private List<String> createParsingLines(
            VisionOcrResult ocrResult
    ) {
        List<OcrTextBlock> textBlocks =
                ocrResult.getTextBlocks();

        if (textBlocks == null
                || textBlocks.isEmpty()) {

            return normalizeLines(
                    ocrResult.getRawText()
            );
        }

        List<String> reconstructedLines =
                reconstructLines(textBlocks);

        if (reconstructedLines.isEmpty()) {
            return normalizeLines(
                    ocrResult.getRawText()
            );
        }

        return reconstructedLines;
    }

    private List<String> reconstructLines(
            List<OcrTextBlock> textBlocks
    ) {
        List<PositionedText> positionedTexts =
                textBlocks.stream()
                        .filter(this::hasValidVertices)
                        .map(this::toPositionedText)
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                PositionedText::getCenterY
                                        )
                                        .thenComparingInt(
                                                PositionedText::getLeftX
                                        )
                        )
                        .collect(Collectors.toList());

        if (positionedTexts.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<PositionedText>> rows =
                new ArrayList<>();

        for (PositionedText positionedText
                : positionedTexts) {

            List<PositionedText> matchedRow =
                    findClosestRow(
                            rows,
                            positionedText
                    );

            if (matchedRow == null) {
                matchedRow = new ArrayList<>();
                rows.add(matchedRow);
            }

            matchedRow.add(positionedText);
        }

        rows.sort(
                Comparator.comparingInt(
                        this::calculateRowCenterY
                )
        );

        return rows.stream()
                .map(this::joinRowTexts)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
    }

    private boolean hasValidVertices(
            OcrTextBlock block
    ) {
        return block != null
                && block.getText() != null
                && !block.getText().isBlank()
                && block.getVertices() != null
                && block.getVertices().size() >= 2;
    }

    private PositionedText toPositionedText(
            OcrTextBlock block
    ) {
        int leftX = block.getVertices()
                .stream()
                .mapToInt(
                        OcrTextBlock.Vertex::getX
                )
                .min()
                .orElse(0);

        int rightX = block.getVertices()
                .stream()
                .mapToInt(
                        OcrTextBlock.Vertex::getX
                )
                .max()
                .orElse(leftX);

        int topY = block.getVertices()
                .stream()
                .mapToInt(
                        OcrTextBlock.Vertex::getY
                )
                .min()
                .orElse(0);

        int bottomY = block.getVertices()
                .stream()
                .mapToInt(
                        OcrTextBlock.Vertex::getY
                )
                .max()
                .orElse(topY);

        return new PositionedText(
                block.getText().trim(),
                leftX,
                rightX,
                topY,
                bottomY
        );
    }

    private List<PositionedText> findClosestRow(
            List<List<PositionedText>> rows,
            PositionedText candidate
    ) {
        List<PositionedText> closestRow = null;
        int smallestDifference =
                Integer.MAX_VALUE;

        for (List<PositionedText> row : rows) {
            int rowCenterY =
                    calculateRowCenterY(row);

            int difference =
                    Math.abs(
                            rowCenterY
                                    - candidate.getCenterY()
                    );

            int rowHeight =
                    calculateAverageRowHeight(row);

            int tolerance =
                    Math.max(
                            8,
                            Math.min(
                                    30,
                                    Math.max(
                                            rowHeight,
                                            candidate.getHeight()
                                    ) / 2
                            )
                    );

            if (difference <= tolerance
                    && difference < smallestDifference) {

                closestRow = row;
                smallestDifference = difference;
            }
        }

        return closestRow;
    }

    private int calculateRowCenterY(
            List<PositionedText> row
    ) {
        return (int) row.stream()
                .mapToInt(
                        PositionedText::getCenterY
                )
                .average()
                .orElse(0);
    }

    private int calculateAverageRowHeight(
            List<PositionedText> row
    ) {
        return (int) row.stream()
                .mapToInt(
                        PositionedText::getHeight
                )
                .average()
                .orElse(10);
    }

    private String joinRowTexts(
            List<PositionedText> row
    ) {
        row.sort(
                Comparator.comparingInt(
                        PositionedText::getLeftX
                )
        );

        StringBuilder builder =
                new StringBuilder();

        PositionedText previous = null;

        for (PositionedText current : row) {
            if (previous != null) {
                int gap =
                        current.getLeftX()
                                - previous.getRightX();

                if (gap > 2) {
                    builder.append(' ');
                }
            }

            builder.append(current.getText());
            previous = current;
        }

        return builder.toString()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> normalizeLines(
            String rawText
    ) {
        return Arrays.stream(rawText.split("\\R"))
                .map(String::trim)
                .map(line ->
                        line.replaceAll("\\s+", " ")
                )
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
    }

    private String findMerchantName(
            List<String> lines
    ) {
        int searchLimit =
                Math.min(lines.size(), 12);

        for (int index = 0;
             index < searchLimit;
             index++) {

            String line = lines.get(index);

            if (isMerchantCandidate(line)) {
                return line;
            }
        }

        return null;
    }

    private boolean isMerchantCandidate(
            String line
    ) {
        if (line.length() < 2
                || line.length() > 255) {
            return false;
        }

        if (!line.matches(".*[\\p{L}].*")) {
            return false;
        }

        if (containsDateTime(line)
                || TOTAL_PATTERN.matcher(line).find()
                || TAX_PATTERN.matcher(line).find()
                || PAYMENT_METADATA_PATTERN.matcher(line).find()
                || MERCHANT_EXCLUSION_PATTERN.matcher(line).find()) {

            return false;
        }

        // 숫자와 기호가 대부분인 행은 상호명에서 제외한다.
        String lettersOnly =
                line.replaceAll("[^\\p{L}]", "");

        return lettersOnly.length() >= 2;
    }

    private LocalDateTime findPaymentDateTime(
            List<String> lines
    ) {
        String joinedText =
                String.join(" ", lines);

        LocalDateTime yearFirst =
                parseYearFirstDateTime(joinedText);

        if (yearFirst != null) {
            return yearFirst;
        }

        LocalDateTime eastAsian =
                parseEastAsianDateTime(joinedText);

        if (eastAsian != null) {
            return eastAsian;
        }

        return parseYearLastDateTime(joinedText);
    }

    private LocalDateTime parseYearFirstDateTime(
            String text
    ) {
        Matcher matcher =
                YEAR_FIRST_DATE_TIME_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        return createDateTime(
                parseInteger(matcher.group(1)),
                parseInteger(matcher.group(2)),
                parseInteger(matcher.group(3)),
                parseInteger(matcher.group(4)),
                parseInteger(matcher.group(5)),
                parseOptionalSecond(matcher.group(6))
        );
    }

    private LocalDateTime parseYearLastDateTime(
            String text
    ) {
        Matcher matcher =
                YEAR_LAST_DATE_TIME_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        int firstValue =
                parseInteger(matcher.group(1));

        int secondValue =
                parseInteger(matcher.group(2));

        int month =
                firstValue > 12
                        ? secondValue
                        : firstValue;

        int day =
                firstValue > 12
                        ? firstValue
                        : secondValue;

        return createDateTime(
                parseInteger(matcher.group(3)),
                month,
                day,
                parseInteger(matcher.group(4)),
                parseInteger(matcher.group(5)),
                parseOptionalSecond(matcher.group(6))
        );
    }

    private LocalDateTime parseEastAsianDateTime(
            String text
    ) {
        Matcher matcher =
                EAST_ASIAN_DATE_TIME_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        return createDateTime(
                parseInteger(matcher.group(1)),
                parseInteger(matcher.group(2)),
                parseInteger(matcher.group(3)),
                parseInteger(matcher.group(4)),
                parseInteger(matcher.group(5)),
                parseOptionalSecond(matcher.group(6))
        );
    }

    private LocalDateTime createDateTime(
            int year,
            int month,
            int day,
            int hour,
            int minute,
            int second
    ) {
        try {
            LocalDate date =
                    LocalDate.of(
                            year,
                            month,
                            day
                    );

            LocalTime time =
                    LocalTime.of(
                            hour,
                            minute,
                            second
                    );

            return LocalDateTime.of(date, time);

        } catch (DateTimeException exception) {
            return null;
        }
    }

    private String findCurrencyCode(
            String rawText,
            String detectedLanguageCode
    ) {
        Matcher codeMatcher =
                CURRENCY_CODE_PATTERN.matcher(rawText);

        if (codeMatcher.find()) {
            return codeMatcher.group(1)
                    .toUpperCase(Locale.ROOT);
        }

        String lowerText =
                rawText.toLowerCase(Locale.ROOT);

        if (rawText.contains("€")
                || lowerText.contains("euro")) {
            return "EUR";
        }

        if (rawText.contains("£")) {
            return "GBP";
        }

        if (rawText.contains("₩")
                || rawText.contains("￦")) {
            return "KRW";
        }

        /*
         * Vision이 일본 영수증을 중국어로 감지할 수 있으므로
         * 감지 언어보다 일본식 날짜와 영수증 단어를 우선한다.
         */
        if (looksLikeJapaneseReceipt(rawText)) {
            return "JPY";
        }

        if (rawText.contains("¥")
                || rawText.contains("￥")) {

            if (detectedLanguageCode != null
                    && detectedLanguageCode
                    .toLowerCase(Locale.ROOT)
                    .startsWith("zh")) {

                return "CNH";
            }

            return "JPY";
        }

        if (detectedLanguageCode != null
                && detectedLanguageCode
                .toLowerCase(Locale.ROOT)
                .startsWith("ko")
                && looksLikeKoreanReceipt(rawText)) {

            return "KRW";
        }

        if (rawText.contains("$")) {
            return "USD";
        }

        return null;
    }

    private boolean looksLikeJapaneseReceipt(
            String rawText
    ) {
        return EAST_ASIAN_DATE_TIME_PATTERN
                .matcher(rawText)
                .find()
                || rawText.contains("領収")
                || rawText.contains("領取証")
                || rawText.contains("お預り")
                || rawText.contains("お釣")
                || rawText.contains("消費税")
                || rawText.contains("現金")
                || rawText.contains("カード")
                || rawText.contains("クレジット");
    }

    private boolean looksLikeKoreanReceipt(
            String rawText
    ) {
        return rawText.contains("결제금액")
                || rawText.contains("공급가액")
                || rawText.contains("부가세")
                || rawText.contains("주문일시")
                || rawText.contains("영수증");
    }

    private BigDecimal findTotalAmount(
            List<String> lines
    ) {
        for (int index = 0;
             index < lines.size();
             index++) {

            String currentLine =
                    lines.get(index);

            int keywordEndIndex =
                    findFinalTotalKeywordEndIndex(
                            lines,
                            index
                    );

            if (keywordEndIndex < 0) {
                continue;
            }

            BigDecimal sameLineAmount =
                    extractLastAmount(currentLine);

            if (sameLineAmount != null
                    && sameLineAmount.signum() > 0
                    && !isLikelyIdentifier(sameLineAmount)) {
                return sameLineAmount;
            }

            int lastIndex =
                    Math.min(
                            keywordEndIndex + 5,
                            lines.size() - 1
                    );

            BigDecimal selectedAmount = null;

            for (int nextIndex = keywordEndIndex + 1;
                 nextIndex <= lastIndex;
                 nextIndex++) {

                String nextLine =
                        lines.get(nextIndex);

                if (isTotalSearchBoundary(nextLine)) {
                    break;
                }

                BigDecimal amount =
                        extractStandaloneAmount(nextLine);

                if (amount == null) {
                    amount =
                            extractAmountAfterDelimiter(nextLine);
                }

                if (amount != null
                        && amount.signum() > 0
                        && !isLikelyIdentifier(amount)
                        && (selectedAmount == null
                        || amount.compareTo(selectedAmount) > 0)) {
                    // OCR 줄 순서가 어긋나 합계 뒤에 품목 금액이나 0이
                    // 함께 나타날 수 있으므로 가장 큰 정상 금액을 선택한다.
                    selectedAmount = amount;
                }
            }

            if (selectedAmount != null) {
                return selectedAmount;
            }
        }

        return null;
    }

    private int findFinalTotalKeywordEndIndex(
            List<String> lines,
            int index
    ) {
        String line =
                lines.get(index);

        if (isSubtotalKeyword(line)
                || TAX_PATTERN.matcher(line).find()
                || isItemColumnHeader(line)) {

            return -1;
        }

        if (TOTAL_PATTERN.matcher(line).find()) {
            return index;
        }

        if (index + 1 >= lines.size()) {
            return -1;
        }

        String current =
                normalizeKeywordToken(line);

        String next =
                normalizeKeywordToken(
                        lines.get(index + 1)
                );

        if (("합".equals(current)
                && "계".equals(next))
                || ("合".equals(current)
                && "計".equals(next))
                || ("总".equals(current)
                && "计".equals(next))
                || ("總".equals(current)
                && "計".equals(next))) {

            return index + 1;
        }

        return -1;
    }

    private String normalizeKeywordToken(String value) {
        return value == null
                ? ""
                : value.replaceAll("[\\s|:：]", "");
    }

    private boolean isSubtotalKeyword(String line) {
        return Pattern.compile(
                        "(?iu)(subtotal|sub\\s*total|小\\s*計|소\\s*계)"
                )
                .matcher(line)
                .find();
    }

    private boolean isTotalSearchBoundary(String line) {
        return isSubtotalKeyword(line)
                || TOTAL_PATTERN.matcher(line).find()
                || TAX_PATTERN.matcher(line).find()
                || PAYMENT_METADATA_PATTERN.matcher(line).find();
    }

    private List<ParsedReceiptItem> findItems(
            List<String> lines,
            String merchantName,
            BigDecimal totalAmount
    ) {
        List<ParsedReceiptItem> items =
                new ArrayList<>();

        int displayOrder = 1;

        for (int index = 0;
             index < lines.size();
             index++) {

            String line =
                    lines.get(index);

            /*
             * 하나 이상의 품목을 찾은 후 합계·소계 영역이 나오면
             * 이후 결제·세금·잔돈 정보가 품목으로 들어가지 않도록
             * 품목 탐색을 종료한다.
             */
            if (!items.isEmpty()
                    && isItemSectionEnd(
                    lines,
                    index
            )) {
                break;
            }

            if (!isItemNameCandidate(
                    line,
                    merchantName
            )) {
                continue;
            }

            /*
             * 스위스 식당 형식:
             *
             * 2xLatte Macchiato à 4.50 CHF 9.00
             *
             * 수량, 품목명, 단가, 통화, 최종 품목금액 순서다.
             */
            ParsedReceiptItem swissAtPriceItem =
                    parseSwissAtPriceItem(
                            line,
                            displayOrder
                    );

            if (isAcceptableItem(
                    swissAtPriceItem,
                    totalAmount
            )) {
                items.add(swissAtPriceItem);
                displayOrder++;
                continue;
            }

            /*
             * 프랑스 형식:
             *
             * 2 Expresso 2.50 5.00
             *
             * 수량, 품목명, 단가, 최종 품목금액 순서다.
             */
            ParsedReceiptItem quantityUnitTotalItem =
                    parseQuantityUnitTotalItem(
                            line,
                            displayOrder
                    );

            if (isAcceptableItem(
                    quantityUnitTotalItem,
                    totalAmount
            )) {
                items.add(quantityUnitTotalItem);
                displayOrder++;
                continue;
            }

            /*
             * 스위스 마트 형식:
             *
             * Bio Aelplerbrot 1 3.20 3.20
             *
             * 품목명, 수량, 단가, 최종 품목금액 순서다.
             */
            ParsedReceiptItem columnItem =
                    parseColumnItem(
                            line,
                            displayOrder
                    );

            if (isAcceptableItem(
                    columnItem,
                    totalAmount
            )) {
                items.add(columnItem);
                displayOrder++;
                continue;
            }

            /*
             * 홍콩·일본 형식:
             *
             * 1 水 $14
             * 1 商品名 ¥151
             */
            ParsedReceiptItem quantityItem =
                    parseQuantityNameAmountItem(
                            line,
                            displayOrder
                    );

            if (isAcceptableItem(
                    quantityItem,
                    totalAmount
            )) {
                items.add(quantityItem);
                displayOrder++;
                continue;
            }

            /*
             * 독일 형식:
             *
             * Paprika-Mix 0,99 A
             *
             * 금액 뒤의 A/B는 세금 분류 문자다.
             */
            ParsedReceiptItem europeanItem =
                    parseEuropeanItem(
                            line,
                            displayOrder
                    );

            if (isAcceptableItem(
                    europeanItem,
                    totalAmount
            )) {
                items.add(europeanItem);
                displayOrder++;
                continue;
            }

            /*
             * 일반적인 같은 행 형식:
             *
             * Coffee 5.00
             * 商品名 ¥151
             */
            Matcher sameLineMatcher =
                    LINE_AMOUNT_PATTERN.matcher(line);

            if (sameLineMatcher.matches()) {
                String originalName =
                        sameLineMatcher.group(1)
                                .trim();

                BigDecimal amount =
                        parseAmount(
                                sameLineMatcher.group(2)
                        );

                String cleanedName =
                        cleanItemName(originalName);

                ParsedReceiptItem parsedItem =
                        createItem(
                                cleanedName,
                                findQuantity(originalName),
                                amount,
                                displayOrder
                        );

                if (isAcceptableItem(
                        parsedItem,
                        totalAmount
                )) {
                    items.add(parsedItem);
                    displayOrder++;
                }

                continue;
            }

            /*
             * Vision 좌표를 적용해도 이름과 금액이 다른 줄로
             * 분리된 경우에 사용하는 보조 처리:
             *
             * 1 Burger
             * 9.00
             *
             * 1x Coffee
             * £5.10
             */
            if (hasLeadingOrExplicitQuantity(line)) {
                AmountSearchResult amountResult =
                        findNearbyAmount(
                                lines,
                                index + 1,
                                3
                        );

                if (amountResult == null) {
                    continue;
                }

                ParsedReceiptItem parsedItem =
                        createItem(
                                cleanItemName(line),
                                findQuantity(line),
                                amountResult.getAmount(),
                                displayOrder
                        );

                if (isAcceptableItem(
                        parsedItem,
                        totalAmount
                )) {
                    items.add(parsedItem);
                    displayOrder++;
                }

                /*
                 * 현재 품목에 사용한 금액 행을 반복해서 분석하지
                 * 않도록 반복문 위치를 해당 금액 행까지 이동한다.
                 */
                index =
                        amountResult.getLineIndex();
            }
        }

        return items;
    }

    private ParsedReceiptItem parseColumnItem(
            String line,
            int displayOrder
    ) {
        Matcher matcher =
                COLUMN_ITEM_PATTERN.matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        String name =
                matcher.group(1).trim();

        Integer quantity =
                parseInteger(matcher.group(2));

        BigDecimal finalAmount =
                parseAmount(matcher.group(4));

        return createItem(
                name,
                quantity,
                finalAmount,
                displayOrder
        );
    }

    private ParsedReceiptItem parseQuantityNameAmountItem(
            String line,
            int displayOrder
    ) {
        Matcher matcher =
                QUANTITY_NAME_AMOUNT_PATTERN.matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        int leadingNumber =
                parseInteger(matcher.group(1));

        /*
         * 홍콩 영수증의 25처럼 큰 앞자리 숫자는
         * 수량보다 메뉴 번호일 가능성이 높다.
         */
        Integer quantity =
                leadingNumber >= 1
                        && leadingNumber <= 9
                        ? leadingNumber
                        : null;

        String name =
                matcher.group(2).trim();

        BigDecimal amount =
                parseAmount(matcher.group(3));

        return createItem(
                name,
                quantity,
                amount,
                displayOrder
        );
    }

    private ParsedReceiptItem parseEuropeanItem(
            String line,
            int displayOrder
    ) {
        Matcher matcher =
                EUROPEAN_ITEM_PATTERN.matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        String name =
                matcher.group(1).trim();

        /*
         * 숫자와 기호가 포함된 헤더·식별번호 행은 제외한다.
         */
        if (!name.matches(".*[\\p{L}].*")) {
            return null;
        }

        BigDecimal amount =
                parseAmount(matcher.group(2));

        return createItem(
                cleanItemName(name),
                findQuantity(name),
                amount,
                displayOrder
        );
    }

    private ParsedReceiptItem createItem(
            String name,
            Integer quantity,
            BigDecimal amount,
            int displayOrder
    ) {
        if (!isValidParsedItem(
                name,
                amount
        )) {
            return null;
        }

        return new ParsedReceiptItem(
                name,
                quantity,
                amount,
                displayOrder
        );
    }

    private boolean isAcceptableItem(
            ParsedReceiptItem item,
            BigDecimal totalAmount
    ) {
        if (item == null
                || item.getAmount() == null) {
            return false;
        }

        if (item.getAmount().signum() < 0) {
            return false;
        }

        /*
         * 단일 품목 금액이 총 결제금액보다 크면
         * 받은 금액·전화번호 등일 가능성이 높다.
         */
        return totalAmount == null
                || item.getAmount()
                .compareTo(totalAmount) <= 0;
    }

    private boolean isItemSectionEnd(
            List<String> lines,
            int index
    ) {
        String line =
                lines.get(index);

        if (isSubtotalKeyword(line)
                || TOTAL_PATTERN.matcher(line).find()) {
            return true;
        }

        if (index + 1 >= lines.size()) {
            return false;
        }

        String current =
                normalizeKeywordToken(line);

        String next =
                normalizeKeywordToken(
                        lines.get(index + 1)
                );

        return ("小".equals(current)
                && "計".equals(next))
                || ("合".equals(current)
                && "計".equals(next))
                || ("总".equals(current)
                && "计".equals(next))
                || ("總".equals(current)
                && "計".equals(next));
    }

    private boolean isItemNameCandidate(
            String line,
            String merchantName
    ) {
        if (merchantName != null
                && line.equals(merchantName)) {
            return false;
        }

        if (!line.matches(".*[\\p{L}].*")) {
            return false;
        }

        return !containsDateTime(line)
                && !TOTAL_PATTERN.matcher(line).find()
                && !TAX_PATTERN.matcher(line).find()
                && !PAYMENT_METADATA_PATTERN.matcher(line).find()
                && !NON_ITEM_PATTERN.matcher(line).find();
    }

    private boolean hasLeadingOrExplicitQuantity(
            String line
    ) {
        return EXPLICIT_QUANTITY_PATTERN
                .matcher(line)
                .find()
                || LEADING_QUANTITY_PATTERN
                .matcher(line)
                .matches();
    }

    private AmountSearchResult findNearbyAmount(
            List<String> lines,
            int startIndex,
            int maximumDistance
    ) {
        BigDecimal selectedAmount = null;
        int selectedIndex = -1;

        int lastIndex =
                Math.min(
                        startIndex + maximumDistance - 1,
                        lines.size() - 1
                );

        for (int index = startIndex;
             index <= lastIndex;
             index++) {

            String candidateLine =
                    lines.get(index);

            if (TOTAL_PATTERN.matcher(candidateLine).find()
                    || isSubtotalKeyword(candidateLine)
                    || TAX_PATTERN.matcher(candidateLine).find()
                    || PAYMENT_METADATA_PATTERN.matcher(candidateLine).find()) {
                break;
            }

            // 다른 품목 행을 만나면 현재 품목 검색을 중단한다.
            if (index > startIndex
                    && hasLeadingOrExplicitQuantity(candidateLine)
                    && candidateLine.matches(".*[\\p{L}].*")) {
                break;
            }

            BigDecimal amount =
                    extractStandaloneAmount(candidateLine);

            if (amount != null
                    && amount.signum() >= 0
                    && !isLikelyIdentifier(amount)) {
                /*
                 * 수량 2, 단가 2.50, 합계 5.00처럼
                 * 연속된 금액이 있으면 마지막 금액을 품목 금액으로 사용한다.
                 */
                selectedAmount = amount;
                selectedIndex = index;
            }
        }

        if (selectedAmount == null) {
            return null;
        }

        return new AmountSearchResult(
                selectedAmount,
                selectedIndex
        );
    }

    private boolean isValidParsedItem(
            String itemName,
            BigDecimal amount
    ) {
        return itemName != null
                && !itemName.isBlank()
                && itemName.matches(".*[\\p{L}].*")
                && amount != null
                && amount.signum() >= 0
                && !NON_ITEM_PATTERN.matcher(itemName).find();
    }

    private Integer findQuantity(
            String itemName
    ) {
        Matcher explicitMatcher =
                EXPLICIT_QUANTITY_PATTERN.matcher(itemName);

        if (explicitMatcher.find()) {
            return parseInteger(
                    explicitMatcher.group(1)
            );
        }

        Matcher leadingMatcher =
                LEADING_QUANTITY_PATTERN.matcher(itemName);

        if (leadingMatcher.matches()) {
            return parseInteger(
                    leadingMatcher.group(1)
            );
        }

        return null;
    }

    private String cleanItemName(
            String itemName
    ) {
        return itemName
                .replaceFirst(
                        "(?i)^\\s*\\d+\\s*(?:x|×)?\\s+",
                        ""
                )
                .trim();
    }

    private BigDecimal extractLastAmount(
            String line
    ) {
        Matcher matcher =
                Pattern.compile(
                                "(["
                                        + CURRENCY_SYMBOLS
                                        + "]?\\s*-?\\d[\\d.,]*)"
                                        + "\\s*(?:[A-Z]{3})?"
                                        + "\\s*[^\\d.,]*$"
                        )
                        .matcher(line);

        if (!matcher.find()) {
            return null;
        }

        return parseAmount(
                matcher.group(1)
        );
    }

    private BigDecimal extractStandaloneAmount(
            String line
    ) {
        Matcher matcher =
                Pattern.compile(
                                "^\\s*(["
                                        + CURRENCY_SYMBOLS
                                        + "]\\s*)?"
                                        + "(-?\\d[\\d.,]*)"
                                        + "\\s*(?:[A-Z]{3})?"
                                        + "\\s*[^\\d.,]*$"
                        )
                        .matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        String symbol =
                matcher.group(1) == null
                        ? ""
                        : matcher.group(1);

        return parseAmount(
                symbol + matcher.group(2)
        );
    }

    // ": 1,800"처럼 구분자 뒤에 있는 금액을 추출한다.
    private BigDecimal extractAmountAfterDelimiter(
            String line
    ) {
        Matcher matcher =
                Pattern.compile(
                                "[:：]\\s*(["
                                        + CURRENCY_SYMBOLS
                                        + "]?\\s*-?\\d[\\d.,]*)"
                                        + "\\s*$"
                        )
                        .matcher(line);

        if (!matcher.find()) {
            return null;
        }

        return parseAmount(
                matcher.group(1)
        );
    }

    private BigDecimal parseAmount(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.replaceAll(
                        "[$€£¥￥₩￦\\s]",
                        ""
                );

        int lastComma =
                normalized.lastIndexOf(',');

        int lastDot =
                normalized.lastIndexOf('.');

        if (lastComma >= 0
                && lastDot >= 0) {

            if (lastComma > lastDot) {
                normalized =
                        normalized
                                .replace(".", "")
                                .replace(',', '.');
            } else {
                normalized =
                        normalized.replace(",", "");
            }

        } else if (lastComma >= 0) {
            normalized =
                    normalizeSingleSeparator(
                            normalized,
                            ','
                    );

        } else if (lastDot >= 0) {
            normalized =
                    normalizeSingleSeparator(
                            normalized,
                            '.'
                    );
        }

        try {
            return new BigDecimal(normalized);

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizeSingleSeparator(
            String value,
            char separator
    ) {
        int separatorIndex =
                value.lastIndexOf(separator);

        int decimalLength =
                value.length()
                        - separatorIndex
                        - 1;

        // 구분자 뒤가 3자리면 천 단위 구분자로 판단한다.
        if (decimalLength == 3) {
            return value.replace(
                    String.valueOf(separator),
                    ""
            );
        }

        if (separator == ',') {
            return value.replace(',', '.');
        }

        return value;
    }

    private boolean isLikelyIdentifier(
            BigDecimal amount
    ) {
        return amount.scale() <= 0
                && amount.compareTo(
                new BigDecimal("999999999")
        ) > 0;
    }

    private boolean containsDateTime(
            String text
    ) {
        return YEAR_FIRST_DATE_TIME_PATTERN
                .matcher(text)
                .find()
                || YEAR_LAST_DATE_TIME_PATTERN
                .matcher(text)
                .find()
                || EAST_ASIAN_DATE_TIME_PATTERN
                .matcher(text)
                .find();
    }

    private int parseOptionalSecond(
            String value
    ) {
        return value == null
                ? 0
                : parseInteger(value);
    }

    private int parseInteger(
            String value
    ) {
        try {
            return Integer.parseInt(value);

        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private ParsedReceiptData emptyResult() {
        return new ParsedReceiptData(
                null,
                null,
                null,
                null,
                Collections.emptyList()
        );
    }

    private static class PositionedText {

        private final String text;
        private final int leftX;
        private final int rightX;
        private final int topY;
        private final int bottomY;

        private PositionedText(
                String text,
                int leftX,
                int rightX,
                int topY,
                int bottomY
        ) {
            this.text = text;
            this.leftX = leftX;
            this.rightX = rightX;
            this.topY = topY;
            this.bottomY = bottomY;
        }

        private String getText() {
            return text;
        }

        private int getLeftX() {
            return leftX;
        }

        private int getRightX() {
            return rightX;
        }

        private int getCenterY() {
            return (topY + bottomY) / 2;
        }

        private int getHeight() {
            return Math.max(
                    1,
                    bottomY - topY
            );
        }
    }

    private ParsedReceiptItem parseQuantityUnitTotalItem(
            String line,
            int displayOrder
    ) {
        Matcher matcher =
                QUANTITY_UNIT_TOTAL_PATTERN.matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        Integer quantity =
                parseInteger(matcher.group(1));

        String name =
                matcher.group(2).trim();

        BigDecimal totalAmount =
                parseAmount(matcher.group(4));

        return createItem(
                name,
                quantity,
                totalAmount,
                displayOrder
        );
    }

    private static class AmountSearchResult {

        private final BigDecimal amount;
        private final int lineIndex;

        private AmountSearchResult(
                BigDecimal amount,
                int lineIndex
        ) {
            this.amount = amount;
            this.lineIndex = lineIndex;
        }

        private BigDecimal getAmount() {
            return amount;
        }

        private int getLineIndex() {
            return lineIndex;
        }
    }

    private List<ParsedReceiptItem> selectBetterItems(
            List<ParsedReceiptItem> rawItems,
            List<ParsedReceiptItem> coordinateItems
    ) {
        if (rawItems == null
                || rawItems.isEmpty()) {

            return coordinateItems == null
                    ? Collections.emptyList()
                    : coordinateItems;
        }

        if (coordinateItems == null
                || coordinateItems.isEmpty()) {

            return rawItems;
        }

        /*
         * 현재는 품목 수가 더 많은 결과를 우선한다.
         *
         * 프랑스처럼 원문 줄 구조가 더 정확한 경우 rawItems가 선택되고,
         * 일본 세븐일레븐처럼 줄 순서가 깨진 경우에는 좌표 결과가
         * 더 많은 품목을 복원하면 coordinateItems가 선택된다.
         */
        return coordinateItems.size() > rawItems.size()
                ? coordinateItems
                : rawItems;
    }

    private boolean isItemColumnHeader(
            String line
    ) {
        if (line == null
                || line.isBlank()) {
            return false;
        }

        String lowerLine =
                line.toLowerCase(Locale.ROOT);

        int matchedHeaderCount = 0;

        if (lowerLine.contains("artikel")
                || lowerLine.contains("article")) {
            matchedHeaderCount++;
        }

        if (lowerLine.contains("menge")
                || lowerLine.contains("quantity")) {
            matchedHeaderCount++;
        }

        if (lowerLine.contains("preis")
                || lowerLine.contains("price")) {
            matchedHeaderCount++;
        }

        if (lowerLine.contains("aktion")
                || lowerLine.contains("action")) {
            matchedHeaderCount++;
        }

        if (lowerLine.contains("total")) {
            matchedHeaderCount++;
        }

        return matchedHeaderCount >= 2;
    }

    private ParsedReceiptItem parseSwissAtPriceItem(
            String line,
            int displayOrder
    ) {
        Matcher matcher =
                SWISS_AT_PRICE_PATTERN.matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        Integer quantity =
                parseInteger(matcher.group(1));

        String name =
                matcher.group(2).trim();

        BigDecimal finalAmount =
                parseAmount(matcher.group(4));

        return createItem(
                name,
                quantity,
                finalAmount,
                displayOrder
        );
    }

}
