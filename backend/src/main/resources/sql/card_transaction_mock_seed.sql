-- #193 카드 거래 Mock Seed
-- 실행 전제: schema.sql 또는 update_schema_193.sql이 반영되어 있어야 합니다.
-- 주 시연 계정은 cadry1을 우선 선택하고, 없으면 가장 먼저 생성된 활성 회원을 사용합니다.

SET @primary_user_id := COALESCE(
    (SELECT id FROM users WHERE login_id = 'cadry1' AND is_deleted = 0 ORDER BY id LIMIT 1),
    (SELECT id FROM users WHERE is_deleted = 0 ORDER BY id LIMIT 1)
);

-- 사용자별 결과가 섞이지 않는지 확인할 때 사용할 두 번째 회원입니다.
SET @secondary_user_id := (
    SELECT id
    FROM users
    WHERE is_deleted = 0
      AND id <> @primary_user_id
    ORDER BY id
    LIMIT 1
);

INSERT IGNORE INTO cards
(user_id, card_name, card_company, masked_card_number, external_card_key,
 connection_type, last_synced_at, is_active)
SELECT @primary_user_id, 'KB국민 트레블러스 체크카드', 'KB국민카드', '****-****-****-1931',
       'MOCK-CARD-PRIMARY-193', 'MOCK', '2026-08-01 00:05:00', TRUE
WHERE @primary_user_id IS NOT NULL;

INSERT IGNORE INTO cards
(user_id, card_name, card_company, masked_card_number, external_card_key,
 connection_type, last_synced_at, is_active)
SELECT @secondary_user_id, '데모 라이프 카드', '데모카드', '****-****-****-1932',
       'MOCK-CARD-SECONDARY-193', 'MOCK', '2026-08-01 00:05:00', TRUE
WHERE @secondary_user_id IS NOT NULL;

SET @primary_card_id := (
    SELECT id FROM cards
    WHERE user_id = @primary_user_id
      AND external_card_key = 'MOCK-CARD-PRIMARY-193'
    LIMIT 1
);

SET @secondary_card_id := (
    SELECT id FROM cards
    WHERE user_id = @secondary_user_id
      AND external_card_key = 'MOCK-CARD-SECONDARY-193'
    LIMIT 1
);

-- 6월 비교 기준 거래
-- 7월의 카페·식비 증가와 반복 결제를 임시 분석기가 판별할 수 있도록 작은 비교군을 둔다.
INSERT IGNORE INTO transactions
(account_id, card_id, external_key, transaction_date, transaction_time,
 transaction_type, transaction_region, amount, balance_after, merchant_name, memo, category_id)
SELECT NULL, @primary_card_id, mock.external_key, mock.transaction_date, mock.transaction_time,
       'WITHDRAWAL', 'DOMESTIC', mock.amount, NULL, mock.merchant_name, NULL, NULL
FROM (
    SELECT 'MOCK:193:20260603:081500:4800' external_key, DATE '2026-06-03' transaction_date, TIME '08:15:00' transaction_time, 4800 amount, '메가MGC커피 역삼점' merchant_name
    UNION ALL SELECT 'MOCK:193:20260605:121000:11000', DATE '2026-06-05', TIME '12:10:00', 11000, '김밥천국 강남점'
    UNION ALL SELECT 'MOCK:193:20260608:184000:18900', DATE '2026-06-08', TIME '18:40:00', 18900, '이마트24 용산삼일'
    UNION ALL SELECT 'MOCK:193:20260611:075000:1500', DATE '2026-06-11', TIME '07:50:00', 1500, '서울교통공사'
    UNION ALL SELECT 'MOCK:193:20260614:143000:5500', DATE '2026-06-14', TIME '14:30:00', 5500, '스타벅스 역삼점'
    UNION ALL SELECT 'MOCK:193:20260618:201500:28000', DATE '2026-06-18', TIME '20:15:00', 28000, 'CGV 왕십리'
    UNION ALL SELECT 'MOCK:193:20260622:191000:23500', DATE '2026-06-22', TIME '19:10:00', 23500, '배달의민족 맛있는밥집'
    UNION ALL SELECT 'MOCK:193:20260625:223000:17600', DATE '2026-06-25', TIME '22:30:00', 17600, '카카오T 택시'
    UNION ALL SELECT 'MOCK:193:20260627:160000:35000', DATE '2026-06-27', TIME '16:00:00', 35000, '올리브영 강남점'
    UNION ALL SELECT 'MOCK:193:20260630:123000:9800', DATE '2026-06-30', TIME '12:30:00', 9800, '서브웨이 선릉점'
) mock
WHERE @primary_card_id IS NOT NULL;

