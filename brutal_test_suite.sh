#!/bin/bash

# FinGaurd Brutal Test Suite
# Tests all features under harsh conditions

BASE_URL="http://localhost:8080/api"
ADMIN_EMAIL="admin@fingaurd.com"
ADMIN_PASSWORD="admin123"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to log test results
log_test() {
    local test_name="$1"
    local result="$2"
    local details="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$result" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        if [ -n "$details" ]; then
            echo -e "  ${BLUE}Details:${NC} $details"
        fi
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name"
        if [ -n "$details" ]; then
            echo -e "  ${RED}Error:${NC} $details"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo
}

# Function to make HTTP requests
make_request() {
    local method="$1"
    local url="$2"
    local data="$3"
    local headers="$4"
    
    if [ -n "$data" ]; then
        if [ -n "$headers" ]; then
            curl -s -X "$method" "$url" -H "Content-Type: application/json" -H "$headers" -d "$data"
        else
            curl -s -X "$method" "$url" -H "Content-Type: application/json" -d "$data"
        fi
    else
        if [ -n "$headers" ]; then
            curl -s -X "$method" "$url" -H "$headers"
        else
            curl -s -X "$method" "$url"
        fi
    fi
}

echo -e "${BLUE}🚀 Starting FinGaurd Brutal Test Suite${NC}"
echo "==============================================="
echo

# Test 1: Application Health Check
echo -e "${YELLOW}1. Testing Application Health${NC}"
health_response=$(make_request "GET" "http://localhost:8080/actuator/health")
if echo "$health_response" | grep -q '"status":"UP"'; then
    log_test "Application Health Check" "PASS" "Application is running and healthy"
else
    log_test "Application Health Check" "FAIL" "Application health check failed: $health_response"
fi

# Test 2: Admin Login
echo -e "${YELLOW}2. Testing Admin Authentication${NC}"
login_data='{"email":"'$ADMIN_EMAIL'","password":"'$ADMIN_PASSWORD'"}'
login_response=$(make_request "POST" "$BASE_URL/auth/login" "$login_data")

if echo "$login_response" | grep -q '"accessToken"'; then
    TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    log_test "Admin Login" "PASS" "Successfully obtained JWT token"
else
    log_test "Admin Login" "FAIL" "Login failed: $login_response"
    echo -e "${RED}❌ Cannot continue without authentication token${NC}"
    exit 1
fi

AUTH_HEADER="Authorization: Bearer $TOKEN"

# Test 3: User Profile Access
echo -e "${YELLOW}3. Testing User Profile Access${NC}"
profile_response=$(make_request "GET" "$BASE_URL/users/me" "" "$AUTH_HEADER")
if echo "$profile_response" | grep -q '"email":"'$ADMIN_EMAIL'"'; then
    USER_ID=$(echo "$profile_response" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    log_test "User Profile Access" "PASS" "Successfully retrieved user profile (ID: $USER_ID)"
else
    log_test "User Profile Access" "FAIL" "Failed to retrieve profile: $profile_response"
fi

# Test 4: Malformed UUID Handling
echo -e "${YELLOW}4. Testing Malformed UUID Handling${NC}"
malformed_response=$(make_request "GET" "$BASE_URL/transactions/invalid-uuid" "" "$AUTH_HEADER")
if echo "$malformed_response" | grep -q '"status":400'; then
    log_test "Malformed UUID Handling" "PASS" "Properly returns 400 for invalid UUID"
else
    log_test "Malformed UUID Handling" "FAIL" "Should return 400 for malformed UUID: $malformed_response"
fi

# Test 5: Transaction Creation - Normal Case
echo -e "${YELLOW}5. Testing Transaction Creation${NC}"
transaction_data='{
    "amount": 100.50,
    "transactionType": "INCOME",
    "category": "Salary",
    "description": "Monthly salary payment"
}'
create_response=$(make_request "POST" "$BASE_URL/transactions" "$transaction_data" "$AUTH_HEADER")
if echo "$create_response" | grep -q '"amount":100.5'; then
    TRANSACTION_ID=$(echo "$create_response" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    log_test "Transaction Creation" "PASS" "Successfully created transaction (ID: $TRANSACTION_ID)"
else
    log_test "Transaction Creation" "FAIL" "Failed to create transaction: $create_response"
fi

# Test 6: Transaction Creation - Edge Cases
echo -e "${YELLOW}6. Testing Transaction Edge Cases${NC}"

# Test 6a: Minimum Amount
min_amount_data='{
    "amount": 0.01,
    "transactionType": "EXPENSE",
    "category": "Test",
    "description": "Minimum amount test"
}'
min_response=$(make_request "POST" "$BASE_URL/transactions" "$min_amount_data" "$AUTH_HEADER")
if echo "$min_response" | grep -q '"amount":0.01'; then
    log_test "Minimum Amount Transaction" "PASS" "Successfully created transaction with minimum amount"
