package com.tripass.asset.service.provider;

import com.tripass.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 설정값에 따라 Mock 또는 CODEF Provider를 선택합니다.
 */
@Component
public class CardTransactionProviderRouter {

    private final Map<String, CardTransactionProvider> providers;
    private final String configuredProviderType;

    public CardTransactionProviderRouter(
            List<CardTransactionProvider> providers,
            @Value("${card.transaction.provider:mock}") String configuredProviderType
    ) {
        this.providers = new HashMap<>();
        for (CardTransactionProvider provider : providers) {
            this.providers.put(provider.getProviderType().toLowerCase(Locale.ROOT), provider);
        }
        this.configuredProviderType = configuredProviderType.toLowerCase(Locale.ROOT);
    }

    public CardTransactionProvider getProvider() {
        CardTransactionProvider provider = providers.get(configuredProviderType);
        if (provider == null) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "CARD_PROVIDER_NOT_FOUND",
                    "카드 거래 Provider 설정을 확인해 주세요."
            );
        }
        return provider;
    }
}
