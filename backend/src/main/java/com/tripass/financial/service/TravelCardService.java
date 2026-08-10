package com.tripass.financial.service;

import com.tripass.common.exception.CustomException;
import com.tripass.financial.dto.TravelCardDetailResponseDto;
import com.tripass.financial.dto.TravelCardListResponseDto;
import com.tripass.financial.mapper.TravelCardMapper;
import com.tripass.financial.dto.TravelCardComparisonResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelCardService {

    private final TravelCardMapper travelCardMapper;

    public List<TravelCardListResponseDto> getTravelCards(
            String keyword,
            String currencyCode,
            Boolean instantUse,
            Boolean transitCard
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedCurrencyCode = normalizeCurrencyCode(currencyCode);

        return travelCardMapper.findAll(
                normalizedKeyword,
                normalizedCurrencyCode,
                instantUse,
                transitCard
        );
    }

    public TravelCardDetailResponseDto getTravelCard(Long cardId) {
        validateCardId(cardId);

        TravelCardDetailResponseDto travelCard =
                travelCardMapper.findById(cardId);

        if (travelCard == null) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "GDS_TRAVEL_CARD_NOT_FOUND",
                    "해당 트래블카드를 찾을 수 없습니다."
            );
        }

        List<String> supportedCurrencies =
                travelCardMapper.findCurrencyCodesByCardId(cardId);

        travelCard.setSupportedCurrencies(supportedCurrencies);

        return travelCard;
    }

    public List<TravelCardComparisonResponseDto> getTravelCardComparison(
            List<Long> cardIds
    ) {
        validateComparisonCardIds(cardIds);

        List<TravelCardComparisonResponseDto> comparisonCards =
                travelCardMapper.findAllByIds(cardIds);

        if (comparisonCards.size() != cardIds.size()) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "GDS_TRAVEL_CARD_NOT_FOUND",
                    "비교 대상에 존재하지 않거나 비활성화된 트래블카드가 포함되어 있습니다."
            );
        }

        return comparisonCards;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        return keyword.trim();
    }

    private String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return null;
        }

        String normalizedCode =
                currencyCode.trim().toUpperCase(Locale.ROOT);

        if (!normalizedCode.matches("^[A-Z]{3}$")) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "GDS_INVALID_CURRENCY_CODE",
                    "통화 코드는 ISO 4217 형식의 영문 3자리여야 합니다."
            );
        }

        return normalizedCode;
    }


    private void validateCardId(Long cardId) {
        if (cardId == null || cardId <= 0) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "GDS_INVALID_CARD_ID",
                    "카드 ID는 1 이상의 값이어야 합니다."
            );
        }
    }


    private void validateComparisonCardIds(List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty() || cardIds.size() > 3) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "GDS_COMPARISON_CARD_COUNT_INVALID",
                    "비교할 트래블카드는 1개 이상 3개 이하로 선택해야 합니다."
            );
        }

        boolean hasInvalidCardId = cardIds.stream()
                .anyMatch(cardId -> cardId == null || cardId <= 0);

        if (hasInvalidCardId) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "GDS_INVALID_CARD_ID",
                    "카드 ID는 1 이상의 값이어야 합니다."
            );
        }

        if (new HashSet<>(cardIds).size() != cardIds.size()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "GDS_DUPLICATED_CARD_ID",
                    "동일한 트래블카드를 중복하여 비교할 수 없습니다."
            );
        }
    }
}