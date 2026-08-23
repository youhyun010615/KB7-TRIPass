#!/usr/bin/env bash
set -euo pipefail

# 촬영 전 yuhyun 데모 계정을 재구성하고 Mock 거래를 미리 적재한다.
# 사용법: 백엔드가 실행 중인 상태에서 backend 디렉터리에서 실행
#   ./scripts/prepare-demo-yuhyun.sh

DEMO_API_BASE="${DEMO_API_BASE:-http://127.0.0.1:8080}"
DEMO_DB_HOST="${DEMO_DB_HOST:-127.0.0.1}"
DEMO_DB_PORT="${DEMO_DB_PORT:-3306}"
DEMO_DB_NAME="${DEMO_DB_NAME:-tripass}"
DEMO_DB_USER="${DEMO_DB_USER:-root}"
DEMO_DB_PASSWORD="${DEMO_DB_PASSWORD:-}"

mysql_args=(-h "$DEMO_DB_HOST" -P "$DEMO_DB_PORT" -u "$DEMO_DB_USER")
if [[ -n "$DEMO_DB_PASSWORD" ]]; then
  mysql_args+=("-p$DEMO_DB_PASSWORD")
fi

mysql "${mysql_args[@]}" < src/main/resources/sql/update_schema_demo_clock.sql
mysql "${mysql_args[@]}" < src/main/resources/sql/update_schema_demo_runtime.sql
mysql "${mysql_args[@]}" < src/main/resources/sql/travel_checklist_seeds.sql
mysql "${mysql_args[@]}" < src/main/resources/sql/demo_yuhyun_seed.sql

login_response="$(mktemp)"
trap 'rm -f "$login_response"' EXIT

curl --fail-with-body --silent --show-error \
  -H 'Content-Type: application/json' \
  -d '{"loginId":"yuhyun","password":"Mock1234!"}' \
  "$DEMO_API_BASE/api/v1/auth/login" > "$login_response"

access_token="$(jq -r '.data.accessToken // empty' "$login_response")"
if [[ -z "$access_token" ]]; then
  echo "데모 로그인 토큰을 받지 못했습니다." >&2
  exit 1
fi

user_id="$(mysql "${mysql_args[@]}" -N -s "$DEMO_DB_NAME" -e \
  "SELECT id FROM users WHERE login_provider='LOCAL' AND login_id='yuhyun' LIMIT 1")"
account_id="$(mysql "${mysql_args[@]}" -N -s "$DEMO_DB_NAME" -e \
  "SELECT id FROM accounts WHERE user_id=$user_id AND account_number='496501-01-110300' AND is_deleted=0 LIMIT 1")"
domestic_card_id="$(mysql "${mysql_args[@]}" -N -s "$DEMO_DB_NAME" -e \
  "SELECT id FROM cards WHERE user_id=$user_id AND masked_card_number LIKE '%9901' AND is_deleted=0 LIMIT 1")"
travel_card_id="$(mysql "${mysql_args[@]}" -N -s "$DEMO_DB_NAME" -e \
  "SELECT id FROM cards WHERE user_id=$user_id AND masked_card_number LIKE '%9902' AND is_deleted=0 LIMIT 1")"

# 여행 중 날짜에서 동기화해야 해외 거래가 여행·국가에 자동 귀속된다.
curl --fail-with-body --silent --show-error -o /dev/null -X POST \
  -H "Authorization: Bearer $access_token" \
  "$DEMO_API_BASE/api/v1/dev/override-date?date=2027-04-12"

curl --fail-with-body --silent --show-error -o /dev/null -X POST \
  -H "Authorization: Bearer $access_token" \
  -H 'Content-Type: application/json' \
  -d "{\"accountId\":$account_id,\"startDate\":\"2026-04-01\",\"endDate\":\"2027-04-12\"}" \
  "$DEMO_API_BASE/api/v1/accounts/transactions"

curl --fail-with-body --silent --show-error -o /dev/null -X POST \
  -H "Authorization: Bearer $access_token" \
  "$DEMO_API_BASE/api/v1/cards/$domestic_card_id/transactions/fetch?startDate=2026-04-01&endDate=2027-04-03"

curl --fail-with-body --silent --show-error -o /dev/null -X POST \
  -H "Authorization: Bearer $access_token" \
  "$DEMO_API_BASE/api/v1/cards/$travel_card_id/transactions/fetch?startDate=2027-04-04&endDate=2027-04-12"

# 촬영 시작 체크포인트로 복귀한다. 미래 거래는 가상 날짜 필터로 노출되지 않는다.
curl --fail-with-body --silent --show-error -o /dev/null -X POST \
  -H "Authorization: Bearer $access_token" \
  "$DEMO_API_BASE/api/v1/dev/override-date?date=2026-08-26"

mysql "${mysql_args[@]}" -N -s "$DEMO_DB_NAME" -e \
  "SELECT CONCAT('준비 완료: user=', u.login_id,
                 ', trip=', t.trip_name,
                 ', transactions=', COUNT(DISTINCT tx.id),
                 ', start_date=', DATE_FORMAT(u.dev_override_date, '%Y-%m-%d'))
     FROM users u
     JOIN trips t ON t.user_id=u.id AND t.is_deleted=0
     LEFT JOIN accounts a ON a.user_id=u.id AND a.is_deleted=0
     LEFT JOIN cards c ON c.user_id=u.id AND c.is_deleted=0
     LEFT JOIN transactions tx ON tx.account_id=a.id OR tx.card_id=c.id
    WHERE u.id=$user_id
    GROUP BY u.id,t.id;"
