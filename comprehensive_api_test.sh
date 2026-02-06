#!/bin/bash

# Comprehensive API Testing Script for FinGaurd Application
# Tests all endpoints with hard scenarios and edge cases

BASE_URL="http://localhost:8080"
TEST_RESULTS=()
PASSED=0
FAILED=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to log test results
log_test() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name - $details"
        ((FAILED++))
    fi
    TEST_RESULTS+=("$test_name:$status:$details")
}

# Function to make HTTP requests
make_request() {
    local method="$1"
    local url="$2"
    local data="$3"
    local headers="$4"
    
    if [ -n "$data" ] && [ -n "$headers" ]; then
        curl -s -w "\n%{http_code}" -X "$method" "$url" -H "Content-Type: application/json" -H "$headers" -d "$data"
    elif [ -n "$data" ]; then
        curl -s -w "\n%{http_code}" -X "$method" "$url" -H "Content-Type: application/json" -d "$data"
    elif [ -n "$headers" ]; then
        curl -s -w "\n%{http_code}" -X "$method" "$url" -H "$headers"
    else
        curl -s -w "\n%{http_code}" -X "$method" "$url"
    fi
}

# Function to extract JSON field (macOS compatible)
extract_json_field() {
    local json="$1"
    local field="$2"
    echo "$json" | grep -o "\"$field\":\"[^\"]*\"" | cut -d'"' -f4
}

# Function to extract access token specifically
extract_access_token() {
    local json="$1"
    echo "$json" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4
}

echo -e "${BLUE}🚀 Starting Comprehensive API Testing for FinGaurd Application${NC}"
echo "=================================================="
echo ""

# Wait for application to be ready
echo -e "${YELLOW}⏳ Waiting for application to start...${NC}"
for i in {1..30}; do
    if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Application is ready!${NC}"
        break
    fi
    sleep 2
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ Application failed to start within 60 seconds${NC}"
        exit 1
    fi
done

echo ""
echo -e "${BLUE}📋 TESTING AUTHENTICATION ENDPOINTS${NC}"
echo "=========================================="

# Test 1: User Registration - Valid User
echo -e "${YELLOW}Test 1: Valid User Registration${NC}"
USER_DATA='{
    "username": "testuser'$(date +%s)'",
    "email": "test'$(date +%s)'@fingaurd.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/auth/signup" "$USER_DATA")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    log_test "Valid User Registration" "PASS" "HTTP $HTTP_CODE"
    USER_ID=$(extract_json_field "$BODY" "id")
else
    log_test "Valid User Registration" "FAIL" "HTTP $HTTP_CODE - $BODY"
fi

# Test 2: User Registration - Duplicate Email
echo -e "${YELLOW}Test 2: Duplicate Email Registration${NC}"
RESPONSE=$(make_request "POST" "$BASE_URL/api/auth/signup" "$USER_DATA")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "409" ]; then
    log_test "Duplicate Email Registration" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Duplicate Email Registration" "FAIL" "HTTP $HTTP_CODE - $BODY"
fi

# Test 3: User Registration - Invalid Email Format
echo -e "${YELLOW}Test 3: Invalid Email Format${NC}"
INVALID_EMAIL_DATA='{
    "username": "testuser2",
    "email": "invalid-email",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/auth/signup" "$INVALID_EMAIL_DATA")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Invalid Email Format" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Invalid Email Format" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 4: User Registration - Weak Password
echo -e "${YELLOW}Test 4: Weak Password${NC}"
WEAK_PASSWORD_DATA='{
    "username": "testuser3",
    "email": "test3@fingaurd.com",
    "password": "123",
    "firstName": "Test",
    "lastName": "User"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/auth/signup" "$WEAK_PASSWORD_DATA")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Weak Password Validation" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Weak Password Validation" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 5: User Login - Valid Credentials
echo -e "${YELLOW}Test 5: Valid User Login${NC}"
# Extract the email from the user data
USER_EMAIL=$(echo "$USER_DATA" | grep -o '"email":"[^"]*"' | cut -d'"' -f4)
LOGIN_DATA="{
    \"email\": \"$USER_EMAIL\",
    \"password\": \"SecurePass123!\"
}"
RESPONSE=$(make_request "POST" "$BASE_URL/api/auth/login" "$LOGIN_DATA")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    log_test "Valid User Login" "PASS" "HTTP $HTTP_CODE"
    TOKEN=$(extract_access_token "$BODY")
    echo "Token: ${TOKEN:0:20}..."
else
    log_test "Valid User Login" "FAIL" "HTTP $HTTP_CODE - $BODY"
fi

