-- supported_institutions seed data
INSERT INTO `supported_institutions` (organization_code, institution_name, business_type, display_order, is_active)
VALUES
    ('0004', 'KB국민은행', 'BK', 1, TRUE),
    ('0011', 'NH농협은행', 'BK', 2, TRUE),
    ('0020', '우리은행', 'BK', 3, TRUE),
    ('0081', 'KEB하나은행', 'BK', 4, TRUE),
    ('0088', '신한은행', 'BK', 5, TRUE),
    ('0003', 'IBK기업은행', 'BK', 6, TRUE),
    ('0089', 'K뱅크', 'BK', 7, TRUE),
    ('0031', '대구은행', 'BK', 8, TRUE),
    ('0301', 'KB카드', 'CD', 101, TRUE),
    ('0302', '현대카드', 'CD', 102, TRUE),
    ('0303', '삼성카드', 'CD', 103, TRUE),
    ('0304', 'NH카드', 'CD', 104, TRUE),
    ('0305', 'BC카드', 'CD', 105, TRUE),
    ('0306', '신한카드', 'CD', 106, TRUE),
    ('0311', '롯데카드', 'CD', 107, TRUE),
    ('0313', '하나카드', 'CD', 108, TRUE)
ON DUPLICATE KEY UPDATE
    institution_name = VALUES(institution_name),
    business_type = VALUES(business_type),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active),
    updated_at = NOW();
