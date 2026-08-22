-- #179 여행 목표 등록 화면용 국가·통화 기준 데이터
-- 이미 존재하는 통화/국가는 INSERT IGNORE로 건너뜁니다.

INSERT IGNORE INTO currencies (currency_code, currency_name, symbol, unit) VALUES
('AED', '아랍에미리트 디르함', 'د.إ', 1),
('AUD', '호주 달러', '$', 1),
('BHD', '바레인 디나르', '.د.ب', 1),
('BND', '브루나이 달러', '$', 1),
('CAD', '캐나다 달러', '$', 1),
('CHF', '스위스 프랑', 'CHF', 1),
('CNH', '중국 위안화', '¥', 1),
('DKK', '덴마크 크로네', 'kr', 1),
('EUR', '유로', '€', 1),
('GBP', '영국 파운드', '£', 1),
('HKD', '홍콩 달러', '$', 1),
('IDR', '인도네시아 루피아', 'Rp', 100),
('JPY', '일본 엔', '¥', 100),
('KWD', '쿠웨이트 디나르', 'د.ك', 1),
('MYR', '말레이시아 링깃', 'RM', 1),
('NOK', '노르웨이 크로네', 'kr', 1),
('NZD', '뉴질랜드 달러', '$', 1),
('SAR', '사우디아라비아 리얄', '﷼', 1),
('SEK', '스웨덴 크로나', 'kr', 1),
('SGD', '싱가포르 달러', '$', 1),
('THB', '태국 바트', '฿', 1),
('USD', '미국 달러', '$', 1);

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT seed.country_name, cur.id, seed.time_zone
FROM (
    SELECT '아랍에미리트' country_name, 'AED' currency_code, 'Asia/Dubai' time_zone
    UNION ALL SELECT '호주', 'AUD', 'Australia/Sydney'
    UNION ALL SELECT '바레인', 'BHD', 'Asia/Bahrain'
    UNION ALL SELECT '브루나이', 'BND', 'Asia/Brunei'
    UNION ALL SELECT '캐나다', 'CAD', 'America/Toronto'
    UNION ALL SELECT '스위스', 'CHF', 'Europe/Zurich'
    UNION ALL SELECT '중국', 'CNH', 'Asia/Shanghai'
    UNION ALL SELECT '덴마크', 'DKK', 'Europe/Copenhagen'
    UNION ALL SELECT '프랑스', 'EUR', 'Europe/Paris'
    UNION ALL SELECT '영국', 'GBP', 'Europe/London'
    UNION ALL SELECT '홍콩', 'HKD', 'Asia/Hong_Kong'
    UNION ALL SELECT '인도네시아', 'IDR', 'Asia/Jakarta'
    UNION ALL SELECT '일본', 'JPY', 'Asia/Tokyo'
    UNION ALL SELECT '쿠웨이트', 'KWD', 'Asia/Kuwait'
    UNION ALL SELECT '말레이시아', 'MYR', 'Asia/Kuala_Lumpur'
    UNION ALL SELECT '노르웨이', 'NOK', 'Europe/Oslo'
    UNION ALL SELECT '뉴질랜드', 'NZD', 'Pacific/Auckland'
    UNION ALL SELECT '사우디아라비아', 'SAR', 'Asia/Riyadh'
    UNION ALL SELECT '스웨덴', 'SEK', 'Europe/Stockholm'
    UNION ALL SELECT '싱가포르', 'SGD', 'Asia/Singapore'
    UNION ALL SELECT '태국', 'THB', 'Asia/Bangkok'
    UNION ALL SELECT '미국', 'USD', 'America/New_York'
    UNION ALL SELECT '이탈리아', 'EUR', 'Europe/Rome'
    UNION ALL SELECT '스페인', 'EUR', 'Europe/Madrid'
    UNION ALL SELECT '네덜란드', 'EUR', 'Europe/Amsterdam'
    UNION ALL SELECT '벨기에', 'EUR', 'Europe/Brussels'
    UNION ALL SELECT '오스트리아', 'EUR', 'Europe/Vienna'
    UNION ALL SELECT '포르투갈', 'EUR', 'Europe/Lisbon'
    UNION ALL SELECT '그리스', 'EUR', 'Europe/Athens'
    UNION ALL SELECT '아일랜드', 'EUR', 'Europe/Dublin'
    UNION ALL SELECT '핀란드', 'EUR', 'Europe/Helsinki'
    UNION ALL SELECT '괌', 'USD', 'Pacific/Guam'
) seed
JOIN currencies cur ON cur.currency_code = seed.currency_code
WHERE NOT EXISTS (
    SELECT 1 FROM countries c WHERE c.country_name = seed.country_name
);
