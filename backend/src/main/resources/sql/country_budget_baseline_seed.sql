-- 국가·통화 시드(trip_goal_reference_seed.sql) 실행 후 적용합니다.
-- 기준 단가는 성인 1인 일반 여행자 기준이며 2026-08-11 조사값입니다.

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '독일', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Berlin'
WHERE NOT EXISTS (
    SELECT 1 FROM countries WHERE country_name = '독일'
);

INSERT INTO country_budget_baselines
(country_id, round_trip_airfare, lodging_per_night, food_per_day, activity_per_day, transport_per_day, misc_per_day, data_source, reference_date)
VALUES
((SELECT id FROM countries WHERE country_name = '프랑스'),
 1200000, 130000, 85000, 45000, 18000, 25000,
 'KAYAK 항공권가, 파리 모빌리스 1일권, 호텔·여행 예산 가이드', '2026-08-11'),
((SELECT id FROM countries WHERE country_name = '스위스'),
 1300000, 200000, 70000, 55000, 40000, 30000,
 'KAYAK 항공권가, 스위스 세이버데이패스·여행 예산 가이드', '2026-08-11'),
((SELECT MIN(id) FROM countries WHERE country_name = '독일'),
 1100000, 110000, 60000, 40000, 25000, 20000,
 'KAYAK 항공권가, 도이칠란트티켓·여행 예산 가이드', '2026-08-11'),
((SELECT id FROM countries WHERE country_name = '일본'),
 400000, 90000, 45000, 35000, 9000, 15000,
 'KAYAK 항공권가, 도쿄메트로 요금·여행 예산 가이드', '2026-08-11'),
((SELECT id FROM countries WHERE country_name = '포르투갈'),
 900000, 80000, 50000, 30000, 12000, 15000,
 'KAYAK 항공권가, 리스본 비바비아젠 교통권·여행 예산 가이드', '2026-08-11'),
((SELECT id FROM countries WHERE country_name = '홍콩'),
 350000, 130000, 40000, 35000, 7000, 15000,
 'KAYAK 항공권가, MTR·옥토퍼스 요금·여행 예산 가이드', '2026-08-11')
ON DUPLICATE KEY UPDATE
    round_trip_airfare = VALUES(round_trip_airfare),
    lodging_per_night = VALUES(lodging_per_night),
    food_per_day = VALUES(food_per_day),
    activity_per_day = VALUES(activity_per_day),
    transport_per_day = VALUES(transport_per_day),
    misc_per_day = VALUES(misc_per_day),
    data_source = VALUES(data_source),
    reference_date = VALUES(reference_date);
