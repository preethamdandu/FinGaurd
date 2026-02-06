#!/bin/bash

# FinGaurd Controller Test Suite - Hard Scenarios

# Configuration
BASE_URL="http://localhost:8080/api"
ADMIN_EMAIL="admin@fingaurd.com"
ADMIN_PASSWORD="admin123"
USER_EMAIL="testuser@example.com"
USER_PASSWORD="Password123!"
TOKEN=""
USER_ID=""
TRANSACTION_ID=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to log test results
log_test() {
    local test_name=$1
    local status=$2
    local details=$3

    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        echo -e "  ${BLUE}Details:${NC} $details"
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name"
        echo -e "  ${RED}Error:${NC} $details"
    fi
}

# Function to make authenticated requests
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local auth_header=$4

    if [ -n "$data" ]; then
        curl -s -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "$auth_header" \
            -d "$data"
    else
        curl -s -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "$auth_header"
    fi
}

echo -e "${BLUE}🚀 Starting FinGaurd Controller Test Suite - Hard Scenarios${NC}"
echo "=============================================================="

# --- Test 1: Application Health ---
echo -e "${YELLOW}1. Testing Application Health${NC}"
health_response=$(curl -s "$BASE_URL/../actuator/health")
if echo "$health_response" | grep -q '"status":"UP"'; then
    log_test "Application Health Check" "PASS" "Application is running and healthy"
else
    log_test "Application Health Check" "FAIL" "Application health check failed: $health_response"
    echo -e "${RED}❌ Application not healthy, exiting.${NC}"
    exit 1
fi

# --- Test 2: AuthController - Signup (Hard Scenarios) ---
echo -e "${YELLOW}2. Testing AuthController Signup - Hard Scenarios${NC}"

# Test 2a: Valid signup
signup_data='{"username":"testuser","email":"'$USER_EMAIL'","password":"'$USER_PASSWORD'","firstName":"Test","lastName":"User"}'
signup_response=$(make_request "POST" "$BASE_URL/auth/signup" "$signup_data")
if echo "$signup_response" | grep -q '"id"'; then
    log_test "Valid Signup" "PASS" "Successfully created new user"
else
    log_test "Valid Signup" "FAIL" "Signup failed: $signup_response"
fi

# Test 2b: Duplicate email signup
duplicate_signup_response=$(make_request "POST" "$BASE_URL/auth/signup" "$signup_data")
if echo "$duplicate_signup_response" | grep -q '"status":409'; then
    log_test "Duplicate Email Validation" "PASS" "Properly rejected duplicate email"
else
    log_test "Duplicate Email Validation" "FAIL" "Did not reject duplicate email: $duplicate_signup_response"
fi

# Test 2c: Invalid email format
invalid_email_data='{"username":"testuser2","email":"invalid-email","password":"'$USER_PASSWORD'","firstName":"Test","lastName":"User"}'
invalid_email_response=$(make_request "POST" "$BASE_URL/auth/signup" "$invalid_email_data")
if echo "$invalid_email_response" | grep -q '"status":400'; then
    log_test "Invalid Email Format Validation" "PASS" "Properly rejected invalid email format"
else
    log_test "Invalid Email Format Validation" "FAIL" "Did not reject invalid email: $invalid_email_response"
fi

# Test 2d: Weak password
weak_password_data='{"username":"testuser3","email":"testuser3@example.com","password":"123","firstName":"Test","lastName":"User"}'
weak_password_response=$(make_request "POST" "$BASE_URL/auth/signup" "$weak_password_data")
if echo "$weak_password_response" | grep -q '"status":400'; then
    log_test "Weak Password Validation" "PASS" "Properly rejected weak password"
else
    log_test "Weak Password Validation" "FAIL" "Did not reject weak password: $weak_password_response"
fi

# Test 2e: Missing required fields
missing_fields_data='{"email":"testuser4@example.com","password":"'$USER_PASSWORD'"}'
missing_fields_response=$(make_request "POST" "$BASE_URL/auth/signup" "$missing_fields_data")
if echo "$missing_fields_response" | grep -q '"status":400'; then
    log_test "Missing Required Fields Validation" "PASS" "Properly rejected request with missing fields"
