-- 전체 국가 예산 기준값 추가 (기존 5개국 제외)
-- 2024-2025 실제 시세 기반, 기존 FR/CH/DE/JP/HK 대비 캘리브레이션

-- 아랍에미리트 (두바이)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 1, 800000, 150000, 60000, 50000, 15000, 20000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 1);

-- 호주 (시드니)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 2, 900000, 140000, 60000, 45000, 20000, 20000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 2);

-- 바레인
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 3, 750000, 100000, 40000, 30000, 12000, 15000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 3);

-- 브루나이
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 4, 500000, 80000, 25000, 20000, 8000, 10000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 4);

-- 캐나다 (토론토)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 5, 1200000, 150000, 60000, 40000, 15000, 20000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 5);

-- 덴마크 (코펜하겐)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 7, 1200000, 160000, 80000, 45000, 25000, 25000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 7);

-- 영국 (런던)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 9, 1200000, 150000, 70000, 45000, 20000, 25000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 9);

-- 인도네시아 (발리)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 11, 450000, 60000, 20000, 25000, 8000, 10000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 11);

-- 쿠웨이트
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 13, 800000, 110000, 40000, 25000, 12000, 15000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 13);

-- 말레이시아 (쿠알라룸푸르)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 14, 400000, 60000, 20000, 20000, 5000, 10000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 14);

-- 노르웨이 (오슬로)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 15, 1300000, 180000, 80000, 50000, 30000, 30000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 15);

-- 뉴질랜드 (오클랜드)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 16, 1000000, 130000, 55000, 45000, 20000, 20000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 16);

-- 사우디아라비아 (리야드)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 17, 800000, 120000, 40000, 30000, 12000, 15000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 17);

-- 스웨덴 (스톡홀름)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 18, 1200000, 150000, 70000, 40000, 25000, 25000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 18);

-- 싱가포르
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 19, 400000, 120000, 30000, 35000, 8000, 12000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 19);

-- 태국 (방콕)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 20, 350000, 50000, 15000, 20000, 5000, 8000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 20);

-- 미국 (뉴욕)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 21, 1300000, 200000, 70000, 50000, 15000, 25000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 21);

-- 이탈리아 (로마)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 56, 1100000, 120000, 60000, 40000, 15000, 20000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 56);

-- 스페인 (마드리드)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 57, 1100000, 100000, 50000, 35000, 15000, 18000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 57);

-- 네덜란드 (암스테르담)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 58, 1100000, 140000, 60000, 40000, 15000, 20000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 58);

-- 벨기에 (브뤼셀)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 59, 1100000, 110000, 55000, 35000, 15000, 18000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 59);

-- 오스트리아 (빈)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 60, 1100000, 120000, 55000, 40000, 18000, 20000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 60);

-- 포르투갈 (리스본)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 61, 1150000, 90000, 40000, 30000, 12000, 15000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 61);

-- 그리스 (아테네)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 62, 1100000, 90000, 40000, 30000, 12000, 15000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 62);

-- 아일랜드 (더블린)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 63, 1200000, 140000, 65000, 40000, 18000, 22000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 63);

-- 핀란드 (헬싱키)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 64, 1100000, 140000, 65000, 40000, 20000, 22000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 64);

-- 중국 (베이징)
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 65, 350000, 70000, 30000, 25000, 5000, 10000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 65);

-- 괌
INSERT INTO country_budget_baselines (country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
SELECT 66, 450000, 120000, 50000, 40000, 10000, 15000, 'KAYAK/Skyscanner/BudgetYourTrip 2024-2025 평균, 기존 5개국 대비 캘리브레이션', '2026-08-19'
WHERE NOT EXISTS (SELECT 1 FROM country_budget_baselines WHERE country_id = 66);