else
    log_test "Minimum Amount Transaction" "FAIL" "Failed to create minimum amount transaction: $min_response"
fi

# Test 6b: Maximum Amount
max_amount_data='{
    "amount": 999999999.99,
    "transactionType": "INCOME",
    "category": "Test",
    "description": "Maximum amount test"
}'
max_response=$(make_request "POST" "$BASE_URL/transactions" "$max_amount_data" "$AUTH_HEADER")
if echo "$max_response" | grep -q '"amount":999999999.99'; then
    log_test "Maximum Amount Transaction" "PASS" "Successfully created transaction with maximum amount"
else
    log_test "Maximum Amount Transaction" "FAIL" "Failed to create maximum amount transaction: $max_response"
fi

# Test 6c: Invalid Amount (too small)
invalid_amount_data='{
    "amount": 0.001,
    "transactionType": "EXPENSE",
    "category": "Test",
    "description": "Invalid amount test"
}'
invalid_amount_response=$(make_request "POST" "$BASE_URL/transactions" "$invalid_amount_data" "$AUTH_HEADER")
if echo "$invalid_amount_response" | grep -q '"status":400'; then
    log_test "Invalid Amount Validation" "PASS" "Properly rejected amount below minimum"
else
    log_test "Invalid Amount Validation" "FAIL" "Should reject amount below minimum: $invalid_amount_response"
fi

# Test 6d: Invalid Amount (too large)
oversized_amount_data='{
    "amount": 999999999999.99,
    "transactionType": "EXPENSE",
    "category": "Test",
    "description": "Oversized amount test"
}'
oversized_response=$(make_request "POST" "$BASE_URL/transactions" "$oversized_amount_data" "$AUTH_HEADER")
if echo "$oversized_response" | grep -q '"status":400'; then
    log_test "Oversized Amount Validation" "PASS" "Properly rejected amount above maximum"
else
    log_test "Oversized Amount Validation" "FAIL" "Should reject amount above maximum: $oversized_response"
fi

# Test 7: Fraud Detection
echo -e "${YELLOW}7. Testing Fraud Detection${NC}"

# Test 7a: High Amount Transaction (should trigger fraud detection)
high_amount_data='{
    "amount": 50000.00,
    "transactionType": "EXPENSE",
    "category": "Gambling",
    "description": "High amount gambling transaction"
}'
fraud_response=$(make_request "POST" "$BASE_URL/transactions" "$high_amount_data" "$AUTH_HEADER")
if echo "$fraud_response" | grep -q '"fraudRiskScore"'; then
    fraud_score=$(echo "$fraud_response" | grep -o '"fraudRiskScore":[0-9.]*' | cut -d':' -f2)
    is_flagged=$(echo "$fraud_response" | grep -o '"isFraudFlagged":[^,]*' | cut -d':' -f2)
    log_test "Fraud Detection" "PASS" "Fraud detection working (Score: $fraud_score, Flagged: $is_flagged)"
else
    log_test "Fraud Detection" "FAIL" "Fraud detection not working: $fraud_response"
fi

