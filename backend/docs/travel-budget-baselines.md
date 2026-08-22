# 국가별 여행 예산 기준값

## 산정 기준

- 조사일: 2026-08-19
- 여행자: 성인 1인, `MID_RANGE`
- 항공: 인천(ICN) 출발 일반석 왕복, 유연한 날짜 검색의 통상 가격대. 실시간 최저가는 변동성이 커서 그대로 사용하지 않는다.
- 숙박: 대표 도시의 2인 1실 중급 숙소에서 1인이 부담하는 1박 비용
- 현지 비용: Budget Your Trip에 수집된 실제 여행자 지출의 식비·현지 교통·관광/엔터테인먼트 항목
- 기타 비용: 통신, 생수, 소액 수수료와 가격 변동을 흡수하는 보수적 여유 금액
- 환산: USD/KRW 1,520원을 공통 환산 기준으로 사용하고 천 원 단위로 반올림
- 계산: `항공료 + (1박 숙박비 × 숙박일) + ((식비 + 액티비티 + 교통 + 기타) × 여행일)`
- 항공·숙박은 사전 지출이며 식비·액티비티·교통·기타만 여행 저축 목표에 포함한다.

Budget Your Trip의 카테고리 값은 표본별 평균이므로 카테고리 합이 사이트의 전체 일평균과 정확히 같지 않을 수 있다. 서비스 기준값은 각 카테고리의 비율과 대표 도시의 전체 일평균을 함께 비교해 과대 계산되는 값을 보정했다.

## 조사 출처

| 국가 | 기준 도시 | 현지 지출 자료 | 항공 검색 노선 |
|---|---|---|---|
| 아랍에미리트 | 두바이 | https://www.budgetyourtrip.com/united-arab-emirates/dubai | ICN-DXB |
| 호주 | 시드니 | https://www.budgetyourtrip.com/australia/sydney | ICN-SYD |
| 바레인 | 마나마 | https://www.budgetyourtrip.com/bahrain | ICN-BAH |
| 브루나이 | 반다르스리브가완 | https://www.budgetyourtrip.com/brunei | ICN-BWN |
| 캐나다 | 토론토 | https://www.budgetyourtrip.com/canada/toronto | ICN-YYZ |
| 덴마크 | 코펜하겐 | https://www.budgetyourtrip.com/denmark/copenhagen | ICN-CPH |
| 영국 | 런던 | https://www.budgetyourtrip.com/united-kingdom/london | ICN-LON |
| 인도네시아 | 발리 | https://www.budgetyourtrip.com/indonesia/bali | ICN-DPS |
| 쿠웨이트 | 쿠웨이트시티 | https://www.budgetyourtrip.com/kuwait | ICN-KWI |
| 말레이시아 | 쿠알라룸푸르 | https://www.budgetyourtrip.com/malaysia/kuala-lumpur | ICN-KUL |
| 노르웨이 | 오슬로 | https://www.budgetyourtrip.com/norway/oslo | ICN-OSL |
| 뉴질랜드 | 오클랜드 | https://www.budgetyourtrip.com/new-zealand/auckland | ICN-AKL |
| 사우디아라비아 | 리야드 | https://www.budgetyourtrip.com/saudi-arabia | ICN-RUH |
| 스웨덴 | 스톡홀름 | https://www.budgetyourtrip.com/sweden/stockholm | ICN-STO |
| 싱가포르 | 싱가포르 | https://www.budgetyourtrip.com/singapore | ICN-SIN |
| 태국 | 방콕 | https://www.budgetyourtrip.com/thailand/bangkok | ICN-BKK |
| 미국 | 뉴욕 | https://www.budgetyourtrip.com/united-states-of-america/new-york-city | ICN-NYC |
| 이탈리아 | 로마 | https://www.budgetyourtrip.com/italy/rome | ICN-ROM |
| 스페인 | 마드리드 | https://www.budgetyourtrip.com/spain/madrid | ICN-MAD |
| 네덜란드 | 암스테르담 | https://www.budgetyourtrip.com/netherlands/amsterdam | ICN-AMS |
| 벨기에 | 브뤼셀 | https://www.budgetyourtrip.com/belgium/brussels | ICN-BRU |
| 오스트리아 | 빈 | https://www.budgetyourtrip.com/austria/vienna | ICN-VIE |
| 포르투갈 | 리스본 | https://www.budgetyourtrip.com/portugal/lisbon | ICN-LIS |
| 그리스 | 아테네 | https://www.budgetyourtrip.com/greece/athens | ICN-ATH |
| 아일랜드 | 더블린 | https://www.budgetyourtrip.com/ireland/dublin | ICN-DUB |
| 핀란드 | 헬싱키 | https://www.budgetyourtrip.com/finland/helsinki | ICN-HEL |
| 중국 | 베이징 | https://www.budgetyourtrip.com/china/beijing | ICN-PEK |
| 괌 | 괌 | https://www.budgetyourtrip.com/guam | ICN-GUM |

항공료는 각 노선을 Skyscanner와 KAYAK에서 교차 확인했다. 특정 날짜의 프로모션 최저가가 아니라 추천 예산에 적합한 왕복 기준가를 사용한다.

- Skyscanner: https://www.skyscanner.co.kr/transport/flights/
- KAYAK: https://www.kayak.co.kr/flights
- 환율 참고: https://eng.bond.co.kr/post/dawn/36658
- 현지 자료가 부족한 브루나이·쿠웨이트·사우디아라비아·괌 보조 자료: https://www.numbeo.com/cost-of-living/

## 운영 규칙

1. `trip_goal_reference_seed.sql`을 먼저 적용해 국가와 통화를 준비한다.
2. 신규 DB는 `country_budget_baseline_seed.sql` 적용 후 `insert_country_budget_baselines.sql`을 적용한다.
3. 기존 DB에는 `update_countries_add_travel_destinations.sql`과 `insert_country_budget_baselines.sql`을 순서대로 적용한다.
4. 기준값을 갱신할 때는 조사일과 출처를 함께 바꾸고 계산기 테스트를 실행한다.
