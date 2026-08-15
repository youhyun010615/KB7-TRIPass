USE tripass;

-- 영수증 및 예산 기능에서 공통으로 사용하는 지출 카테고리
-- 프론트 임시 카테고리 목록과 PK를 동일하게 유지한다.
INSERT INTO spending_categories (id,
                                 category_code,
                                 category_name,
                                 display_order)
VALUES (1, 'FOOD', '식비', 1),
       (2, 'TRANSPORT', '교통', 2),
       (3, 'LODGING', '숙박', 3),
       (4, 'SHOPPING', '쇼핑', 4),
       (5, 'SIGHTSEEING', '관광', 5),
       (6, 'OTHER', '기타', 6)
ON DUPLICATE KEY UPDATE category_code = VALUES(category_code),
                        category_name = VALUES(category_name),
                        display_order = VALUES(display_order);

-- AI 저축 미션 전용 카테고리 — 고정 PK를 쓰지 않고 category_code 기준으로 없을 때만 추가한다.
-- (기존 DB에 id 7~9가 이미 다른 용도로 쓰이고 있을 수 있어 고정 id INSERT는 그 데이터를 덮어쓸 위험이 있다)
INSERT INTO spending_categories (category_code, category_name, display_order)
SELECT 'CAFE', '카페', 7
WHERE NOT EXISTS (SELECT 1 FROM spending_categories WHERE category_code = 'CAFE');

INSERT INTO spending_categories (category_code, category_name, display_order)
SELECT 'LIVING', '생활비', 8
WHERE NOT EXISTS (SELECT 1 FROM spending_categories WHERE category_code = 'LIVING');

INSERT INTO spending_categories (category_code, category_name, display_order)
SELECT 'LEISURE', '취미여가', 9
WHERE NOT EXISTS (SELECT 1 FROM spending_categories WHERE category_code = 'LEISURE');