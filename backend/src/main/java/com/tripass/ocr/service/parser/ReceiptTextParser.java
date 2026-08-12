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

//OCR 원문을 영수증 필드별 데이터로 변환한다.
@Component
public class ReceiptTextParser {

    //연도가 앞에 있는 결제일시 형식
    private static final Pattern YEAR_FIRST_DATE_TIME_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})"
                            + "[ T]+"
                            + "(\\d{1,2}):(\\d{2})(?::(\\d{2}))?"
            );

    //연도가 뒤에 있는 결제일시 형식
    private static final Pattern YEAR_LAST_DATE_TIME_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})"
                            + "[ T]+"
                            + "(\\d{1,2}):(\\d{2})(?::(\\d{2}))?"
            );

    //영수증 줄의 마지막 금액
    private static final Pattern LINE_AMOUNT_PATTERN =
            Pattern.compile(
                    "^(.+?)\\s+"
                            + "([$€£¥₩]?\\s*-?\\d[\\d.,]*)"
                            + "\\s*(?:[A-Z]{3})?$"
            );

    //수량 표현 - 예: 2 x, 2×
    private static final Pattern QUANTITY_PATTERN =
            Pattern.compile(
                    "(?i)(?:^|\\s)(\\d+)\\s*[x×]"
            );

    //ISO 4217 통화 코드
    private static final Pattern CURRENCY_CODE_PATTERN =
            Pattern.compile(
                    "(?i)\\b("
                            + "USD|JPY|EUR|GBP|CNY|KRW|AUD|CAD|CHF|"
                            + "HKD|SGD|THB|VND|PHP|IDR|MYR|TWD|NZD"
                            + ")\\b"
            );

    //총액을 나타내는 단어
    private static final Pattern TOTAL_PATTERN =
            Pattern.compile(
                    "(?iu)"
                            + "(grand\\s+total|amount\\s+due|balance\\s+due|"
                            + "(?<!sub)total|合計|総計|总计|總計|합계)"
            );

    //세금을 나타내는 단어
    private static final Pattern TAX_PATTERN =
            Pattern.compile(
                    "(?iu)(\\b(?:tax|vat|gst)\\b|"
                            + "消費税|税|세금|부가세)"
            );

    //품목이 아닌 결제 정보 단어
    private static final Pattern PAYMENT_METADATA_PATTERN =
            Pattern.compile(
                    "(?iu)(\\b(?:cash|credit|card|visa|mastercard|"
                            + "change|subtotal|receipt|invoice)\\b|"
                            + "현금|카드|영수증)"
            );

    /**
     * Vision OCR 결과를 구조화한 영수증 데이터로 변환한다.
     */
    public ParsedReceiptData parse(
            VisionOcrResult ocrResult
    ) {
        if (ocrResult == null
                || ocrResult.getRawText() == null
                || ocrResult.getRawText().isBlank()) {

            return emptyResult();
        }

        List<String> lines =
                normalizeLines(ocrResult.getRawText());

        String merchantName =
                findMerchantName(lines);

        LocalDateTime paymentDateTime =
                findPaymentDateTime(lines);

        String currencyCode =
                findCurrencyCode(
                        ocrResult.getRawText(),
                        ocrResult.getDetectedLanguageCode()
                );

        BigDecimal totalAmount =
                findAmountByKeyword(
                        lines,
                        TOTAL_PATTERN
                );

        BigDecimal taxAmount =
                findAmountByKeyword(
                        lines,
                        TAX_PATTERN
                );

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
                taxAmount,
                items
        );
    }

    /**
     * OCR 원문을 공백이 정리된 줄 목록으로 변환한다.
     */
    private List<String> normalizeLines(String rawText) {
        return Arrays.stream(rawText.split("\\R"))
                .map(String::trim)
                .map(line ->
                        line.replaceAll("\\s+", " ")
                )
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 영수증 상단에서 상호명 후보를 찾는다.
     */
    private String findMerchantName(
            List<String> lines
    ) {
        for (String line : lines) {
            if (isMerchantCandidate(line)) {
                return line;
            }
        }

        return null;
    }

    private boolean isMerchantCandidate(String line) {
        if (line.length() < 2
                || line.length() > 255) {
            return false;
        }

        if (containsDateTime(line)
                || TOTAL_PATTERN.matcher(line).find()
                || TAX_PATTERN.matcher(line).find()
                || PAYMENT_METADATA_PATTERN.matcher(line).find()) {

            return false;
        }

        //숫자와 기호로만 이루어진 줄은 상호명에서 제외한다.
        return line.matches(".*[\\p{L}].*");
    }

    /**
     * 원문에서 결제일시를 찾는다.
     */
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

        /*
         * 첫 번째 값이 12보다 크면 일/월 형식으로 판단한다.
         * 둘 다 12 이하인 모호한 형식은 월/일 형식으로 처리하고
         * 사용자가 분석 화면에서 수정할 수 있게 한다.
         */
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

    /**
     * ISO 통화 코드를 우선 찾고, 없으면 통화 기호로 판단한다.
     */
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

        if (rawText.contains("€")) {
            return "EUR";
        }

        if (rawText.contains("£")) {
            return "GBP";
        }

        if (rawText.contains("₩")) {
            return "KRW";
        }

        if (rawText.contains("¥")) {
            if (detectedLanguageCode != null
                    && detectedLanguageCode
                    .toLowerCase(Locale.ROOT)
                    .startsWith("zh")) {

                return "CNY";
            }

            return "JPY";
        }

        if (rawText.contains("$")) {
            return "USD";
        }

        return null;
    }

    /**
     * 총액이나 세금 키워드가 있는 줄에서 마지막 금액을 찾는다.
     */
    private BigDecimal findAmountByKeyword(
            List<String> lines,
            Pattern keywordPattern
    ) {
        for (int index = lines.size() - 1;
             index >= 0;
             index--) {

            String line = lines.get(index);

            if (!keywordPattern.matcher(line).find()) {
                continue;
            }

            // Total £29.85 형태
            BigDecimal amount = extractLastAmount(line);

            if (amount != null) {
                return amount;
            }

            // Total
            // £29.85 형태
            int lastIndex =
                    Math.min(index + 3, lines.size() - 1);

            for (int nextIndex = index + 1;
                 nextIndex <= lastIndex;
                 nextIndex++) {

                amount =
                        extractStandaloneAmount(lines.get(nextIndex));

                if (amount != null) {
                    return amount;
                }
            }
        }

        return null;
    }

    /**
     * 품목명과 줄 마지막 금액을 추출한다.
     */
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

            if (line.equals(merchantName)
                    || containsDateTime(line)
                    || TOTAL_PATTERN.matcher(line).find()
                    || TAX_PATTERN.matcher(line).find()
                    || PAYMENT_METADATA_PATTERN.matcher(line).find()) {

                continue;
            }

            // 품목명과 금액이 같은 줄인 경우
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

                if (!cleanedName.isBlank()
                        && amount != null
                        && cleanedName.matches(
                        ".*[\\p{L}].*"
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

            // 품목명 다음 줄에 금액이 있는 경우
            if (QUANTITY_PATTERN.matcher(line).find()
                    && index + 1 < lines.size()) {

                BigDecimal nextLineAmount =
                        extractStandaloneAmount(
                                lines.get(index + 1)
                        );

                if (nextLineAmount != null) {
                    String cleanedName =
                            cleanItemName(line);

                    if (!cleanedName.isBlank()
                            && cleanedName.matches(
                            ".*[\\p{L}].*"
                    )) {

                        items.add(
                                new ParsedReceiptItem(
                                        cleanedName,
                                        findQuantity(line),
                                        nextLineAmount,
                                        displayOrder++
                                )
                        );
                    }

                    index++;
                }
            }
        }

        return items;
    }

    private Integer findQuantity(String itemName) {
        Matcher matcher =
                QUANTITY_PATTERN.matcher(itemName);

        if (!matcher.find()) {
            return null;
        }

        return parseInteger(matcher.group(1));
    }

    private String cleanItemName(String itemName) {
        return itemName
                .replaceFirst(
                        "(?i)^\\s*\\d+\\s*[x×]\\s*",
                        ""
                )
                .trim();
    }

    /**
     * 문자열 마지막에 있는 금액을 추출한다.
     */
    private BigDecimal extractLastAmount(String line) {
        Matcher matcher =
                Pattern.compile(
                                "([$€£¥₩]?\\s*-?\\d[\\d.,]*)"
                                        + "\\s*(?:[A-Z]{3})?\\s*$"
                        )
                        .matcher(line);

        if (!matcher.find()) {
            return null;
        }

        return parseAmount(matcher.group(1));
    }

    /**
     * 국가별 쉼표·마침표 표기를 BigDecimal로 변환한다.
     */
    private BigDecimal parseAmount(String value) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.replaceAll(
                        "[$€£¥₩\\s]",
                        ""
                );

        int lastComma =
                normalized.lastIndexOf(',');

        int lastDot =
                normalized.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                normalized = normalized
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

        //구분자 뒤가 세 자리면 천 단위 구분자로 판단한다.
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

    private boolean containsDateTime(String text) {
        return YEAR_FIRST_DATE_TIME_PATTERN
                .matcher(text)
                .find()
                || YEAR_LAST_DATE_TIME_PATTERN
                .matcher(text)
                .find();
    }

    private int parseOptionalSecond(String value) {
        return value == null
                ? 0
                : parseInteger(value);
    }

    private int parseInteger(String value) {
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
                null,
                Collections.emptyList()
        );
    }

    private BigDecimal extractStandaloneAmount(String line) {
        Matcher matcher =
                Pattern.compile(
                                "^\\s*([$€£¥₩]\\s*)?"
                                        + "(-?\\d[\\d.,]*)"
                                        + "\\s*(?:[A-Z]{3})?\\s*$"
                        )
                        .matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        return parseAmount(
                (matcher.group(1) == null
                        ? ""
                        : matcher.group(1))
                        + matcher.group(2)
        );
    }
}