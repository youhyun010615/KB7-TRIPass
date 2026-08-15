package com.tripass.saving.classification;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MerchantNameNormalizer {

    private static final int[] NGRAM_SIZES = {1, 2, 3, 4};

    public String normalize(String merchantName) {
        if (merchantName == null) {
            return "";
        }

        String normalized = merchantName.toUpperCase(Locale.ROOT).trim();
        normalized = normalized.replaceAll("\\([^)]*\\)|\\[[^]]*]", " ");
        normalized = normalized.replaceAll("주식회사|유한회사|㈜|가맹점|본점|지점", " ");
        return normalized.replaceAll("[^0-9A-Z가-힣]+", "");
    }

    public List<String> createCharacterNgrams(String merchantName) {
        String normalized = normalize(merchantName);
        if (normalized.isEmpty()) {
            return List.of();
        }

        String bounded = "^" + normalized + "$";
        List<String> tokens = new ArrayList<>();
        for (int size : NGRAM_SIZES) {
            for (int index = 0; index <= bounded.length() - size; index++) {
                tokens.add(bounded.substring(index, index + size));
            }
        }
        return tokens;
    }
}
