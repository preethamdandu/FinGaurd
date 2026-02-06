#!/bin/bash

# Debug Authentication Test Script
BASE_URL="http://localhost:8080"

echo "🔍 Debugging Authentication Issues"
echo "=================================="

# Test 1: User Registration
echo "1. Testing user registration..."
USER_DATA='{
    "username": "debuguser",
    "email": "debug@fingaurd.com",
    "password": "SecurePass123!",
    "firstName": "Debug",
    "lastName": "User"
}'

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d "$USER_DATA")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Registration Response Code: $HTTP_CODE"
echo "Registration Response Body: $BODY"

# Test 2: User Login
echo ""
echo "2. Testing user login..."
LOGIN_DATA='{
    "email": "debug@fingaurd.com",
    "password": "SecurePass123!"
}'

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_DATA")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Login Response Code: $HTTP_CODE"
echo "Login Response Body: $BODY"

# Extract token
TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
echo "Extracted Token: ${TOKEN:0:50}..."

if [ -n "$TOKEN" ]; then
    echo ""
    echo "3. Testing authenticated request..."
    
    # Test transaction creation with token
    TRANSACTION_DATA='{
        "amount": 100.00,
        "transactionType": "EXPENSE",
        "category": "Test",
        "description": "Debug test transaction"
    }'
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/transactions" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "$TRANSACTION_DATA")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    echo "Transaction Creation Response Code: $HTTP_CODE"
    echo "Transaction Creation Response Body: $BODY"
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo "✅ Authentication working correctly!"
    else
        echo "❌ Authentication failed"
    fi
else
    echo "❌ No token received from login"
fi