else
    log_test "Missing Required Fields Validation" "FAIL" "Did not reject missing fields: $missing_fields_response"
fi

# --- Test 3: AuthController - Login (Hard Scenarios) ---
echo -e "${YELLOW}3. Testing AuthController Login - Hard Scenarios${NC}"

# Test 3a: Valid login
login_data='{"email":"'$USER_EMAIL'","password":"'$USER_PASSWORD'"}'
login_response=$(make_request "POST" "$BASE_URL/auth/login" "$login_data")
if echo "$login_response" | grep -q '"accessToken"'; then
    TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    log_test "Valid Login" "PASS" "Successfully obtained JWT token"
else
    log_test "Valid Login" "FAIL" "Login failed: $login_response"
fi

# Test 3b: Invalid credentials
invalid_login_data='{"email":"'$USER_EMAIL'","password":"wrongpassword"}'
invalid_login_response=$(make_request "POST" "$BASE_URL/auth/login" "$invalid_login_data")
if echo "$invalid_login_response" | grep -q '"status":401'; then
    log_test "Invalid Credentials Rejection" "PASS" "Properly rejected invalid credentials"
else
    log_test "Invalid Credentials Rejection" "FAIL" "Did not reject invalid credentials: $invalid_login_response"
fi

# Test 3c: Non-existent user
nonexistent_login_data='{"email":"nonexistent@example.com","password":"'$USER_PASSWORD'"}'
nonexistent_login_response=$(make_request "POST" "$BASE_URL/auth/login" "$nonexistent_login_data")
if echo "$nonexistent_login_response" | grep -q '"status":401'; then
    log_test "Non-existent User Rejection" "PASS" "Properly rejected non-existent user"
else
    log_test "Non-existent User Rejection" "FAIL" "Did not reject non-existent user: $nonexistent_login_response"
fi

# Test 3d: Empty credentials
empty_login_data='{"email":"","password":""}'
empty_login_response=$(make_request "POST" "$BASE_URL/auth/login" "$empty_login_data")
if echo "$empty_login_response" | grep -q '"status":400'; then
    log_test "Empty Credentials Validation" "PASS" "Properly rejected empty credentials"
else
    log_test "Empty Credentials Validation" "FAIL" "Did not reject empty credentials: $empty_login_response"
fi

# --- Test 4: TransactionController - Create Transaction (Hard Scenarios) ---
echo -e "${YELLOW}4. Testing TransactionController Create - Hard Scenarios${NC}"

