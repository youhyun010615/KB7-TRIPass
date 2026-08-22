package com.tripass.ocr.client;

import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;
import com.tripass.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Google Cloud Translation API를 이용해 외국어 텍스트를 한국어로 번역한다.
@Component
public class GoogleTranslationClient {

    private static final String TARGET_LANGUAGE_CODE = "ko";
    private static final String LOCATION = "global";

    private static final Duration RPC_TIMEOUT =
            Duration.ofSeconds(10);

    private static final Duration TOTAL_TIMEOUT =
            Duration.ofSeconds(15);

    private final String projectId;

    public GoogleTranslationClient(
            @Value("${google.cloud.project-id:}")
            String projectId
    ) {
        this.projectId = projectId;
    }

    // 외국어 문자열 목록을 한국어로 일괄 번역한다.
    public List<String> translateToKorean(
            List<String> texts
    ) {
        validateConfiguration();

        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        validateTexts(texts);

        try (TranslationServiceClient client =
                     TranslationServiceClient.create(
                             createClientSettings()
                     )) {

            TranslateTextRequest request =
                    TranslateTextRequest.newBuilder()
                            .setParent(
                                    LocationName.of(
                                            projectId,
                                            LOCATION
                                    ).toString()
                            )
                            .addAllContents(texts)
                            .setMimeType("text/plain")
                            .setTargetLanguageCode(
                                    TARGET_LANGUAGE_CODE
                            )
                            .build();

            TranslateTextResponse response =
                    client.translateText(request);

            return response.getTranslationsList()
                    .stream()
                    .map(Translation::getTranslatedText)
                    .collect(Collectors.toList());

        } catch (IOException | ApiException exception) {
            throw new CustomException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "TRANSLATION_SERVICE_UNAVAILABLE",
                    "영수증 번역 서비스에 연결할 수 없습니다."
            );
        }
    }

    // Google Translation 클라이언트의 요청 제한시간을 설정한다.
    private TranslationServiceSettings createClientSettings()
            throws IOException {

        TranslationServiceSettings.Builder settingsBuilder =
                TranslationServiceSettings.newBuilder();

        RetrySettings retrySettings =
                settingsBuilder
                        .translateTextSettings()
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
                .translateTextSettings()
                .setRetrySettings(retrySettings);

        return settingsBuilder.build();
    }

    // Google Cloud 프로젝트 설정 여부를 검사한다.
    private void validateConfiguration() {
        if (projectId == null || projectId.isBlank()) {
            throw new CustomException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "TRANSLATION_NOT_CONFIGURED",
                    "영수증 번역 서비스 설정이 완료되지 않았습니다."
            );
        }
    }

    // 번역 요청에 빈 문자열이 포함되지 않도록 검사한다.
    private void validateTexts(List<String> texts) {
        boolean containsBlankText =
                texts.stream()
                        .anyMatch(text ->
                                text == null
                                        || text.isBlank()
                        );

        if (containsBlankText) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "TRANSLATION_TEXT_REQUIRED",
                    "번역할 문자열을 입력해 주세요."
            );
        }
    }
}