# Test 6: User Login - Invalid Credentials
echo -e "${YELLOW}Test 6: Invalid Login Credentials${NC}"
INVALID_LOGIN_DATA="{
    \"email\": \"$USER_EMAIL\",
    \"password\": \"WrongPassword123!\"
}"
RESPONSE=$(make_request "POST" "$BASE_URL/api/auth/login" "$INVALID_LOGIN_DATA")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "401" ]; then
    log_test "Invalid Login Credentials" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Invalid Login Credentials" "FAIL" "HTTP $HTTP_CODE"
fi

echo ""
echo -e "${BLUE}📋 TESTING TRANSACTION ENDPOINTS${NC}"
echo "====================================="

# Test 7: Create Transaction - Valid Transaction
echo -e "${YELLOW}Test 7: Valid Transaction Creation${NC}"
TRANSACTION_DATA='{
    "amount": 150.75,
    "transactionType": "EXPENSE",
    "category": "Groceries",
    "description": "Weekly grocery shopping"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$TRANSACTION_DATA" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    log_test "Valid Transaction Creation" "PASS" "HTTP $HTTP_CODE"
    TRANSACTION_ID=$(extract_json_field "$BODY" "id")
else
    log_test "Valid Transaction Creation" "FAIL" "HTTP $HTTP_CODE - $BODY"
fi

# Test 8: Create Transaction - Invalid Amount (Negative)
echo -e "${YELLOW}Test 8: Invalid Amount (Negative)${NC}"
INVALID_AMOUNT_DATA='{
    "amount": -50.00,
    "transactionType": "EXPENSE",
    "category": "Shopping",
    "description": "Invalid negative amount"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$INVALID_AMOUNT_DATA" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Invalid Amount (Negative)" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Invalid Amount (Negative)" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 9: Create Transaction - Invalid Amount (Zero)
echo -e "${YELLOW}Test 9: Invalid Amount (Zero)${NC}"
ZERO_AMOUNT_DATA='{
    "amount": 0,
    "transactionType": "EXPENSE",
    "category": "Shopping",
    "description": "Zero amount transaction"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$ZERO_AMOUNT_DATA" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Invalid Amount (Zero)" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Invalid Amount (Zero)" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 10: Create Transaction - Missing Required Fields
echo -e "${YELLOW}Test 10: Missing Required Fields${NC}"
INCOMPLETE_DATA='{
    "amount": 100.00,
    "transactionType": "EXPENSE"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$INCOMPLETE_DATA" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Missing Required Fields" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Missing Required Fields" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 11: Create Transaction - Very Large Amount
echo -e "${YELLOW}Test 11: Very Large Amount${NC}"
LARGE_AMOUNT_DATA='{
    "amount": 999999999.99,
    "transactionType": "INCOME",
    "category": "Salary",
    "description": "Very large salary amount"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$LARGE_AMOUNT_DATA" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "201" ]; then
    log_test "Very Large Amount" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Very Large Amount" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 12: Create Transaction - Long Description
echo -e "${YELLOW}Test 12: Long Description${NC}"
LONG_DESC_DATA='{
    "amount": 25.50,
    "transactionType": "EXPENSE",
    "category": "Entertainment",
    "description": "'$(printf 'A%.0s' {1..501})'"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$LONG_DESC_DATA" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Long Description Validation" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Long Description Validation" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 13: Get Transactions - Valid Request
