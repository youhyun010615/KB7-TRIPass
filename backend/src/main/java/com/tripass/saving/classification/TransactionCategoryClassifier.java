package com.tripass.saving.classification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자 지정, CODEF 업종, 가맹점명 AI 모델을 하나의 우선순위로 결합한다.
 */
@Component
@RequiredArgsConstructor
public class TransactionCategoryClassifier {

    private final CodefMerchantTypeClassifier codefMerchantTypeClassifier;
    private final MerchantNameClassifier merchantNameClassifier;

    public CategoryClassificationResult classify(
            String merchantName,
            String merchantType
    ) {
        return classify(merchantName, merchantType, null);
    }

    public CategoryClassificationResult classify(
            String merchantName,
            String merchantType,
            ConsumptionCategoryCode userCategory
    ) {
        if (userCategory != null) {
            return CategoryClassificationResult.fromUser(userCategory);
        }

        return codefMerchantTypeClassifier.classify(merchantType)
                .orElseGet(() -> merchantNameClassifier.classify(merchantName));
    }
}