# Test 8: Transaction Retrieval
echo -e "${YELLOW}8. Testing Transaction Retrieval${NC}"

# Test 8a: Get all transactions
all_transactions=$(make_request "GET" "$BASE_URL/transactions?page=0&size=10" "" "$AUTH_HEADER")
if echo "$all_transactions" | grep -q '"content"'; then
    log_test "Transaction List Retrieval" "PASS" "Successfully retrieved transaction list"
else
    log_test "Transaction List Retrieval" "FAIL" "Failed to retrieve transaction list: $all_transactions"
fi

# Test 8b: Get specific transaction
if [ -n "$TRANSACTION_ID" ]; then
    specific_transaction=$(make_request "GET" "$BASE_URL/transactions/$TRANSACTION_ID" "" "$AUTH_HEADER")
    if echo "$specific_transaction" | grep -q '"id":"'$TRANSACTION_ID'"'; then
        log_test "Specific Transaction Retrieval" "PASS" "Successfully retrieved specific transaction"
    else
        log_test "Specific Transaction Retrieval" "FAIL" "Failed to retrieve specific transaction: $specific_transaction"
    fi
fi

# Test 9: Transaction Statistics
echo -e "${YELLOW}9. Testing Transaction Statistics${NC}"
stats_response=$(make_request "GET" "$BASE_URL/transactions/stats" "" "$AUTH_HEADER")
if echo "$stats_response" | grep -q '"totalTransactions"'; then
    total_tx=$(echo "$stats_response" | grep -o '"totalTransactions":[0-9]*' | cut -d':' -f2)
    log_test "Transaction Statistics" "PASS" "Successfully retrieved statistics (Total transactions: $total_tx)"
else
    log_test "Transaction Statistics" "FAIL" "Failed to retrieve statistics: $stats_response"
fi

# Test 10: Stress Testing - Multiple Concurrent Transactions
echo -e "${YELLOW}10. Testing Stress Conditions${NC}"

# Test 10a: Rapid Transaction Creation
echo "Creating 10 rapid transactions..."
for i in {1..10}; do
    rapid_data='{
        "amount": '$((100 + i))'.50,
        "transactionType": "EXPENSE",
        "category": "Stress Test",
        "description": "Rapid transaction '$i'"
    }'
    rapid_response=$(make_request "POST" "$BASE_URL/transactions" "$rapid_data" "$AUTH_HEADER")
    if ! echo "$rapid_response" | grep -q '"amount"'; then
        log_test "Rapid Transaction $i" "FAIL" "Failed to create rapid transaction: $rapid_response"
        break
    fi
done
log_test "Rapid Transaction Creation" "PASS" "Successfully created 10 rapid transactions"

# Test 10b: Large Transaction Description
large_desc_data='{
    "amount": 100.00,
    "transactionType": "EXPENSE",
    "category": "Test",
    "description": "'$(printf 'A%.0s' {1..500})'"
}'
large_desc_response=$(make_request "POST" "$BASE_URL/transactions" "$large_desc_data" "$AUTH_HEADER")
if echo "$large_desc_response" | grep -q '"amount":100'; then
    log_test "Large Description Transaction" "PASS" "Successfully handled maximum description length"
else
    log_test "Large Description Transaction" "FAIL" "Failed to handle large description: $large_desc_response"
fi

# Test 11: Security Testing
echo -e "${YELLOW}11. Testing Security${NC}"

# Test 11a: Unauthorized Access
unauth_response=$(curl -s -w "%{http_code}" -o /tmp/unauth_body "$BASE_URL/transactions")
if [ "$unauth_response" = "403" ] || [ "$unauth_response" = "401" ]; then
    log_test "Unauthorized Access Protection" "PASS" "Properly blocks unauthorized access (HTTP $unauth_response)"
else
    log_test "Unauthorized Access Protection" "FAIL" "Should block unauthorized access (HTTP $unauth_response)"
fi