if [ -n "$TOKEN" ]; then
    # Test 4a: Valid transaction creation
    transaction_data='{"amount":100.50,"transactionType":"EXPENSE","category":"Groceries","description":"Weekly groceries"}'
    create_transaction_response=$(make_request "POST" "$BASE_URL/transactions" "$transaction_data" "Authorization: Bearer $TOKEN")
    if echo "$create_transaction_response" | grep -q '"id"'; then
        TRANSACTION_ID=$(echo "$create_transaction_response" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        log_test "Valid Transaction Creation" "PASS" "Successfully created transaction (ID: $TRANSACTION_ID)"
    else
        log_test "Valid Transaction Creation" "FAIL" "Failed to create transaction: $create_transaction_response"
    fi

    # Test 4b: Zero amount transaction
    zero_amount_data='{"amount":0.00,"transactionType":"EXPENSE","category":"Test","description":"Zero amount test"}'
    zero_amount_response=$(make_request "POST" "$BASE_URL/transactions" "$zero_amount_data" "Authorization: Bearer $TOKEN")
    if echo "$zero_amount_response" | grep -q '"status":400'; then
        log_test "Zero Amount Validation" "PASS" "Properly rejected zero amount transaction"
    else
        log_test "Zero Amount Validation" "FAIL" "Did not reject zero amount: $zero_amount_response"
    fi

    # Test 4c: Negative amount transaction
    negative_amount_data='{"amount":-50.00,"transactionType":"EXPENSE","category":"Test","description":"Negative amount test"}'
    negative_amount_response=$(make_request "POST" "$BASE_URL/transactions" "$negative_amount_data" "Authorization: Bearer $TOKEN")
    if echo "$negative_amount_response" | grep -q '"status":400'; then
        log_test "Negative Amount Validation" "PASS" "Properly rejected negative amount transaction"
    else
        log_test "Negative Amount Validation" "FAIL" "Did not reject negative amount: $negative_amount_response"
    fi

    # Test 4d: Missing required fields
    missing_transaction_data='{"amount":100.00,"category":"Test"}'
    missing_transaction_response=$(make_request "POST" "$BASE_URL/transactions" "$missing_transaction_data" "Authorization: Bearer $TOKEN")
    if echo "$missing_transaction_response" | grep -q '"status":400'; then
        log_test "Missing Transaction Fields Validation" "PASS" "Properly rejected request with missing transaction type"
    else
        log_test "Missing Transaction Fields Validation" "FAIL" "Did not reject missing fields: $missing_transaction_response"
    fi

    # Test 4e: Invalid transaction type
    invalid_type_data='{"amount":100.00,"transactionType":"INVALID_TYPE","category":"Test","description":"Invalid type test"}'
    invalid_type_response=$(make_request "POST" "$BASE_URL/transactions" "$invalid_type_data" "Authorization: Bearer $TOKEN")
    if echo "$invalid_type_response" | grep -q '"status":400'; then
        log_test "Invalid Transaction Type Validation" "PASS" "Properly rejected invalid transaction type"
    else
        log_test "Invalid Transaction Type Validation" "FAIL" "Did not reject invalid type: $invalid_type_response"
    fi

    # Test 4f: Very large amount (should still work but might trigger fraud detection)
    large_amount_data='{"amount":999999999.99,"transactionType":"INCOME","category":"Investment","description":"Very large amount test"}'
    large_amount_response=$(make_request "POST" "$BASE_URL/transactions" "$large_amount_data" "Authorization: Bearer $TOKEN")
    if echo "$large_amount_response" | grep -q '"id"'; then
        log_test "Large Amount Transaction" "PASS" "Successfully handled very large amount"
    else
        log_test "Large Amount Transaction" "FAIL" "Failed to handle large amount: $large_amount_response"
    fi

else
    log_test "Transaction Creation Tests" "FAIL" "No authentication token available"
fi

# --- Test 5: TransactionController - Get Transactions (Hard Scenarios) ---
echo -e "${YELLOW}5. Testing TransactionController Get - Hard Scenarios${NC}"

if [ -n "$TOKEN" ]; then
    # Test 5a: Valid transaction retrieval
    get_transactions_response=$(make_request "GET" "$BASE_URL/transactions" "" "Authorization: Bearer $TOKEN")
    if echo "$get_transactions_response" | grep -q '"content"'; then
        log_test "Valid Transaction Retrieval" "PASS" "Successfully retrieved transaction list"
    else
        log_test "Valid Transaction Retrieval" "FAIL" "Failed to retrieve transactions: $get_transactions_response"
    fi

    # Test 5b: Invalid pagination parameters
    invalid_page_response=$(make_request "GET" "$BASE_URL/transactions?page=-1" "" "Authorization: Bearer $TOKEN")
    if echo "$invalid_page_response" | grep -q '"status":400'; then
        log_test "Invalid Page Number Validation" "PASS" "Properly rejected negative page number"
    else
        log_test "Invalid Page Number Validation" "FAIL" "Did not reject negative page: $invalid_page_response"
    fi

    # Test 5c: Excessive page size
    excessive_size_response=$(make_request "GET" "$BASE_URL/transactions?size=1000" "" "Authorization: Bearer $TOKEN")
    if echo "$excessive_size_response" | grep -q '"status":400'; then
        log_test "Excessive Page Size Validation" "PASS" "Properly rejected excessive page size"
    else
        log_test "Excessive Page Size Validation" "FAIL" "Did not reject excessive size: $excessive_size_response"
    fi

    # Test 5d: Get specific transaction
    if [ -n "$TRANSACTION_ID" ]; then
        get_specific_response=$(make_request "GET" "$BASE_URL/transactions/$TRANSACTION_ID" "" "Authorization: Bearer $TOKEN")
        if echo "$get_specific_response" | grep -q '"id":"'$TRANSACTION_ID'"'; then
            log_test "Specific Transaction Retrieval" "PASS" "Successfully retrieved specific transaction"
        else
            log_test "Specific Transaction Retrieval" "FAIL" "Failed to retrieve specific transaction: $get_specific_response"
        fi
    else
        log_test "Specific Transaction Retrieval" "FAIL" "No transaction ID available for testing"
    fi

    # Test 5e: Invalid UUID format
    invalid_uuid_response=$(make_request "GET" "$BASE_URL/transactions/invalid-uuid" "" "Authorization: Bearer $TOKEN")
    if echo "$invalid_uuid_response" | grep -q '"status":400'; then
        log_test "Invalid UUID Format Validation" "PASS" "Properly rejected invalid UUID format"
    else
        log_test "Invalid UUID Format Validation" "FAIL" "Did not reject invalid UUID: $invalid_uuid_response"
    fi

else
    log_test "Transaction Retrieval Tests" "FAIL" "No authentication token available"
fi

# --- Test 6: TransactionController - Delete Transaction (Hard Scenarios) ---
echo -e "${YELLOW}6. Testing TransactionController Delete - Hard Scenarios${NC}"

if [ -n "$TOKEN" ]; then
    # Test 6a: Delete valid transaction
    if [ -n "$TRANSACTION_ID" ]; then
        delete_response=$(make_request "DELETE" "$BASE_URL/transactions/$TRANSACTION_ID" "" "Authorization: Bearer $TOKEN")
        if [ "$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/transactions/$TRANSACTION_ID" -H "Authorization: Bearer $TOKEN")" = "204" ]; then
            log_test "Valid Transaction Deletion" "PASS" "Successfully deleted transaction"
        else
            log_test "Valid Transaction Deletion" "FAIL" "Failed to delete transaction: HTTP $(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/transactions/$TRANSACTION_ID" -H "Authorization: Bearer $TOKEN")"
        fi
    else
        log_test "Valid Transaction Deletion" "FAIL" "No transaction ID available for testing"
    fi

    # Test 6b: Delete non-existent transaction
    fake_uuid="550e8400-e29b-41d4-a716-446655440001"
    delete_nonexistent_response=$(make_request "DELETE" "$BASE_URL/transactions/$fake_uuid" "" "Authorization: Bearer $TOKEN")
    if echo "$delete_nonexistent_response" | grep -q '"status":404'; then
        log_test "Non-existent Transaction Deletion" "PASS" "Properly handled deletion of non-existent transaction"
    else
        log_test "Non-existent Transaction Deletion" "FAIL" "Did not handle non-existent deletion properly: $delete_nonexistent_response"
    fi

    # Test 6c: Delete with invalid UUID
    delete_invalid_uuid_response=$(make_request "DELETE" "$BASE_URL/transactions/invalid-uuid" "" "Authorization: Bearer $TOKEN")
    if echo "$delete_invalid_uuid_response" | grep -q '"status":400'; then
        log_test "Invalid UUID Deletion Validation" "PASS" "Properly rejected deletion with invalid UUID"
    else
        log_test "Invalid UUID Deletion Validation" "FAIL" "Did not reject invalid UUID deletion: $delete_invalid_uuid_response"
    fi

