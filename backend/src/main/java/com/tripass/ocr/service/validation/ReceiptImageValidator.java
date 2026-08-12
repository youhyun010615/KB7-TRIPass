package com.tripass.ocr.service.validation;

import com.tripass.common.exception.CustomException;
import com.tripass.ocr.dto.internal.ValidatedReceiptImage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

// OCR 분석과 영수증 저장에 공통으로 사용하는 이미지 검증기
@Component
public class ReceiptImageValidator {

    private static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    public ValidatedReceiptImage validateAndRead(
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

        String originalFileName =
                StringUtils.cleanPath(
                        receiptImage.getOriginalFilename() == null
                                ? ""
                                : receiptImage.getOriginalFilename()
                );

        String requestedExtension =
                extractExtension(originalFileName);

        if (!isSupportedExtension(requestedExtension)) {
            throw new CustomException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "OCR_IMAGE_TYPE_NOT_SUPPORTED",
                    "JPG, JPEG, PNG 형식의 이미지만 업로드할 수 있습니다."
            );
        }

        try {
            byte[] imageBytes =
                    receiptImage.getBytes();

            String actualExtension =
                    detectImageExtension(imageBytes);

            if (actualExtension == null) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "OCR_INVALID_IMAGE",
                        "올바른 이미지 파일이 아닙니다."
                );
            }

            return new ValidatedReceiptImage(
                    originalFileName,
                    actualExtension,
                    imageBytes
            );

        } catch (IOException exception) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "OCR_IMAGE_READ_FAILED",
                    "영수증 이미지 파일을 읽지 못했습니다."
            );
        }
    }

    private String extractExtension(String fileName) {
        int extensionIndex =
                fileName.lastIndexOf('.');

        if (extensionIndex < 0
                || extensionIndex
                == fileName.length() - 1) {

            return "";
        }

        return fileName
                .substring(extensionIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private boolean isSupportedExtension(
            String extension
    ) {
        return extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("png");
    }

    private String detectImageExtension(byte[] bytes) {
        if (isJpeg(bytes)) {
            return "jpg";
        }

        if (isPng(bytes)) {
            return "png";
        }

        return null;
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
}