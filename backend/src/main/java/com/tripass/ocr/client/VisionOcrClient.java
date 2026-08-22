package com.tripass.ocr.client;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.ByteString;
import com.tripass.common.exception.CustomException;
import com.tripass.ocr.dto.internal.OcrTextBlock;
import com.tripass.ocr.dto.internal.VisionOcrResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Duration;

//Google Cloud Vision API를 호출하여 영수증 원문을 인식한다.
@Component
public class VisionOcrClient {
    private static final Duration RPC_TIMEOUT =
            Duration.ofSeconds(15);

    private static final Duration TOTAL_TIMEOUT =
            Duration.ofSeconds(20);

    //이미지 바이트를 OCR 처리
    public VisionOcrResult analyze(byte[] imageBytes) {
        validateImageBytes(imageBytes);

        try (ImageAnnotatorClient client =
                     ImageAnnotatorClient.create(
                             createClientSettings()
                     )) {

            AnnotateImageRequest request =
                    createRequest(imageBytes);

            BatchAnnotateImagesResponse batchResponse =
                    client.batchAnnotateImages(
                            Collections.singletonList(request)
                    );

            AnnotateImageResponse response =
                    batchResponse.getResponses(0);

            validateResponse(response);

            return convertResult(response);

        } catch (IOException | ApiException exception) {
            throw new CustomException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OCR_SERVICE_UNAVAILABLE",
                    "영수증 인식 서비스에 연결할 수 없습니다."
            );
        }
    }
    // Google Vision 클라이언트의 요청 제한시간을 설정한다.
    private ImageAnnotatorSettings createClientSettings()
            throws IOException {

        ImageAnnotatorSettings.Builder settingsBuilder =
                ImageAnnotatorSettings.newBuilder();

        RetrySettings retrySettings =
                settingsBuilder
                        .batchAnnotateImagesSettings()
                        .getRetrySettings()
                        .toBuilder()
                        .setInitialRpcTimeoutDuration(
                                RPC_TIMEOUT
                        )
                        .setRpcTimeoutMultiplier(1.0)
                        .setMaxRpcTimeoutDuration(
                                RPC_TIMEOUT
                        )
                        .setTotalTimeoutDuration(
                                TOTAL_TIMEOUT
                        )
                        .build();

        settingsBuilder
                .batchAnnotateImagesSettings()
                .setRetrySettings(retrySettings);

        return settingsBuilder.build();
    }

    //Document_Text_DETECTION 요청을 생성
    private AnnotateImageRequest createRequest(
            byte[] imageBytes
    ) {
        Image image = Image.newBuilder()
                .setContent(ByteString.copyFrom(imageBytes))
                .build();

        Feature feature = Feature.newBuilder()
                .setType(
                        Feature.Type.DOCUMENT_TEXT_DETECTION
                )
                .build();

        return AnnotateImageRequest.newBuilder()
                .setImage(image)
                .addFeatures(feature)
                .build();
    }

    //Google Vision 응답 오류를 확인한다.
    private void validateResponse(
            AnnotateImageResponse response
    ) {
        if (response.hasError()) {
            throw new CustomException(
                    HttpStatus.BAD_GATEWAY,
                    "OCR_PROCESSING_FAILED",
                    "영수증 이미지를 인식하지 못했습니다."
            );
        }
    }

    //Vision 응답을 내부 OCR 결과로 변환한다.
    private VisionOcrResult convertResult(
            AnnotateImageResponse response
    ) {
        TextAnnotation fullTextAnnotation =
                response.getFullTextAnnotation();

        if (fullTextAnnotation == null
                || fullTextAnnotation.getText().isBlank()) {

            throw new CustomException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "OCR_TEXT_NOT_FOUND",
                    "영수증 이미지에서 문자를 찾지 못했습니다."
            );
        }

        String detectedLanguageCode =
                findDetectedLanguageCode(
                        fullTextAnnotation
                );

        List<OcrTextBlock> textBlocks =
                extractTextBlocks(response);

        return new VisionOcrResult(
                fullTextAnnotation.getText(),
                detectedLanguageCode,
                textBlocks
        );
    }

    //Vision이 감지한 대표 언어 코드를 조회한다.
    private String findDetectedLanguageCode(
            TextAnnotation textAnnotation
    ) {
        for (Page page : textAnnotation.getPagesList()) {
            if (page.hasProperty()
                    && page.getProperty()
                    .getDetectedLanguagesCount() > 0) {

                return page.getProperty()
                        .getDetectedLanguages(0)
                        .getLanguageCode();
            }
        }

        return null;
    }

    //OCR 텍스트와 위치 정보를 추출한다.
    private List<OcrTextBlock> extractTextBlocks(
            AnnotateImageResponse response
    ) {
        List<EntityAnnotation> annotations =
                response.getTextAnnotationsList();

        if (annotations.size() <= 1) {
            return Collections.emptyList();
        }

        List<OcrTextBlock> textBlocks =
                new ArrayList<>();

        // 첫 번째 항목은 전체 원문이므로 제외한다.
        for (int index = 1;
             index < annotations.size();
             index++) {

            EntityAnnotation annotation =
                    annotations.get(index);

            List<OcrTextBlock.Vertex> vertices =
                    annotation.getBoundingPoly()
                            .getVerticesList()
                            .stream()
                            .map(vertex ->
                                    new OcrTextBlock.Vertex(
                                            vertex.getX(),
                                            vertex.getY()
                                    )
                            )
                            .toList();

            textBlocks.add(
                    new OcrTextBlock(
                            annotation.getDescription(),
                            vertices
                    )
            );
        }

        return textBlocks;
    }

    /**
     * 빈 이미지 요청을 차단한다.
     */
    private void validateImageBytes(byte[] imageBytes) {
        if (imageBytes == null
                || imageBytes.length == 0) {

            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "OCR_IMAGE_REQUIRED",
                    "영수증 이미지를 첨부해 주세요."
            );
        }
    }
}
