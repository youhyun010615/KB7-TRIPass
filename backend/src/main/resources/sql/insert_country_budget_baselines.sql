-- 추가 28개국 여행 예산 기준값 갱신
-- 기준: 성인 1인, MID_RANGE, 인천 출발 일반석 왕복, 대표 도시, 숙박은 2인 1실의 1인 부담액
-- 현지 비용: Budget Your Trip의 실제 여행자 지출 자료를 2026-08-19 기준 원화로 환산·천 원 단위 반올림
-- 항공료: Skyscanner/KAYAK 인천-대표 공항 왕복 노선 조사값. 상세 URL과 산정 규칙은
-- backend/docs/travel-budget-baselines.md를 참고한다.

INSERT INTO country_budget_baselines
    (country_id, round_trip_airfare, lodging_per_night, food_per_day,
     activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT c.id,
       b.round_trip_airfare,
       b.lodging_per_night,
       b.food_per_day,
       b.activity_per_day,
       b.transport_per_day,
       b.misc_per_day,
       b.data_source,
       '2026-08-19'
FROM countries c
JOIN (
    SELECT '아랍에미리트' country_name,  800000 round_trip_airfare, 258000 lodging_per_night, 146000 food_per_day, 29000 activity_per_day, 20000 transport_per_day, 30000 misc_per_day, 'Dubai: BudgetYourTrip 실제 지출; ICN-DXB Skyscanner/KAYAK; USD/KRW 1,520' data_source
    UNION ALL SELECT '호주',             900000, 145000, 126000, 52000, 24000, 25000, 'Sydney: BudgetYourTrip 실제 지출; ICN-SYD Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '바레인',           750000, 120000, 141000, 52000, 25000, 25000, 'Manama: BudgetYourTrip 실제 지출; ICN-BAH Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '브루나이',         500000,  80000,  35000, 25000, 12000, 10000, 'Bandar Seri Begawan: BudgetYourTrip/Numbeo; ICN-BWN Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '캐나다',          1400000, 134000, 166000, 47000, 49000, 40000, 'Toronto: BudgetYourTrip 실제 지출; ICN-YYZ Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '덴마크',          1200000, 129000, 106000, 53000, 36000, 30000, 'Copenhagen: BudgetYourTrip 실제 지출; ICN-CPH Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '영국',            1200000, 164000,  88000, 61000, 53000, 30000, 'London: BudgetYourTrip 실제 지출; ICN-LON Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '인도네시아',       450000,  56000,  40000, 27000, 12000, 12000, 'Bali: BudgetYourTrip 실제 지출; ICN-DPS Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '쿠웨이트',         800000, 130000,  70000, 40000, 25000, 18000, 'Kuwait City: BudgetYourTrip/Numbeo; ICN-KWI Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '말레이시아',       400000,  50000,  46000, 24000, 11000, 12000, 'Kuala Lumpur: BudgetYourTrip 실제 지출; ICN-KUL Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '노르웨이',        1300000, 106000,  84000, 38000, 33000, 25000, 'Oslo: BudgetYourTrip 실제 지출; ICN-OSL Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '뉴질랜드',        1000000,  96000,  70000, 49000, 30000, 22000, 'Auckland: BudgetYourTrip 실제 지출; ICN-AKL Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '사우디아라비아',   800000, 100000,  61000, 30000, 30000, 18000, 'Riyadh: BudgetYourTrip/Numbeo; ICN-RUH Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '스웨덴',          1200000, 106000,  84000, 50000, 33000, 25000, 'Stockholm: BudgetYourTrip 실제 지출; ICN-STO Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '싱가포르',         550000, 114000,  70000, 46000, 18000, 27000, 'Singapore: BudgetYourTrip 실제 지출; ICN-SIN Skyscanner/Cathay; USD/KRW 1,520'
    UNION ALL SELECT '태국',             480000,  68000,  46000, 27000, 18000, 15000, 'Bangkok: BudgetYourTrip 실제 지출; ICN-BKK Skyscanner/Cathay; USD/KRW 1,520'
    UNION ALL SELECT '미국',            1300000, 252000, 120000, 84000, 61000, 35000, 'New York: BudgetYourTrip 실제 지출; ICN-NYC Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '이탈리아',        1100000, 125000,  78000, 62000, 36000, 25000, 'Rome: BudgetYourTrip 실제 지출; ICN-ROM Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '스페인',          1100000, 119000,  76000, 43000, 27000, 20000, 'Madrid: BudgetYourTrip 실제 지출; ICN-MAD Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '네덜란드',        1100000, 175000, 106000, 49000, 33000, 30000, 'Amsterdam: BudgetYourTrip 실제 지출; ICN-AMS Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '벨기에',          1100000, 126000, 114000, 40000, 29000, 25000, 'Brussels: BudgetYourTrip 실제 지출; ICN-BRU Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '오스트리아',      1100000, 137000,  84000, 53000, 27000, 23000, 'Vienna: BudgetYourTrip 실제 지출; ICN-VIE Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '포르투갈',        1150000, 106000,  99000, 38000, 33000, 18000, 'Lisbon: BudgetYourTrip 실제 지출; ICN-LIS Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '그리스',          1100000, 111000,  84000, 35000, 27000, 18000, 'Athens: BudgetYourTrip 실제 지출; ICN-ATH Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '아일랜드',         685000, 145000, 106000, 43000, 30000, 25000, 'Dublin: BudgetYourTrip 실제 지출; ICN-DUB Skyscanner 684,706원; USD/KRW 1,520'
    UNION ALL SELECT '핀란드',          1100000, 114000,  84000, 38000, 30000, 20000, 'Helsinki: BudgetYourTrip 실제 지출; ICN-HEL Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '중국',             350000,  58000,  46000, 30000, 11000,  8000, 'Beijing: BudgetYourTrip 실제 지출; ICN-PEK Skyscanner/KAYAK; USD/KRW 1,520'
    UNION ALL SELECT '괌',               550000, 258000,  90000, 60000, 45000, 25000, 'Guam: KAYAK hotel/Numbeo; ICN-GUM Skyscanner; USD/KRW 1,520'
) b ON b.country_name = c.country_name
ON DUPLICATE KEY UPDATE
    round_trip_airfare = VALUES(round_trip_airfare),
    lodging_per_night = VALUES(lodging_per_night),
    food_per_day = VALUES(food_per_day),
    activity_per_day = VALUES(activity_per_day),
    transport_per_day = VALUES(transport_per_day),
    misc_per_day = VALUES(misc_per_day),
    data_source = VALUES(data_source),
    reference_date = VALUES(reference_date);
