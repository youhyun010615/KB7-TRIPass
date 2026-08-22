package com.tripass.ocr.service.storage;

import com.tripass.common.exception.CustomException;
import com.tripass.ocr.dto.internal.StoredReceiptFile;
import com.tripass.ocr.dto.internal.ValidatedReceiptImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

// 서버의 로컬 디렉터리에 영수증 이미지를 저장한다.
@Component
public class LocalReceiptFileStorage
        implements ReceiptFileStorage {

    private static final String RECEIPT_DIRECTORY =
            "receipts";

    private final Path uploadRootPath;

    public LocalReceiptFileStorage(
            @Value("${upload.path}") String uploadPath
    ) {
        if (uploadPath == null || uploadPath.isBlank()) {
            throw new IllegalStateException(
                    "파일 업로드 경로를 설정해 주세요."
            );
        }

        this.uploadRootPath =
                Paths.get(uploadPath)
                        .toAbsolutePath()
                        .normalize();
    }

    @Override
    public StoredReceiptFile store(
            ValidatedReceiptImage receiptImage
    ) {
        if (receiptImage == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "OCR_IMAGE_REQUIRED",
                    "저장할 영수증 이미지를 입력해 주세요."
            );
        }

        LocalDate today = LocalDate.now();

        String relativeDirectory =
                RECEIPT_DIRECTORY
                        + "/"
                        + today.getYear()
                        + "/"
                        + String.format(
                        "%02d",
                        today.getMonthValue()
                )
                        + "/"
                        + String.format(
                        "%02d",
                        today.getDayOfMonth()
                );

        String storedFileName =
                UUID.randomUUID()
                        + "."
                        + receiptImage.getFileExtension();

        String storedPath =
                relativeDirectory
                        + "/"
                        + storedFileName;

        Path targetPath =
                resolveStoredPath(storedPath);

        try {
            Files.createDirectories(
                    targetPath.getParent()
            );

            Files.write(
                    targetPath,
                    receiptImage.getImageBytes(),
                    StandardOpenOption.CREATE_NEW
            );

            return new StoredReceiptFile(
                    receiptImage.getOriginalFileName(),
                    storedPath,
                    receiptImage
                            .getFileExtension()
                            .toUpperCase()
            );

        } catch (IOException exception) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RECEIPT_FILE_SAVE_FAILED",
                    "영수증 이미지 저장에 실패했습니다."
            );
        }
    }

    @Override
    public byte[] load(String storedPath) {
        Path targetPath =
                resolveStoredPath(storedPath);

        if (!Files.exists(targetPath)
                || !Files.isRegularFile(targetPath)) {

            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "RECEIPT_FILE_NOT_FOUND",
                    "영수증 이미지 파일을 찾을 수 없습니다."
            );
        }

        try {
            return Files.readAllBytes(targetPath);

        } catch (IOException exception) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RECEIPT_FILE_READ_FAILED",
                    "영수증 이미지 파일을 읽지 못했습니다."
            );
        }
    }

    @Override
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        Path targetPath =
                resolveStoredPath(storedPath);

        try {
            Files.deleteIfExists(targetPath);

        } catch (IOException exception) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RECEIPT_FILE_DELETE_FAILED",
                    "영수증 이미지 파일 삭제에 실패했습니다."
            );
        }
    }

    // 사용자 입력 경로로 상위 디렉터리에 접근하는 것을 차단한다.
    private Path resolveStoredPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_FILE_PATH_REQUIRED",
                    "영수증 이미지 경로가 필요합니다."
            );
        }

        Path resolvedPath =
                uploadRootPath
                        .resolve(storedPath)
                        .normalize();

        if (!resolvedPath.startsWith(uploadRootPath)) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_FILE_PATH_INVALID",
                    "올바르지 않은 영수증 이미지 경로입니다."
            );
        }

        return resolvedPath;
    }
}