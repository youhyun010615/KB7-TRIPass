USE tripass;

-- 영수증 및 예산 기능에서 공통으로 사용하는 지출 카테고리 + AI 저축 미션 전용 카테고리
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
       (6, 'OTHER', '기타', 6),
       (7, 'CAFE', '카페', 7),
       (8, 'LIVING', '생활비', 8),
       (9, 'LEISURE', '취미여가', 9)
ON DUPLICATE KEY UPDATE category_code = VALUES(category_code),
                        category_name = VALUES(category_name),
                        display_order = VALUES(display_order);