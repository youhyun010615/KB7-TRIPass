package com.tripass.saving.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class MerchantNameClassifier {

    private static final String MODEL_PATH = "model/merchant-category-v1.json";

    private final MerchantNameNormalizer normalizer;
    private final MerchantCategoryModel model;

    public MerchantNameClassifier(MerchantNameNormalizer normalizer) {
        this.normalizer = normalizer;
        this.model = loadModel(new ObjectMapper());
        validateModel(model);
    }

    public CategoryClassificationResult classify(String merchantName) {
        List<String> tokens = normalizer.createCharacterNgrams(merchantName);
        if (tokens.isEmpty()) {
            return CategoryClassificationResult.fallback(new BigDecimal("0.0000"));
        }

        Map<ConsumptionCategoryCode, Double> probabilities =
                convertToProbabilities(calculateLogScores(tokens));
        Map.Entry<ConsumptionCategoryCode, Double> prediction = probabilities.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow(() -> new IllegalStateException("카테고리 예측 결과가 없습니다."));

        BigDecimal confidence = BigDecimal.valueOf(prediction.getValue())
                .setScale(4, RoundingMode.HALF_UP);
        if (prediction.getValue() < model.getMinConfidence()) {
            return CategoryClassificationResult.fallback(confidence);
        }

        return CategoryClassificationResult.fromAiModel(prediction.getKey(), confidence);
    }

    private Map<ConsumptionCategoryCode, Double> calculateLogScores(List<String> tokens) {
        Map<ConsumptionCategoryCode, Double> scores =
                new EnumMap<>(ConsumptionCategoryCode.class);
        long totalDocuments = model.getClassDocs().values().stream()
                .mapToLong(Long::longValue)
                .sum();
        int categoryCount = ConsumptionCategoryCode.values().length;
        int vocabularySize = Math.max(1, model.getVocabulary().size());

        for (ConsumptionCategoryCode category : ConsumptionCategoryCode.values()) {
            String categoryName = category.getDisplayName();
            long classDocumentCount = model.getClassDocs().getOrDefault(categoryName, 0L);
            double prior = (classDocumentCount + model.getAlpha())
                    / (totalDocuments + model.getAlpha() * categoryCount);
            double score = Math.log(prior);
            long categoryTokenTotal = model.getTotalTokens().getOrDefault(categoryName, 0L);
            double denominator = categoryTokenTotal + model.getAlpha() * vocabularySize;
            Map<String, Long> categoryTokenCounts = model.getTokenCounts()
                    .getOrDefault(categoryName, Map.of());

            for (String token : tokens) {
                long tokenCount = categoryTokenCounts.getOrDefault(token, 0L);
                score += Math.log((tokenCount + model.getAlpha()) / denominator);
            }
            scores.put(category, score);
        }
        return scores;
    }

    private Map<ConsumptionCategoryCode, Double> convertToProbabilities(
            Map<ConsumptionCategoryCode, Double> logScores
    ) {
        double peak = logScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElseThrow();
        Map<ConsumptionCategoryCode, Double> exponentials =
                new EnumMap<>(ConsumptionCategoryCode.class);
        double total = 0.0;
        for (Map.Entry<ConsumptionCategoryCode, Double> entry : logScores.entrySet()) {
            double exponential = Math.exp(entry.getValue() - peak);
            exponentials.put(entry.getKey(), exponential);
            total += exponential;
        }

        Map<ConsumptionCategoryCode, Double> probabilities =
                new EnumMap<>(ConsumptionCategoryCode.class);
        for (Map.Entry<ConsumptionCategoryCode, Double> entry : exponentials.entrySet()) {
            probabilities.put(entry.getKey(), entry.getValue() / total);
        }
        return probabilities;
    }

    private MerchantCategoryModel loadModel(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(MODEL_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, MerchantCategoryModel.class);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "가맹점 카테고리 모델을 읽을 수 없습니다: " + MODEL_PATH,
                    exception
            );
        }
    }

    private void validateModel(MerchantCategoryModel target) {
        if (target == null
                || target.getClassDocs() == null
                || target.getTokenCounts() == null
                || target.getTotalTokens() == null
                || target.getVocabulary() == null
                || target.getVocabulary().isEmpty()) {
            throw new IllegalStateException("가맹점 카테고리 모델 데이터가 올바르지 않습니다.");
        }
        if (target.getAlpha() <= 0) {
            throw new IllegalStateException("가맹점 카테고리 모델 alpha는 0보다 커야 합니다.");
        }
        if (target.getMinConfidence() < 0 || target.getMinConfidence() > 1) {
            throw new IllegalStateException("모델 신뢰도 기준값은 0~1이어야 합니다.");
        }
    }
}
