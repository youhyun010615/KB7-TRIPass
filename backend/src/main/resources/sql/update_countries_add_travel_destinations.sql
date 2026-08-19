-- 국가 ID는 환경마다 달라질 수 있으므로 ID 범위 삭제를 수행하지 않는다.

-- EUR 유로존 국가 추가
INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '이탈리아', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Rome'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '이탈리아');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '스페인', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Madrid'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '스페인');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '네덜란드', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Amsterdam'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '네덜란드');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '벨기에', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Brussels'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '벨기에');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '오스트리아', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Vienna'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '오스트리아');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '포르투갈', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Lisbon'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '포르투갈');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '그리스', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Athens'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '그리스');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '아일랜드', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Dublin'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '아일랜드');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '핀란드', (SELECT id FROM currencies WHERE currency_code = 'EUR'), 'Europe/Helsinki'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '핀란드');

-- 환율/환전 도메인에서 사용하는 역외 위안화 코드(CNH)를 국가 통화에도 동일하게 사용한다.
INSERT INTO currencies (currency_code, currency_name, symbol, unit)
SELECT 'CNH', '중국 위안화', '¥', 1
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE currency_code = 'CNH');

-- 중국 추가 (CNH 위안화)
INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '중국', (SELECT id FROM currencies WHERE currency_code = 'CNH'), 'Asia/Shanghai'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '중국');

-- 기존 중국 행이 다른 위안화 코드를 참조하는 환경도 서비스 표준 CNH로 정규화한다.
UPDATE countries
SET currency_id = (SELECT id FROM currencies WHERE currency_code = 'CNH')
WHERE country_name = '중국'
  AND EXISTS (SELECT 1 FROM currencies WHERE currency_code = 'CNH');

-- 괌 추가 (USD)
INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '괌', (SELECT id FROM currencies WHERE currency_code = 'USD'), 'Pacific/Guam'
WHERE NOT EXISTS (SELECT 1 FROM countries WHERE country_name = '괌');