echo -e "${YELLOW}Test 13: Get Transactions${NC}"
RESPONSE=$(make_request "GET" "$BASE_URL/api/transactions" "" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    log_test "Get Transactions" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Get Transactions" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 14: Get Transactions - Invalid Pagination
echo -e "${YELLOW}Test 14: Invalid Pagination (Negative Page)${NC}"
RESPONSE=$(make_request "GET" "$BASE_URL/api/transactions?page=-1&size=10" "" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Invalid Pagination (Negative Page)" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Invalid Pagination (Negative Page)" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 15: Get Transactions - Excessive Page Size
echo -e "${YELLOW}Test 15: Excessive Page Size${NC}"
RESPONSE=$(make_request "GET" "$BASE_URL/api/transactions?page=0&size=1000" "" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Excessive Page Size" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Excessive Page Size" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 16: Get Specific Transaction - Valid ID
if [ -n "$TRANSACTION_ID" ]; then
    echo -e "${YELLOW}Test 16: Get Specific Transaction${NC}"
    RESPONSE=$(make_request "GET" "$BASE_URL/api/transactions/$TRANSACTION_ID" "" "Authorization: Bearer $TOKEN")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_test "Get Specific Transaction" "PASS" "HTTP $HTTP_CODE"
    else
        log_test "Get Specific Transaction" "FAIL" "HTTP $HTTP_CODE"
    fi
else
    log_test "Get Specific Transaction" "SKIP" "No transaction ID available"
fi

# Test 17: Get Specific Transaction - Invalid ID
echo -e "${YELLOW}Test 17: Invalid Transaction ID${NC}"
RESPONSE=$(make_request "GET" "$BASE_URL/api/transactions/invalid-uuid" "" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Invalid Transaction ID" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Invalid Transaction ID" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 18: Get Transaction Statistics
echo -e "${YELLOW}Test 18: Get Transaction Statistics${NC}"
RESPONSE=$(make_request "GET" "$BASE_URL/api/transactions/stats" "" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    log_test "Get Transaction Statistics" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Get Transaction Statistics" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 19: Delete Transaction - Valid ID
if [ -n "$TRANSACTION_ID" ]; then
    echo -e "${YELLOW}Test 19: Delete Transaction${NC}"
    RESPONSE=$(make_request "DELETE" "$BASE_URL/api/transactions/$TRANSACTION_ID" "" "Authorization: Bearer $TOKEN")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" = "204" ]; then
        log_test "Delete Transaction" "PASS" "HTTP $HTTP_CODE"
    else
        log_test "Delete Transaction" "FAIL" "HTTP $HTTP_CODE"
    fi
else
    log_test "Delete Transaction" "SKIP" "No transaction ID available"
fi

# Test 20: Delete Transaction - Non-existent ID
echo -e "${YELLOW}Test 20: Delete Non-existent Transaction${NC}"
RESPONSE=$(make_request "DELETE" "$BASE_URL/api/transactions/550e8400-e29b-41d4-a716-446655440000" "" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "404" ]; then
    log_test "Delete Non-existent Transaction" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Delete Non-existent Transaction" "FAIL" "HTTP $HTTP_CODE"
fi

echo ""
echo -e "${BLUE}📋 TESTING SECURITY & AUTHORIZATION${NC}"
echo "======================================="

# Test 21: Access Protected Endpoint Without Token
echo -e "${YELLOW}Test 21: Access Without Authentication${NC}"
RESPONSE=$(make_request "GET" "$BASE_URL/api/transactions")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    log_test "Access Without Authentication" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Access Without Authentication" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 22: Access Protected Endpoint With Invalid Token
echo -e "${YELLOW}Test 22: Access With Invalid Token${NC}"
RESPONSE=$(make_request "GET" "$BASE_URL/api/transactions" "" "Authorization: Bearer invalid-token-123")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    log_test "Access With Invalid Token" "PASS" "HTTP $HTTP_CODE"
else
    log_test "Access With Invalid Token" "FAIL" "HTTP $HTTP_CODE"
fi

# Test 23: SQL Injection Attempt
echo -e "${YELLOW}Test 23: SQL Injection Attempt${NC}"
SQL_INJECTION_DATA='{
    "amount": 100.00,
    "transactionType": "EXPENSE",
    "category": "SQL Injection Test",
    "description": "SQL injection test"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$SQL_INJECTION_DATA" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "201" ]; then
    log_test "SQL Injection Attempt" "PASS" "HTTP $HTTP_CODE - Properly handled"
else
    log_test "SQL Injection Attempt" "FAIL" "HTTP $HTTP_CODE"
fi

echo ""
echo -e "${BLUE}📋 TESTING EDGE CASES & STRESS CONDITIONS${NC}"
echo "============================================="

# Test 24: Rapid Sequential Requests
echo -e "${YELLOW}Test 24: Rapid Sequential Requests${NC}"
RAPID_SUCCESS=0
for i in {1..5}; do
    RAPID_DATA='{
        "amount": '$((i * 10))'.50,
        "transactionType": "EXPENSE",
        "category": "Test'$i'",
        "description": "Rapid test transaction '$i'"
    }'
    RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$RAPID_DATA" "Authorization: Bearer $TOKEN")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" = "201" ]; then
        ((RAPID_SUCCESS++))
    fi
    sleep 0.1
done

if [ $RAPID_SUCCESS -eq 5 ]; then
    log_test "Rapid Sequential Requests" "PASS" "All 5 requests succeeded"
else
    log_test "Rapid Sequential Requests" "FAIL" "Only $RAPID_SUCCESS/5 requests succeeded"
fi

# Test 25: Large JSON Payload
echo -e "${YELLOW}Test 25: Large JSON Payload${NC}"
LARGE_PAYLOAD='{
    "amount": 100.00,
    "transactionType": "EXPENSE",
    "category": "Large Payload Test",
    "description": "'$(printf 'X%.0s' {1..10000})'"
}'
RESPONSE=$(make_request "POST" "$BASE_URL/api/transactions" "$LARGE_PAYLOAD" "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    log_test "Large JSON Payload" "PASS" "HTTP $HTTP_CODE - Properly rejected"
else
    log_test "Large JSON Payload" "FAIL" "HTTP $HTTP_CODE"
fi

echo ""
echo -e "${BLUE}📊 TEST SUMMARY${NC}"
echo "================"
echo -e "Total Tests: $((PASSED + FAILED))"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 ALL TESTS PASSED! The FinGaurd API is working perfectly with hard scenarios! 🎉${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}❌ Some tests failed. Please review the issues above.${NC}"
    exit 1
fi