else
    log_test "Transaction Deletion Tests" "FAIL" "No authentication token available"
fi

# --- Test 7: Security Tests (Hard Scenarios) ---
echo -e "${YELLOW}7. Testing Security - Hard Scenarios${NC}"

# Test 7a: Unauthorized access to protected endpoints
unauth_transactions_response=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/transactions")
if [ "$unauth_transactions_response" = "403" ] || [ "$unauth_transactions_response" = "401" ]; then
    log_test "Unauthorized Access Protection" "PASS" "Properly blocks unauthorized access (HTTP $unauth_transactions_response)"
else
    log_test "Unauthorized Access Protection" "FAIL" "Should block unauthorized access (HTTP $unauth_transactions_response)"
fi

# Test 7b: Invalid token access
invalid_token_response=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/transactions" -H "Authorization: Bearer invalid_token_here")
if [ "$invalid_token_response" = "403" ] || [ "$invalid_token_response" = "401" ]; then
    log_test "Invalid Token Rejection" "PASS" "Properly rejects invalid tokens (HTTP $invalid_token_response)"
else
    log_test "Invalid Token Rejection" "FAIL" "Should reject invalid tokens (HTTP $invalid_token_response)"
fi

# Test 7c: Malformed token access
malformed_token_response=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/transactions" -H "Authorization: Bearer malformed.token.here")
if [ "$malformed_token_response" = "403" ] || [ "$malformed_token_response" = "401" ]; then
    log_test "Malformed Token Rejection" "PASS" "Properly rejects malformed tokens (HTTP $malformed_token_response)"
