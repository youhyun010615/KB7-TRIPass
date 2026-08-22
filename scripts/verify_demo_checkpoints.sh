#!/usr/bin/env bash
# =====================================================
# #300 데모 시연 체크포인트 자동 검증 스크립트
#
# yuhyun 계정으로 로그인 → 가상 날짜를 체크포인트별로 전환하며
# wallet 잔액이 기대값과 일치하는지 curl로 자동 확인한다.
#
# 사용법: bash scripts/verify_demo_checkpoints.sh
# =====================================================
set -euo pipefail

API_BASE="${API_BASE:-http://54.117.23.0:8080/api/v1}"
LOGIN_ID="yuhyun"
LOGIN_PW="Mock1234!"

echo "=== 1. 로그인 (TRIPass 계정) ==="
LOGIN_RES=$(curl -s -X POST "$API_BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"loginId\":\"$LOGIN_ID\",\"password\":\"$LOGIN_PW\"}")

TOKEN=$(echo "$LOGIN_RES" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "로그인 실패:"
  echo "$LOGIN_RES"
  exit 1
fi
echo "로그인 성공 (userId 66, yuhyun)"
echo ""

AUTH_HEADER="Authorization: Bearer $TOKEN"

check_balance() {
  local date="$1"
  local expected="$2"
  local label="$3"

  curl -s -X POST "$API_BASE/dev/override-date?date=$date" -H "$AUTH_HEADER" > /dev/null

  local actual
  actual=$(curl -s "$API_BASE/wallet" -H "$AUTH_HEADER" \
    | python3 -c "import sys,json;print(int(json.load(sys.stdin)['data']['balanceAmount']))" 2>/dev/null)

  if [ "$actual" == "$expected" ]; then
    printf "  [PASS] %-14s %-20s 기대=%-10s 실제=%-10s\n" "$date" "$label" "$expected" "$actual"
  else
    printf "  [FAIL] %-14s %-20s 기대=%-10s 실제=%-10s\n" "$date" "$label" "$expected" "$actual"
  fi
}

echo "=== 2. 체크포인트별 wallet 잔액 검증 ==="
# 주의: 가상 날짜를 실제 오늘 날짜(2026-08-22)와 '정확히' 같은 값으로 설정하면
# devDateUtil.today()가 LocalDate.now()와 같아져 동적 계산(calcBalanceAsOf)이 아닌
# wallet.balance_amount(시드 최종값)가 그대로 노출된다. 반드시 실제 날짜와 다른
# 값으로 테스트해야 하므로 8/24로 확인한다.
check_balance "2026-08-24" "0"        "여행 시작 전(첫 저축 前, 8/25 이전)"
check_balance "2026-09-30" "851000"   "8~9월 저축 완료"
check_balance "2026-10-29" "1659000"  "10월 추가저축 포함"
check_balance "2026-11-30" "2317000"  "11월 정상 저축"
check_balance "2026-12-31" "2825000"  "12월 목표 미달"
check_balance "2027-01-29" "3683000"  "1월 보충 포함"
check_balance "2027-02-28" "4491000"  "2월 추가저축 포함"
check_balance "2027-03-31" "5249000"  "3월 최종 완료(100%)"
check_balance "2027-04-05" "5249000"  "여행중(프랑스, TRAVELING)"
check_balance "2027-04-20" "5249000"  "여행 종료(ENDED)"
echo ""

echo "=== 3. 여행 상태(trip status) 확인 ==="
curl -s -X POST "$API_BASE/dev/override-date?date=2026-08-22" -H "$AUTH_HEADER" > /dev/null
STATUS_BEFORE=$(curl -s "$API_BASE/trips" -H "$AUTH_HEADER" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'][0]['status'])" 2>/dev/null)
echo "  8/22 (여행 전)   status=$STATUS_BEFORE  (기대: PLANNING)"

curl -s -X POST "$API_BASE/dev/override-date?date=2027-04-10" -H "$AUTH_HEADER" > /dev/null
STATUS_DURING=$(curl -s "$API_BASE/trips" -H "$AUTH_HEADER" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'][0]['status'])" 2>/dev/null)
echo "  4/10 (여행 중)   status=$STATUS_DURING  (기대: TRAVELING)"

curl -s -X POST "$API_BASE/dev/override-date?date=2027-04-25" -H "$AUTH_HEADER" > /dev/null
STATUS_AFTER=$(curl -s "$API_BASE/trips" -H "$AUTH_HEADER" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'][0]['status'])" 2>/dev/null)
echo "  4/25 (여행 후)   status=$STATUS_AFTER  (기대: ENDED)"
echo ""

echo "=== 4. 데모 리셋 검증 (라이브 조작 후 원복되는지) ==="
curl -s -X POST "$API_BASE/dev/override-date?date=2026-10-26" -H "$AUTH_HEADER" > /dev/null
BEFORE=$(curl -s "$API_BASE/wallet" -H "$AUTH_HEADER" | python3 -c "import sys,json;print(int(json.load(sys.stdin)['data']['balanceAmount']))")
echo "  10/26 진입 시 시드 잔액: ${BEFORE}원 (기대 1509000, 10월 B는 10/28이라 아직 미반영)"

curl -s -X POST "$API_BASE/wallet/charge" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"sourceAccountId":110,"amount":650000,"idempotencyKey":"verify-script-charge-test"}' > /dev/null 2>&1 || true
LIVE=$(curl -s "$API_BASE/wallet" -H "$AUTH_HEADER" | python3 -c "import sys,json;print(int(json.load(sys.stdin)['data']['balanceAmount']))")
echo "  라이브 충전 후: ${LIVE}원"

curl -s -X POST "$API_BASE/dev/override-date?date=2026-11-15" -H "$AUTH_HEADER" > /dev/null
curl -s -X POST "$API_BASE/dev/override-date?date=2026-10-26" -H "$AUTH_HEADER" > /dev/null
AFTER_RESET=$(curl -s "$API_BASE/wallet" -H "$AUTH_HEADER" | python3 -c "import sys,json;print(int(json.load(sys.stdin)['data']['balanceAmount']))")
if [ "$AFTER_RESET" == "1509000" ]; then
  echo "  [PASS] 날짜 재전환 후 리셋됨: ${AFTER_RESET}원"
else
  echo "  [FAIL] 리셋 안됨: ${AFTER_RESET}원 (기대 1509000)"
fi
echo ""

echo "=== 5. 가상 날짜 해제 (원상복귀) ==="
curl -s -X DELETE "$API_BASE/dev/override-date" -H "$AUTH_HEADER" > /dev/null
curl -s -X POST "$API_BASE/dev/override-date?date=2026-08-22" -H "$AUTH_HEADER" > /dev/null
echo "  완료 — 실제 날짜(2026-08-22) 기준으로 trip status 원복"
echo ""
echo "검증 끝."
