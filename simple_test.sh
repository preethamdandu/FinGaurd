#!/bin/bash

BASE_URL="http://localhost:8080"

echo "🔍 Simple Authentication Test"
echo "============================="

# Test 1: User Registration
echo "1. User Registration..."
TIMESTAMP=$(date +%s)
USER_DATA='{
    "username": "testuser'$TIMESTAMP'",
    "email": "test'$TIMESTAMP'@fingaurd.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
}'

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d "$USER_DATA")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Registration Code: $HTTP_CODE"
if [ "$HTTP_CODE" = "201" ]; then
    echo "✅ Registration successful"
    
    # Test 2: User Login
    echo ""
    echo "2. User Login..."
    LOGIN_DATA='{
        "email": "test'$TIMESTAMP'@fingaurd.com",
        "password": "SecurePass123!"
    }'
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "$LOGIN_DATA")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    echo "Login Code: $HTTP_CODE"
    echo "Login Response: $BODY"
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ Login successful"
        
        # Extract token
        TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        echo "Token: ${TOKEN:0:50}..."
        
        if [ -n "$TOKEN" ]; then
            # Test 3: Create Transaction
            echo ""
            echo "3. Create Transaction..."
            TRANSACTION_DATA='{
                "amount": 100.00,
                "transactionType": "EXPENSE",
                "category": "Test",
                "description": "Test transaction"
            }'
            
            RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/transactions" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $TOKEN" \
                -d "$TRANSACTION_DATA")
            
            HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
            BODY=$(echo "$RESPONSE" | sed '$d')
            
            echo "Transaction Code: $HTTP_CODE"
            if [ "$HTTP_CODE" = "201" ]; then
                echo "✅ Transaction created successfully"
                echo "🎉 All tests passed!"
            else
                echo "❌ Transaction creation failed: $BODY"
            fi
        else
            echo "❌ No token received"
        fi
    else
        echo "❌ Login failed: $BODY"
    fi
else
    echo "❌ Registration failed: $BODY"
fi

