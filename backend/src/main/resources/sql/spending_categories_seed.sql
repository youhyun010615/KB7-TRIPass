USE tripass;

-- 영수증 및 예산 기능에서 공통으로 사용하는 지출 카테고리
-- 프론트 임시 카테고리 목록과 PK를 동일하게 유지한다.
INSERT INTO spending_categories (id,
                                 category_name,
                                 display_order)
VALUES (1, '식비', 1),
       (2, '교통', 2),
       (3, '숙박', 3),
       (4, '쇼핑', 4),
       (5, '관광', 5),
       (6, '기타', 6)
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        display_order = VALUES(display_order);