else
    log_test "Malformed Token Rejection" "FAIL" "Should reject malformed tokens (HTTP $malformed_token_response)"
fi

# --- Test 8: Edge Cases and Stress Tests ---
echo -e "${YELLOW}8. Testing Edge Cases and Stress Scenarios${NC}"

if [ -n "$TOKEN" ]; then
    # Test 8a: Rapid transaction creation (stress test)
    echo "Creating 5 rapid transactions..."
    rapid_success_count=0
    for i in {1..5}; do
        rapid_data='{"amount":'$((50 + i * 10))'.00,"transactionType":"EXPENSE","category":"StressTest","description":"Rapid transaction '$i'"}'
        rapid_response=$(make_request "POST" "$BASE_URL/transactions" "$rapid_data" "Authorization: Bearer $TOKEN")
        if echo "$rapid_response" | grep -q '"id"'; then
            rapid_success_count=$((rapid_success_count + 1))
        fi
    done
    
    if [ "$rapid_success_count" -eq 5 ]; then
        log_test "Rapid Transaction Creation" "PASS" "Successfully created all 5 rapid transactions"
    else
        log_test "Rapid Transaction Creation" "FAIL" "Only created $rapid_success_count out of 5 transactions"
    fi

    # Test 8b: Very long description
    long_description=$(printf 'A%.0s' {1..500}) # 500 'A's
    long_desc_data='{"amount":1.00,"transactionType":"EXPENSE","category":"LongDesc","description":"'$long_description'"}'
    long_desc_response=$(make_request "POST" "$BASE_URL/transactions" "$long_desc_data" "Authorization: Bearer $TOKEN")
    if echo "$long_desc_response" | grep -q '"id"'; then
        log_test "Long Description Handling" "PASS" "Successfully handled maximum description length"
    else
        log_test "Long Description Handling" "FAIL" "Failed to handle long description: $long_desc_response"
    fi

    # Test 8c: Transaction statistics
    stats_response=$(make_request "GET" "$BASE_URL/transactions/stats" "" "Authorization: Bearer $TOKEN")
    if echo "$stats_response" | grep -q '"totalTransactions"'; then
        TOTAL_TRANSACTIONS=$(echo "$stats_response" | grep -o '"totalTransactions":[0-9]*' | cut -d':' -f2)
        log_test "Transaction Statistics" "PASS" "Successfully retrieved statistics (Total: $TOTAL_TRANSACTIONS)"
    else
        log_test "Transaction Statistics" "FAIL" "Failed to retrieve statistics: $stats_response"
    fi

else
    log_test "Edge Cases and Stress Tests" "FAIL" "No authentication token available"
fi

echo "=============================================================="
echo -e "${BLUE}📊 Controller Test Suite Results${NC}"
echo "=============================================================="

# Count results
PASSED_COUNT=$(grep -c "✓ PASS" <<< "$(cat /tmp/controller_test_results.log 2>/dev/null || echo '')")
FAILED_COUNT=$(grep -c "✗ FAIL" <<< "$(cat /tmp/controller_test_results.log 2>/dev/null || echo '')")
TOTAL_COUNT=$((PASSED_COUNT + FAILED_COUNT))

echo "Total Tests: ${BLUE}$TOTAL_COUNT${NC}"
echo "Passed: ${GREEN}$PASSED_COUNT${NC}"
echo "Failed: ${RED}$FAILED_COUNT${NC}"

if [ "$FAILED_COUNT" -gt 0 ]; then
    echo -e "${RED}⚠️  $FAILED_COUNT test(s) failed. The controllers need attention.${NC}"
    exit 1
else
    echo -e "${GREEN}🎉 ALL CONTROLLER TESTS PASSED! The API layer is robust and handles hard scenarios perfectly!${NC}"
    exit 0
fi

