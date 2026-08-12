package com.tripass.ocr.service;

import com.tripass.common.exception.CustomException;
import com.tripass.ocr.client.GoogleTranslationClient;
import com.tripass.ocr.client.VisionOcrClient;
import com.tripass.ocr.dto.internal.ParsedReceiptData;
import com.tripass.ocr.dto.internal.ParsedReceiptItem;
import com.tripass.ocr.dto.internal.ValidatedReceiptImage;
import com.tripass.ocr.dto.internal.VisionOcrResult;
import com.tripass.ocr.dto.response.ReceiptAnalyzeItemResponse;
import com.tripass.ocr.dto.response.ReceiptAnalyzeResponse;
import com.tripass.ocr.service.parser.ReceiptTextParser;
import com.tripass.ocr.service.validation.ReceiptImageValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// 해외 영수증 OCR 분석 기능 구현체
@Service
public class ReceiptAnalysisServiceImpl
        implements ReceiptAnalysisService {

    private final VisionOcrClient visionOcrClient;
    private final GoogleTranslationClient translationClient;
    private final ReceiptTextParser receiptTextParser;
    private final ReceiptImageValidator receiptImageValidator;

    public ReceiptAnalysisServiceImpl(
            VisionOcrClient visionOcrClient,
            GoogleTranslationClient translationClient,
            ReceiptTextParser receiptTextParser,
            ReceiptImageValidator receiptImageValidator
    ) {
        this.visionOcrClient = visionOcrClient;
        this.translationClient = translationClient;
        this.receiptTextParser = receiptTextParser;
        this.receiptImageValidator = receiptImageValidator;
    }

    @Override
    public ReceiptAnalyzeResponse analyzeReceipt(
            Long userId,
            MultipartFile receiptImage
    ) {
        if (userId == null) {
            throw new CustomException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED",
                    "로그인이 필요합니다."
            );
        }

        // 이미지 형식과 크기를 검사하고 파일 내용을 읽는다.
        ValidatedReceiptImage validatedImage =
                receiptImageValidator.validateAndRead(
                        receiptImage
                );

        VisionOcrResult ocrResult =
                visionOcrClient.analyze(
                        validatedImage.getImageBytes()
                );

        ParsedReceiptData parsedReceipt =
                receiptTextParser.parse(ocrResult);

        TranslationResult translationResult =
                translateNames(
                        parsedReceipt,
                        ocrResult.getDetectedLanguageCode()
                );

        List<ReceiptAnalyzeItemResponse> items =
                createItemResponses(
                        parsedReceipt.getItems(),
                        translationResult.getTranslatedItemNames()
                );

        BigDecimal splitAmount =
                parsedReceipt.getTotalAmount();

        return new ReceiptAnalyzeResponse(
                ocrResult.getDetectedLanguageCode(),
                parsedReceipt.getOriginalMerchantName(),
                translationResult.getTranslatedMerchantName(),
                parsedReceipt.getPaymentDateTime(),
                parsedReceipt.getCurrencyCode(),
                parsedReceipt.getTotalAmount(),
                parsedReceipt.getTaxAmount(),
                1,
                splitAmount,
                items,
                ocrResult.getRawText()
        );
    }

    // 상호명과 품목명을 한국어로 일괄 번역한다.
    private TranslationResult translateNames(
            ParsedReceiptData parsedReceipt,
            String detectedLanguageCode
    ) {
        String merchantName =
                parsedReceipt.getOriginalMerchantName();

        List<ParsedReceiptItem> parsedItems =
                parsedReceipt.getItems() == null
                        ? Collections.emptyList()
                        : parsedReceipt.getItems();

        // 원문이 한국어라면 외부 번역 API를 호출하지 않는다.
        if (isKorean(detectedLanguageCode)) {
            return new TranslationResult(
                    merchantName,
                    parsedItems.stream()
                            .map(
                                    ParsedReceiptItem
                                            ::getOriginalName
                            )
                            .toList()
            );
        }

        List<String> textsToTranslate =
                new ArrayList<>();

        boolean hasMerchantName =
                merchantName != null
                        && !merchantName.isBlank();

        if (hasMerchantName) {
            textsToTranslate.add(merchantName);
        }

        parsedItems.stream()
                .map(ParsedReceiptItem::getOriginalName)
                .filter(name ->
                        name != null
                                && !name.isBlank()
                )
                .forEach(textsToTranslate::add);

        if (textsToTranslate.isEmpty()) {
            return new TranslationResult(
                    null,
                    Collections.emptyList()
            );
        }

        List<String> translatedTexts =
                translationClient.translateToKorean(
                        textsToTranslate
                );

        int expectedSize =
                textsToTranslate.size();

        if (translatedTexts.size() != expectedSize) {
            throw new CustomException(
                    HttpStatus.BAD_GATEWAY,
                    "TRANSLATION_RESULT_INVALID",
                    "영수증 번역 결과가 올바르지 않습니다."
            );
        }

        int itemStartIndex =
                hasMerchantName ? 1 : 0;

        String translatedMerchantName =
                hasMerchantName
                        ? translatedTexts.get(0)
                        : null;

        List<String> translatedItemNames =
                new ArrayList<>(
                        translatedTexts.subList(
                                itemStartIndex,
                                translatedTexts.size()
                        )
                );

        return new TranslationResult(
                translatedMerchantName,
                translatedItemNames
        );
    }

    // 파싱된 품목과 번역된 품목명을 응답 DTO로 변환한다.
    private List<ReceiptAnalyzeItemResponse>
    createItemResponses(
            List<ParsedReceiptItem> parsedItems,
            List<String> translatedItemNames
    ) {
        if (parsedItems == null
                || parsedItems.isEmpty()) {

            return Collections.emptyList();
        }

        if (parsedItems.size()
                != translatedItemNames.size()) {

            throw new CustomException(
                    HttpStatus.BAD_GATEWAY,
                    "TRANSLATION_RESULT_INVALID",
                    "영수증 품목 번역 결과가 올바르지 않습니다."
            );
        }

        List<ReceiptAnalyzeItemResponse> responses =
                new ArrayList<>();

        for (int index = 0;
             index < parsedItems.size();
             index++) {

            ParsedReceiptItem parsedItem =
                    parsedItems.get(index);

            responses.add(
                    new ReceiptAnalyzeItemResponse(
                            parsedItem.getOriginalName(),
                            translatedItemNames.get(index),
                            parsedItem.getQuantity(),
                            parsedItem.getAmount(),
                            parsedItem.getDisplayOrder()
                    )
            );
        }

        return responses;
    }

    private boolean isKorean(
            String languageCode
    ) {
        return languageCode != null
                && languageCode
                .toLowerCase(Locale.ROOT)
                .startsWith("ko");
    }

    // 번역된 상호명과 품목명을 함께 전달하는 내부 처리 결과
    private static class TranslationResult {

        private final String translatedMerchantName;
        private final List<String> translatedItemNames;

        private TranslationResult(
                String translatedMerchantName,
                List<String> translatedItemNames
        ) {
            this.translatedMerchantName =
                    translatedMerchantName;

            this.translatedItemNames =
                    translatedItemNames;
        }

        private String getTranslatedMerchantName() {
            return translatedMerchantName;
        }

        private List<String> getTranslatedItemNames() {
            return translatedItemNames;
        }
    }
}