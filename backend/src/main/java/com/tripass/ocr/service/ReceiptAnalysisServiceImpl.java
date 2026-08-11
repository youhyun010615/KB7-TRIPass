package com.tripass.ocr.service;

import com.tripass.common.exception.CustomException;
import com.tripass.ocr.client.GoogleTranslationClient;
import com.tripass.ocr.client.VisionOcrClient;
import com.tripass.ocr.dto.internal.ParsedReceiptData;
import com.tripass.ocr.dto.internal.ParsedReceiptItem;
import com.tripass.ocr.dto.internal.VisionOcrResult;
import com.tripass.ocr.dto.response.ReceiptAnalyzeItemResponse;
import com.tripass.ocr.dto.response.ReceiptAnalyzeResponse;
import com.tripass.ocr.service.parser.ReceiptTextParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

//해외 영수증 OCR 분석 기능 구현체
@Service
public class ReceiptAnalysisServiceImpl
        implements ReceiptAnalysisService {

    private static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    private final VisionOcrClient visionOcrClient;
    private final GoogleTranslationClient translationClient;
    private final ReceiptTextParser receiptTextParser;

    public ReceiptAnalysisServiceImpl(
            VisionOcrClient visionOcrClient,
            GoogleTranslationClient translationClient,
            ReceiptTextParser receiptTextParser
    ) {
        this.visionOcrClient = visionOcrClient;
        this.translationClient = translationClient;
        this.receiptTextParser = receiptTextParser;
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

        byte[] imageBytes =
                validateAndReadImage(receiptImage);

        VisionOcrResult ocrResult =
                visionOcrClient.analyze(imageBytes);

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
                        translationResult
                                .getTranslatedItemNames()
                );

        BigDecimal splitAmount =
                parsedReceipt.getTotalAmount();

        return new ReceiptAnalyzeResponse(
                ocrResult.getDetectedLanguageCode(),
                parsedReceipt.getOriginalMerchantName(),
                translationResult
                        .getTranslatedMerchantName(),
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

    /**
     * 업로드 파일을 검증하고 바이트 배열로 변환한다.
     */
    private byte[] validateAndReadImage(
            MultipartFile receiptImage
    ) {
        if (receiptImage == null
                || receiptImage.isEmpty()) {

            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "OCR_IMAGE_REQUIRED",
                    "영수증 이미지를 첨부해 주세요."
            );
        }

        if (receiptImage.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "OCR_IMAGE_TOO_LARGE",
                    "영수증 이미지는 10MB 이하만 업로드할 수 있습니다."
            );
        }

        String fileName =
                receiptImage.getOriginalFilename();

        if (!hasSupportedExtension(fileName)) {
            throw new CustomException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "OCR_IMAGE_TYPE_NOT_SUPPORTED",
                    "JPG, JPEG, PNG 형식의 이미지만 업로드할 수 있습니다."
            );
        }

        try {
            byte[] imageBytes =
                    receiptImage.getBytes();

            if (!hasSupportedImageSignature(imageBytes)) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "OCR_INVALID_IMAGE",
                        "올바른 이미지 파일이 아닙니다."
                );
            }

            return imageBytes;

        } catch (IOException exception) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "OCR_IMAGE_READ_FAILED",
                    "영수증 이미지 파일을 읽지 못했습니다."
            );
        }
    }

    /**
     * 지원하는 파일 확장자인지 확인한다.
     */
    private boolean hasSupportedExtension(
            String fileName
    ) {
        if (fileName == null
                || !fileName.contains(".")) {
            return false;
        }

        String extension =
                fileName.substring(
                        fileName.lastIndexOf('.') + 1
                ).toLowerCase(Locale.ROOT);

        return extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("png");
    }

    /**
     * 확장자만 위조한 파일이 아닌지 실제 파일 시그니처로 확인한다.
     */
    private boolean hasSupportedImageSignature(
            byte[] imageBytes
    ) {
        return isJpeg(imageBytes)
                || isPng(imageBytes);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        int[] pngSignature = {
                0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        if (bytes.length < pngSignature.length) {
            return false;
        }

        for (int index = 0;
             index < pngSignature.length;
             index++) {

            if ((bytes[index] & 0xFF)
                    != pngSignature[index]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 상호명과 품목명을 한국어로 일괄 번역한다.
     */
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

    /**
     * 번역된 상호명과 품목명을 내부에서 함께 전달한다.
     */
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