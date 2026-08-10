package com.tripass.financial.service;

import com.tripass.common.exception.CustomException;
import com.tripass.financial.dto.TravelCardDetailResponseDto;
import com.tripass.financial.dto.TravelCardListResponseDto;
import com.tripass.financial.mapper.TravelCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

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
}