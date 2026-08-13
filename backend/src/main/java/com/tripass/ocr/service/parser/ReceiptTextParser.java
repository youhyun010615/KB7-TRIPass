package com.tripass.ocr.service.parser;

import com.tripass.ocr.dto.internal.ParsedReceiptData;
import com.tripass.ocr.dto.internal.ParsedReceiptItem;
import com.tripass.ocr.dto.internal.VisionOcrResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
                            + "[ T]+"
                            + "(\\d{1,2}):(\\d{2})(?::(\\d{2}))?"
            );

    // 21/01/2023 13:28
    private static final Pattern YEAR_LAST_DATE_TIME_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})"
                            + "[ T]+"
                            + "(\\d{1,2}):(\\d{2})(?::(\\d{2}))?"
            );

    // 2017年2月26日(日) 20:46
    private static final Pattern EAST_ASIAN_DATE_TIME_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{4})\\s*年\\s*"
                            + "(\\d{1,2})\\s*月\\s*"
                            + "(\\d{1,2})\\s*日"
                            + "(?:\\s*\\([^)]*\\))?"
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
                            + "|(?<!sub)total"
                            + "|\\btot\\b"
                            + "|tot\\s+euro"
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
                            + "(전화|\\btel\\.?\\s*[:#]?|\\bphone\\b|주소|우편번호|"
                            + "번호|주문번호|승인번호|가맹점번호|카드번호|"
                            + "사업자|대표자|발급사|매입사|"
                            + "\\brcs\\b|\\bcaidset\\b|"
                            + "\\bdirection\\b|\\btable\\s*[:#]?|"
                            + "\\b(?:lun|mar|mer|jeu|ven|sam|dim)\\.?\\s*\\d|"
                            + "\\b(?:street|road|avenue|rue)\\b|"
                            + "\\b[A-Z]{1,2}\\d[A-Z\\d]?\\s+\\d[A-Z]{2}\\b|"
                            + "(?:서울|부산|대구|인천|광주|대전|울산|세종|"
                            + "경기|강원|충북|충남|전북|전남|경북|경남|제주)"
                            + ".*(?:시|군|구|로|길)\\s*\\d|"
                            + "\\bno\\.?\\s*\\d+|"
                            + "공급가액|부가세|결제금액|"
                            + "subtotal|change|cash|"
                            + "お預り|お釣|現金|カード|クレジット|消費税|"
                            + "www\\.|https?://|"
                            + "merci|스탬프|광고|"
                            + "device|printed|allergen)"
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

        String rawText = ocrResult.getRawText();

        List<String> lines =
                normalizeLines(rawText);

        String merchantName =
                findMerchantName(lines);

        LocalDateTime paymentDateTime =
                findPaymentDateTime(lines);

        String currencyCode =
                findCurrencyCode(
                        rawText,
                        ocrResult.getDetectedLanguageCode()
                );

        BigDecimal totalAmount =
                findTotalAmount(lines);

        List<ParsedReceiptItem> items =
                findItems(
                        lines,
                        merchantName
                );

        return new ParsedReceiptData(
                merchantName,
                paymentDateTime,
                currencyCode,
                totalAmount,
                items
        );
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
                || rawText.contains("合計")
                || rawText.contains("領収")
                || rawText.contains("領取証")
                || rawText.contains("お預り")
                || rawText.contains("お釣")
                || rawText.contains("消費税");
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
        String line = lines.get(index);

        if (isSubtotalKeyword(line)
                || TAX_PATTERN.matcher(line).find()) {
            return -1;
        }

        if (TOTAL_PATTERN.matcher(line).find()) {
            return index;
        }

        if (index + 1 >= lines.size()) {
            return -1;
        }

        String current = normalizeKeywordToken(line);
        String next = normalizeKeywordToken(lines.get(index + 1));

        if (("합".equals(current) && "계".equals(next))
                || ("合".equals(current) && "計".equals(next))
                || ("总".equals(current) && "计".equals(next))
                || ("總".equals(current) && "計".equals(next))) {
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
            String merchantName
    ) {
        List<ParsedReceiptItem> items =
                new ArrayList<>();

        int displayOrder = 1;

        for (int index = 0;
             index < lines.size();
             index++) {

            String line = lines.get(index);

            if (!isItemNameCandidate(
                    line,
                    merchantName
            )) {
                continue;
            }

            Matcher sameLineMatcher =
                    LINE_AMOUNT_PATTERN.matcher(line);

            if (sameLineMatcher.matches()) {
                String originalName =
                        sameLineMatcher.group(1).trim();

                BigDecimal amount =
                        parseAmount(
                                sameLineMatcher.group(2)
                        );

                String cleanedName =
                        cleanItemName(originalName);

                if (isValidParsedItem(
                        cleanedName,
                        amount
                )) {
                    items.add(
                            new ParsedReceiptItem(
                                    cleanedName,
                                    findQuantity(originalName),
                                    amount,
                                    displayOrder++
                            )
                    );
                }

                continue;
            }

            /*
             * 다음 행에 금액이 있는 형식:
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

                if (amountResult != null) {
                    String cleanedName =
                            cleanItemName(line);

                    if (isValidParsedItem(
                            cleanedName,
                            amountResult.getAmount()
                    )) {
                        items.add(
                                new ParsedReceiptItem(
                                        cleanedName,
                                        findQuantity(line),
                                        amountResult.getAmount(),
                                        displayOrder++
                                )
                        );
                    }

                    index =
                            amountResult.getLineIndex();
                }
            }
        }

        return items;
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
}