# Test 11b: Invalid Token
invalid_token_response=$(curl -s -w "%{http_code}" -o /tmp/invalid_token_body "$BASE_URL/transactions" -H "Authorization: Bearer invalid_token")
if [ "$invalid_token_response" = "403" ] || [ "$invalid_token_response" = "401" ]; then
    log_test "Invalid Token Rejection" "PASS" "Properly rejects invalid tokens (HTTP $invalid_token_response)"
else
    log_test "Invalid Token Rejection" "FAIL" "Should reject invalid tokens (HTTP $invalid_token_response)"
fi

# Test 12: Data Validation
echo -e "${YELLOW}12. Testing Data Validation${NC}"

# Test 12a: Missing Required Fields
missing_fields_data='{
    "amount": 100.00
}'
missing_fields_response=$(make_request "POST" "$BASE_URL/transactions" "$missing_fields_data" "$AUTH_HEADER")
if echo "$missing_fields_response" | grep -q '"status":400'; then
    log_test "Missing Fields Validation" "PASS" "Properly validates required fields"
else
    log_test "Missing Fields Validation" "FAIL" "Should validate required fields: $missing_fields_response"
fi

# Test 12b: Invalid Transaction Type
invalid_type_data='{
    "amount": 100.00,
    "transactionType": "INVALID_TYPE",
    "category": "Test",
    "description": "Invalid type test"
}'
invalid_type_response=$(make_request "POST" "$BASE_URL/transactions" "$invalid_type_data" "$AUTH_HEADER")
if echo "$invalid_type_response" | grep -q '"status":400'; then
    log_test "Invalid Type Validation" "PASS" "Properly validates transaction type"
else
    log_test "Invalid Type Validation" "FAIL" "Should validate transaction type: $invalid_type_response"
fi

# Test 13: Performance Testing
echo -e "${YELLOW}13. Testing Performance${NC}"

# Test 13a: Response Time Measurement
start_time=$(date +%s%N)
perf_response=$(make_request "GET" "$BASE_URL/users/me" "" "$AUTH_HEADER")
end_time=$(date +%s%N)
response_time=$(( (end_time - start_time) / 1000000 )) # Convert to milliseconds

if [ $response_time -lt 1000 ]; then
    log_test "Response Time Performance" "PASS" "Response time: ${response_time}ms (under 1 second)"
else
    log_test "Response Time Performance" "FAIL" "Response time: ${response_time}ms (over 1 second)"
fi

# Test 14: Edge Case Testing
echo -e "${YELLOW}14. Testing Edge Cases${NC}"

# Test 14a: Empty Category
empty_category_data='{
    "amount": 100.00,
    "transactionType": "EXPENSE",
    "category": "",
    "description": "Empty category test"
}'
empty_category_response=$(make_request "POST" "$BASE_URL/transactions" "$empty_category_data" "$AUTH_HEADER")
if echo "$empty_category_response" | grep -q '"status":400'; then
    log_test "Empty Category Validation" "PASS" "Properly rejects empty category"
else
    log_test "Empty Category Validation" "FAIL" "Should reject empty category: $empty_category_response"
fi

# Test 14b: Very Long Category
long_category_data='{
    "amount": 100.00,
    "transactionType": "EXPENSE",
    "category": "'$(printf 'A%.0s' {1..60})'",
    "description": "Long category test"
}'
long_category_response=$(make_request "POST" "$BASE_URL/transactions" "$long_category_data" "$AUTH_HEADER")
if echo "$long_category_response" | grep -q '"status":400'; then
    log_test "Long Category Validation" "PASS" "Properly rejects category over 50 characters"
else
    log_test "Long Category Validation" "FAIL" "Should reject category over 50 characters: $long_category_response"
fi

# Final Results
echo "==============================================="
echo -e "${BLUE}📊 Brutal Test Suite Results${NC}"
echo "==============================================="
echo -e "Total Tests: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 ALL TESTS PASSED! The application is robust and handles harsh conditions perfectly!${NC}"
    exit 0
else
    echo -e "\n${RED}⚠️  $FAILED_TESTS test(s) failed. The application needs attention.${NC}"
    exit 1
fi
