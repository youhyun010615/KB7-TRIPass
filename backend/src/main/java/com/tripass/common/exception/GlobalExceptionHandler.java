package com.tripass.common.exception;

import com.tripass.common.response.ApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.validation.ConstraintViolationException;

/**
 * 전역 예외 핸들러 — 모든 컨트롤러에서 발생하는 예외를 ApiResponse 형태로 통일
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * 도메인별 커스텀 예외를 처리합니다.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 잘못된 인자값으로 발생한 예외를 처리합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_INPUT", e.getMessage()));
    }

    /**
     * 처리되지 않은 모든 서버 예외를 기록하고 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {

        log.error("처리되지 않은 서버 예외 발생", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }

    /**
     * 경로 변수와 요청 파라미터의 검증 실패를 처리합니다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("잘못된 요청값입니다.");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("INVALID_INPUT", message));
    }

    /**
     * 요청 DTO 필드의 검증 실패를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("잘못된 요청값입니다.");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("INVALID_INPUT", message));
    }

    /**
     * 잘못된 JSON 형식 또는 Enum 요청값을 처리합니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("INVALID_INPUT", "요청값의 형식이 올바르지 않습니다."));
    }

    /**
     * 최대 업로드 크기를 초과한 요청을 처리한다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(
                        ApiResponse.error(
                                "OCR_IMAGE_TOO_LARGE",
                                "영수증 이미지는 10MB 이하만 업로드할 수 있습니다."
                        )
                );
    }

    /**
     * multipart 요청에서 필수 파일이 누락된 경우를 처리한다.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleMissingServletRequestPart(
            MissingServletRequestPartException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.error(
                                "OCR_IMAGE_REQUIRED",
                                "영수증 이미지를 첨부해 주세요."
                        )
                );
    }

    /**
     * 잘못된 multipart 요청 형식을 처리한다.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleMultipartException(
            MultipartException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.error(
                                "OCR_MULTIPART_INVALID",
                                "파일 업로드 요청 형식이 올바르지 않습니다."
                        )
                );
    }
}