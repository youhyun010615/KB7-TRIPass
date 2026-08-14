
-- 1. 여행 준비 (PRE_TRAVEL) - D30 단계
INSERT INTO checklist_templates (checklist_type, dday_stage, item_name) VALUES
('PRE_TRAVEL', 'D30', '항공권 예약 확인'),
('PRE_TRAVEL', 'D30', '숙소 예약하기'),
('PRE_TRAVEL', 'D30', '여권 유효기간 확인'),
('PRE_TRAVEL', 'D30', '국제운전면허증 발급'),
('PRE_TRAVEL', 'D30', '여행자보험 가입'),
('PRE_TRAVEL', 'D30', '여행지 맛집·카페 저장해두기'),
('PRE_TRAVEL', 'D30', '가고 싶은 관광지·명소 코스 짜기'),
('PRE_TRAVEL', 'D30', '트래블카드·월렛 준비하기');


-- 2. 여행 준비 (PRE_TRAVEL) - D7 단계
INSERT INTO checklist_templates (checklist_type, dday_stage, item_name) VALUES
('PRE_TRAVEL', 'D7', '환전 완료하기'),
('PRE_TRAVEL', 'D7', '여행 필수템 구매하기'),
('PRE_TRAVEL', 'D7', '여행에 유용한 앱 설치하기'),
('PRE_TRAVEL', 'D7', '해외 결제 카드 확인'),
('PRE_TRAVEL', 'D7', '포켓 와이파이 / 유심·eSIM 신청'),
('PRE_TRAVEL', 'D7', '여행 의류 및 착장 정하기'),
('PRE_TRAVEL', 'D7', '상비약 챙기기');

-- 3. 여행 준비 (PRE_TRAVEL) - D1 단계
INSERT INTO checklist_templates (checklist_type, dday_stage, item_name) VALUES
('PRE_TRAVEL', 'D1', '여권 챙기기'),
('PRE_TRAVEL', 'D1', '항공권·탑승 정보 확인'),
('PRE_TRAVEL', 'D1', '수하물 규정 무게 확인'),
('PRE_TRAVEL', 'D1', '전자기기 충전기 및 어댑터 챙기기'),
('PRE_TRAVEL', 'D1', '보조배터리 챙기기');

-- 4. 귀국 (RETURN) 단계
INSERT INTO checklist_templates (checklist_type, dday_stage, item_name) VALUES
('RETURN', NULL, '여권·지갑·소지품 확인'),
('RETURN', NULL, '모바일 탑승권 저장'),
('RETURN', NULL, '택스 리펀 서류 챙기기'),
('RETURN', NULL, '보조배터리 기내 소지'),
('RETURN', NULL, '트래블카드 자동충전 OFF'),
('RETURN', NULL, '지인 선물 및 기념품 챙기기'),
('RETURN', NULL, '남은 외화 처리하기');
