#!/usr/bin/env bash
set -euo pipefail

BASE_JAVA="http://localhost:8080"
BASE_PY="http://localhost:8001"

GREEN="\033[0;32m"; RED="\033[0;31m"; YELLOW="\033[1;33m"; NC="\033[0m"
pass(){ echo -e "${GREEN}✓ PASS${NC}: $1"; }
fail(){ echo -e "${RED}✗ FAIL${NC}: $1"; EXIT_CODE=1; }
info(){ echo -e "${YELLOW}$1${NC}" >&2; }

EXIT_CODE=0

health_check() {
  info "Checking health..."
  curl -sf "$BASE_JAVA/actuator/health" >/dev/null || fail "Java health failed"
  curl -sf "$BASE_PY/health" >/dev/null || fail "Python health failed"
}

signup_and_login() {
  info "Signing up and logging in..."
  TS=$(date +%s)
  USER_EMAIL="phase3_${TS}@fingaurd.com"
  echo '{"username":"U_PLACE","email":"E_PLACE","password":"SecurePass123@","firstName":"P3","lastName":"Test"}' \
    | sed "s/U_PLACE/phase3_$TS/; s/E_PLACE/$USER_EMAIL/" > /tmp/p3_user.json
  REG_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_JAVA/api/auth/signup" -H "Content-Type: application/json" -d @/tmp/p3_user.json)
  [ "$REG_CODE" = "201" ] || fail "Signup failed ($REG_CODE)"

  echo '{"email":"E_PLACE","password":"SecurePass123@"}' | sed "s/E_PLACE/$USER_EMAIL/" > /tmp/p3_login.json
  TOKEN=$(curl -s -X POST "$BASE_JAVA/api/auth/login" -H "Content-Type: application/json" -d @/tmp/p3_login.json | grep -o '"accessToken":"[^\"]*"' | cut -d'"' -f4)
  [ -n "$TOKEN" ] || fail "Login failed; token missing"
  echo "$TOKEN"
}

python_direct_tests() {
  info "Python direct tests: high amount rule"
  RES=$(curl -s -X POST "$BASE_PY/detect" -H "Content-Type: application/json" -d '{"user_id":123,"amount":6001,"timestamp":"2025-10-24T12:00:00Z"}')
  echo "$RES" | grep -q '"is_fraudulent":true' && pass "High amount detected" || fail "High amount not detected"

  info "Python direct tests: odd-hour rule"
  RES=$(curl -s -X POST "$BASE_PY/detect" -H "Content-Type: application/json" -d '{"user_id":123,"amount":50,"timestamp":"2025-10-24T02:30:00Z"}')
  echo "$RES" | grep -q '"is_fraudulent":true' && pass "Odd-hour detected" || fail "Odd-hour not detected"

  info "Python direct tests: normal transaction"
  RES=$(curl -s -X POST "$BASE_PY/detect" -H "Content-Type: application/json" -d '{"user_id":123,"amount":50,"timestamp":"2025-10-24T12:30:00Z"}')
  echo "$RES" | grep -q '"is_fraudulent":false' && pass "Normal non-fraudulent" || fail "Normal misclassified"
}

java_integration_tests() {
  TOKEN="$1"
  info "Java integration: create normal transaction"
  echo '{"amount":45.25,"transactionType":"EXPENSE","category":"Food","description":"Normal"}' > /tmp/p3_tx_normal.json
  RES=$(curl -s -w "\n%{http_code}\n" -X POST "$BASE_JAVA/api/transactions" \
    -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
    -d @/tmp/p3_tx_normal.json)
  BODY=$(echo "$RES" | sed '$d'); CODE=$(echo "$RES" | tail -n1)
  if [ "$CODE" = "201" ]; then pass "Normal tx created"; else fail "Normal tx create failed ($CODE)"; echo "$BODY"; fi

  info "Java integration: high amount triggers fraud"
  echo '{"amount":8000,"transactionType":"EXPENSE","category":"Electronics","description":"Big"}' > /tmp/p3_tx_high.json
  RES=$(curl -s -w "\n%{http_code}\n" -X POST "$BASE_JAVA/api/transactions" \
    -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
    -d @/tmp/p3_tx_high.json)
  BODY=$(echo "$RES" | sed '$d'); CODE=$(echo "$RES" | tail -n1)
  if [ "$CODE" = "201" ]; then pass "High amount tx created"; else fail "High amount tx create failed ($CODE)"; echo "$BODY"; fi

  info "Wait for async external flag persist..."
  sleep 1
  TX_ID=$(echo "$BODY" | grep -o '"id":"[^\"]*"' | cut -d'"' -f4)
  GET=$(curl -s -X GET "$BASE_JAVA/api/transactions/$TX_ID" -H "Authorization: Bearer $TOKEN")
  echo "$GET" | grep -q '"isFraudFlagged":true' && pass "External fraud flag persisted" || fail "External fraud flag missing"
}

main(){
  health_check
  python_direct_tests
  TOKEN=$(signup_and_login)
  java_integration_tests "$TOKEN"
  if [ "${EXIT_CODE:-0}" -eq 0 ]; then
    echo -e "${GREEN}ALL PHASE 3 TESTS PASSED${NC}"
    exit 0
  else
    echo -e "${RED}PHASE 3 TESTS FAILED${NC}"
    exit 1
  fi
}

main


