package com.tripass.bank.service;

import com.tripass.bank.client.BankClient;
import com.tripass.bank.domain.ExchangeBankBranch;
import com.tripass.bank.mapper.BankMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankService {

    private final BankClient bankClient;
    private final BankMapper bankMapper;

    @Transactional
    public int syncBankBranches() {
        // 1. 기존 데이터 초기화
        bankMapper.deleteAllBranches();

        // 2. 대한민국 전역 격자 좌표 생성 (간격 0.15도, 약 15km)
        List<Coordinate> grid = generateGridCoordinates();
        log.info("Generated {} grid coordinates for nationwide coverage.", grid.size());

        Set<String> processedKeys = new HashSet<>();
        List<ExchangeBankBranch> batchList = new ArrayList<>();
        int totalProcessed = 0;

        // 3. 각 격자점을 순회하며 API 호출
        for (Coordinate coord : grid) {
            int page = 1;
            boolean isEnd = false;

            while (!isEnd) {
                // 반경 20km(20000m) 검색으로 사각지대 완벽 방어
                Map<String, Object> response = bankClient.fetchBankBranchesByLocation(
                        coord.getLat(), coord.getLon(), 20000, page
                );

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

    private List<Coordinate> generateGridCoordinates() {
        List<Coordinate> coordinates = new ArrayList<>();
        // 대한민국 경계 범위 (위도 33.1 ~ 38.5, 경도 124.6 ~ 129.6)
        // 남북 5.4도, 동서 5.0도를 0.15도 단위로 순회하여 겹치는 원들로 전역 격자를 생성
        for (double lat = 33.1; lat <= 38.5; lat += 0.15) {
            for (double lon = 124.6; lon <= 129.6; lon += 0.15) {
                coordinates.add(new Coordinate(lat, lon));
            }
        }
        return coordinates;
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

    @Data
    @AllArgsConstructor
    private static class Coordinate {
        private double lat;
        private double lon;
    }
}
