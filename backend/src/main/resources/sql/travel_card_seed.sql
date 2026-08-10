INSERT INTO travel_cards (
    card_name,
    card_company,
    bank_name,
    required_account,
    instant_use,
    applied_rate_info,
    exchange_fee,
    re_exchange_fee,
    payment_fee,
    withdrawal_fee,
    auto_charge_supported,
    is_transit_card,
    is_active
)
VALUES

-- KB국민 트래블러스 체크카드
(
    'KB국민 트래블러스 체크카드',
    'KB국민카드',
    'KB국민은행',
    'KB Pay 외화머니 또는 KB국민은행 외화통장',
    TRUE,
    '56종 통화 환율우대 100%',
    '56종 통화 환율우대 100%',
    '환급 시 환율우대 100% (2026-12-31까지)',
    '해외 가맹점 국제브랜드 1% + 해외서비스 0.25% 면제',
    '해외 ATM 국제브랜드 1% + 건당 USD 3 월 10회 면제',
    FALSE,
    TRUE,
    TRUE
),

-- 하나 트래블로그 체크카드
(
    '트래블로그 체크카드',
    '하나카드',
    '하나은행',
    '외화 하나머니',
    TRUE,
    '58종 통화 환율우대 100%',
    '환율우대 100%',
    '환급 시 환급수수료 적용',
    '해외서비스 수수료 건당 USD 0.5 및 국제브랜드 수수료 면제',
    '해외인출 수수료 건당 USD 3 및 국제브랜드 수수료 면제',
    TRUE,
    TRUE,
    TRUE
),

-- 하나 트래블GO 체크카드
(
    '트래블GO 체크카드',
    '하나카드',
    '하나은행',
    '외화 하나머니',
    TRUE,
    '58종 통화 환율우대 100%',
    '환율우대 100%',
    '환급 시 환급수수료 적용',
    '해외서비스 수수료 및 국제브랜드 수수료 면제',
    '해외인출 수수료 및 국제브랜드 수수료 면제',
    TRUE,
    TRUE,
    TRUE
),

-- 우리 위비트래블 체크카드
(
    '위비트래블 체크카드',
    '우리카드',
    '우리은행',
    '우리은행 지정 외화예금',
    FALSE,
    '위비트래블 외화예금 지원 통화 환전',
    '위비트래블 외화예금 환전 혜택 적용',
    NULL,
    '해외 가맹점 Mastercard 1% + 해외서비스 건당 USD 0.5 면제',
    '해외 ATM Mastercard 1% + 건당 USD 3 수수료 발생',
    FALSE,
    TRUE,
    TRUE
),

-- 신한 SOL트래블 체크카드
(
    'SOL트래블 체크카드',
    '신한카드',
    '신한은행',
    'SOL트래블 외화예금',
    FALSE,
    '42종 통화 환율우대 100%',
    '환율우대 100%',
    NULL,
    '해외 가맹점 국제브랜드 1% + 해외서비스 0.2% 면제',
    '해외 ATM 국제브랜드 1% + 건당 USD 3 면제',
    TRUE,
    TRUE,
    TRUE
);