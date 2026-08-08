package com.tripass.bank.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripass.bank.client.BankClient;
import com.tripass.bank.domain.ExchangeBankBranch;
import com.tripass.bank.dto.BankBranchDto;
import com.tripass.bank.dto.ExchangeEstimateDto;
import com.tripass.bank.exception.BankErrorCode;
import com.tripass.bank.exception.BankException;
import com.tripass.bank.mapper.BankMapper;
import com.tripass.exchange.dto.ExchangeMarketDataDto;
import com.tripass.exchange.mapper.MarketDataMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankService {

    private final BankClient bankClient;
    private final BankMapper bankMapper;
    private final MarketDataMapper marketDataMapper;

    @Transactional
    public int syncBankBranches() {
        // 1. 기존 데이터 초기화
        bankMapper.deleteAllBranches();

        // 2. 지역 목록 로드
        List<String> regions = loadRegions();
        log.info("Loaded {} regions for KB bank sync.", regions.size());

        Set<String> processedKeys = new HashSet<>();
        List<ExchangeBankBranch> batchList = new ArrayList<>();
        int totalProcessed = 0;

        // 3. 각 지역을 순회하며 API 호출
        for (String region : regions) {
            int page = 1;
            boolean isEnd = false;

            while (!isEnd) {
                // 키워드 검색 수행
                Map<String, Object> response = bankClient.fetchBankBranches(region + " 국민은행", page);

                if (response == null || !response.containsKey("documents")) {
                    break;
                }

                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                if (documents.isEmpty()) {
                    break;
                }

                for (Map<String, Object> doc : documents) {
                    String placeName = (String) doc.get("place_name");
                    
                    // ATM, 365코너, 자동화기기 등 영업점이 아닌 것은 제외
                    if (placeName.contains("ATM") || placeName.contains("365") || placeName.contains("자동화") ||
                        placeName.contains("무인") || placeName.contains("공단") || placeName.contains("출장소")){
                        continue;
                    }

                    String address = (String) doc.get("road_address_name");
                    if (address == null || address.isEmpty()) {
                        address = (String) doc.get("address_name");
                    }

                    // 중복 저장을 방지하기 위한 유니크 키 조합 (지점명 + 주소)
                    String uniqueKey = placeName + "_" + address;

                    if (!processedKeys.contains(uniqueKey)) {
                        processedKeys.add(uniqueKey);
                        batchList.add(convertToEntity(doc));
                    }
                }

                // 한 페이지당 한 번에 저장하지 않고 일정한 크기가 쌓이면 DB에 반영 (Bulk Insert 최적화)
                if (batchList.size() >= 100) {
                    bankMapper.insertBankBranches(batchList);
                    totalProcessed += batchList.size();
                    batchList.clear();
                }

                Map<String, Object> meta = (Map<String, Object>) response.get("meta");
                isEnd = (boolean) meta.get("is_end");
                page++;

                // API 호출 부하 조절 및 가독성을 위한 가벼운 슬립
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 남은 데이터 최종 저장
        if (!batchList.isEmpty()) {
            bankMapper.insertBankBranches(batchList);
            totalProcessed += batchList.size();
        }

        log.info("Successfully synchronized {} unique KB bank branches.", totalProcessed);
        return totalProcessed;
    }

    private List<String> loadRegions() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                    new ClassPathResource("regions.json").getInputStream(),
                    new TypeReference<>() {}
            );
        } catch (Exception e) {
            log.error("Failed to load regions.json", e);
            throw new BankException(BankErrorCode.SYNC_FAILED);
        }
    }

    private ExchangeBankBranch convertToEntity(Map<String, Object> doc) {
        String address = (String) doc.get("road_address_name");
        if (address == null || address.isEmpty()) {
            address = (String) doc.get("address_name");
        }

        return ExchangeBankBranch.builder()
                .bankName("KB국민은행") // 은행명 통일
                .branchName((String) doc.get("place_name"))
                .address(address)
                .businessHours("09:00-16:00")
                .latitude(new BigDecimal((String) doc.get("y")))
                .longitude(new BigDecimal((String) doc.get("x")))
                .phoneNumber((String) doc.get("phone"))
                .isActive(true)
                .build();
    }

    public List<BankBranchDto> findNearbyBanks(BigDecimal lat, BigDecimal lng, Double radius) {
        double searchRadius = (radius != null) ? radius : 2.0; // 기본 반경 2km
        return bankMapper.findNearbyBanks(lat, lng, searchRadius);
    }

    public BankBranchDto findById(Long id) {
        BankBranchDto bank = bankMapper.findById(id);
        if (bank == null) {
            throw new BankException(BankErrorCode.BANK_NOT_FOUND);
        }
        return bank;
    }

    public List<BankBranchDto> findAll(String keyword) {
        return bankMapper.findAllByKeyword(keyword);
    }

    public ExchangeEstimateDto getEstimate(BigDecimal amount, String currencyCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException(BankErrorCode.INVALID_CALCULATION_INPUT);
        }

        ExchangeMarketDataDto marketData = marketDataMapper.getLatestMarketDataByCurrencyCode(currencyCode);
        if (marketData == null) {
            throw new BankException(BankErrorCode.CURRENCY_NOT_FOUND);
        }

        // 환전 계산 로직
        // 예상금액 = 원화 / (살 때 환율 / 단위)
        BigDecimal buyRate = marketData.getBuyRate();
        BigDecimal unit = new BigDecimal(marketData.getUnit());
        
        if (buyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException(BankErrorCode.INVALID_CALCULATION_INPUT);
        }

        BigDecimal estimatedAmount = amount.divide(buyRate.divide(unit, 4, RoundingMode.HALF_UP), 2, RoundingMode.HALF_UP);

        return ExchangeEstimateDto.builder()
                .inputAmount(amount)
                .estimatedAmount(estimatedAmount)
                .buyRate(buyRate)
                .buyFeeRate(marketData.getBuyFeeRate())
                .sellRate(marketData.getSellRate())
                .sellFeeRate(marketData.getSellFeeRate())
                .baseRate(marketData.getBaseRate())
                .unit(marketData.getUnit())
                .currencyCode(currencyCode)
                .build();
    }
}