-- 7월 분석 시나리오 거래
-- 식비·카페 반복, 2주 소비 집중, 쇼핑 고액 단건, 정기결제를 모두 포함한다.
INSERT IGNORE INTO transactions
(account_id, card_id, external_key, transaction_date, transaction_time,
 transaction_type, transaction_region, amount, balance_after, merchant_name, memo, category_id)
SELECT NULL, @primary_card_id, mock.external_key, mock.transaction_date, mock.transaction_time,
       'WITHDRAWAL', 'DOMESTIC', mock.amount, NULL, mock.merchant_name, NULL, NULL
FROM (
    SELECT 'MOCK:193:20260701:081200:5900' external_key, DATE '2026-07-01' transaction_date, TIME '08:12:00' transaction_time, 5900 amount, '스타벅스 역삼점' merchant_name
    UNION ALL SELECT 'MOCK:193:20260702:121500:12000', DATE '2026-07-02', TIME '12:15:00', 12000, '김밥천국 강남점'
    UNION ALL SELECT 'MOCK:193:20260703:210500:28500', DATE '2026-07-03', TIME '21:05:00', 28500, '배달의민족 한식공방'
    UNION ALL SELECT 'MOCK:193:20260704:154000:6500', DATE '2026-07-04', TIME '15:40:00', 6500, '스타벅스 강남R점'
    UNION ALL SELECT 'MOCK:193:20260705:231000:7400', DATE '2026-07-05', TIME '23:10:00', 7400, '이마트24 용산삼일'
    UNION ALL SELECT 'MOCK:193:20260706:075000:1500', DATE '2026-07-06', TIME '07:50:00', 1500, '서울교통공사'
    UNION ALL SELECT 'MOCK:193:20260707:184500:31900', DATE '2026-07-07', TIME '18:45:00', 31900, '배달의민족 오늘의치킨'
    UNION ALL SELECT 'MOCK:193:20260708:083000:6100', DATE '2026-07-08', TIME '08:30:00', 6100, '투썸플레이스 선릉점'
    UNION ALL SELECT 'MOCK:193:20260708:123500:14500', DATE '2026-07-08', TIME '12:35:00', 14500, '홍콩반점0410 역삼점'
    UNION ALL SELECT 'MOCK:193:20260709:190000:26800', DATE '2026-07-09', TIME '19:00:00', 26800, '배달의민족 알리오올리오'
    UNION ALL SELECT 'MOCK:193:20260710:081500:5900', DATE '2026-07-10', TIME '08:15:00', 5900, '스타벅스 역삼점'
    UNION ALL SELECT 'MOCK:193:20260710:221000:16200', DATE '2026-07-10', TIME '22:10:00', 16200, '카카오T 택시'
    UNION ALL SELECT 'MOCK:193:20260711:133000:42000', DATE '2026-07-11', TIME '13:30:00', 42000, '이마트 용산점'
    UNION ALL SELECT 'MOCK:193:20260711:171000:125000', DATE '2026-07-11', TIME '17:10:00', 125000, '쿠팡'
    UNION ALL SELECT 'MOCK:193:20260712:142000:7300', DATE '2026-07-12', TIME '14:20:00', 7300, '스타벅스 코엑스점'
    UNION ALL SELECT 'MOCK:193:20260712:193000:36500', DATE '2026-07-12', TIME '19:30:00', 36500, '배달의민족 피자하우스'
    UNION ALL SELECT 'MOCK:193:20260713:081000:5900', DATE '2026-07-13', TIME '08:10:00', 5900, '스타벅스 역삼점'
    UNION ALL SELECT 'MOCK:193:20260713:231658:7400', DATE '2026-07-13', TIME '23:16:58', 7400, '이마트24 용산삼일'
    UNION ALL SELECT 'MOCK:193:20260714:121500:13000', DATE '2026-07-14', TIME '12:15:00', 13000, '역전우동0410 선릉점'
    UNION ALL SELECT 'MOCK:193:20260715:201000:28000', DATE '2026-07-15', TIME '20:10:00', 28000, 'CGV 왕십리'
    UNION ALL SELECT 'MOCK:193:20260716:083000:4800', DATE '2026-07-16', TIME '08:30:00', 4800, '메가MGC커피 역삼점'
    UNION ALL SELECT 'MOCK:193:20260717:123000:13500', DATE '2026-07-17', TIME '12:30:00', 13500, '스부엑 강남점'
    UNION ALL SELECT 'MOCK:193:20260718:110000:48000', DATE '2026-07-18', TIME '11:00:00', 48000, '올리브영 강남점'
    UNION ALL SELECT 'MOCK:193:20260718:150000:32000', DATE '2026-07-18', TIME '15:00:00', 32000, '교보문고 광화문점'
    UNION ALL SELECT 'MOCK:193:20260719:193000:25900', DATE '2026-07-19', TIME '19:30:00', 25900, '배달의민족 떡볶이배달'
    UNION ALL SELECT 'MOCK:193:20260720:075000:1500', DATE '2026-07-20', TIME '07:50:00', 1500, '서울교통공사'
    UNION ALL SELECT 'MOCK:193:20260721:183000:67000', DATE '2026-07-21', TIME '18:30:00', 67000, '롯데마트 서초점'
    UNION ALL SELECT 'MOCK:193:20260722:082000:5900', DATE '2026-07-22', TIME '08:20:00', 5900, '스타벅스 역삼점'
    UNION ALL SELECT 'MOCK:193:20260723:221500:18900', DATE '2026-07-23', TIME '22:15:00', 18900, '카카오T 택시'
    UNION ALL SELECT 'MOCK:193:20260724:121000:14000', DATE '2026-07-24', TIME '12:10:00', 14000, '순남시래기 역삼점'
    UNION ALL SELECT 'MOCK:193:20260725:143000:6900', DATE '2026-07-25', TIME '14:30:00', 6900, '스타벅스 강남R점'
    UNION ALL SELECT 'MOCK:193:20260726:180000:45000', DATE '2026-07-26', TIME '18:00:00', 45000, '네이버페이 뮤지컬예매'
    UNION ALL SELECT 'MOCK:193:20260727:193000:29200', DATE '2026-07-27', TIME '19:30:00', 29200, '배달의민족 일식당'
    UNION ALL SELECT 'MOCK:193:20260728:081000:4900', DATE '2026-07-28', TIME '08:10:00', 4900, '컴포즈커피 선릉점'
    UNION ALL SELECT 'MOCK:193:20260729:120000:12500', DATE '2026-07-29', TIME '12:00:00', 12500, '한솔도시락 강남점'
    UNION ALL SELECT 'MOCK:193:20260730:090000:10900', DATE '2026-07-30', TIME '09:00:00', 10900, '넷플릭스 정기결제'
    UNION ALL SELECT 'MOCK:193:20260731:190000:33800', DATE '2026-07-31', TIME '19:00:00', 33800, '배달의민족 주말야식'
) mock
WHERE @primary_card_id IS NOT NULL;

-- 두 번째 회원은 서로 다른 카드·거래 결과를 검증하기 위한 소규모 시나리오다.
INSERT IGNORE INTO transactions
(account_id, card_id, external_key, transaction_date, transaction_time,
 transaction_type, transaction_region, amount, balance_after, merchant_name, memo, category_id)
SELECT NULL, @secondary_card_id, mock.external_key, mock.transaction_date, mock.transaction_time,
       'WITHDRAWAL', 'DOMESTIC', mock.amount, NULL, mock.merchant_name, NULL, NULL
FROM (
    SELECT 'MOCK:193:SECONDARY:20260704:9900' external_key, DATE '2026-07-04' transaction_date, TIME '12:00:00' transaction_time, 9900 amount, '편의점 데모점' merchant_name
    UNION ALL SELECT 'MOCK:193:SECONDARY:20260712:4200', DATE '2026-07-12', TIME '09:10:00', 4200, '데모커피'
    UNION ALL SELECT 'MOCK:193:SECONDARY:20260720:1500', DATE '2026-07-20', TIME '08:00:00', 1500, '서울교통공사'
) mock
WHERE @secondary_card_id IS NOT NULL;
