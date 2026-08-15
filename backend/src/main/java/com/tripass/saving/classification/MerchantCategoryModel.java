package com.tripass.saving.classification;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class MerchantCategoryModel {

    private double alpha;
    private double minConfidence;
    private Map<String, Long> classDocs;
    private Map<String, Map<String, Long>> tokenCounts;
    private Map<String, Long> totalTokens;
    private List<String> vocabulary;
}
