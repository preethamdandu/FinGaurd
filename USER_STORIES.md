# FinGaurd User Stories

This document contains detailed user stories for the FinGaurd MVP. Each story follows the format: "As a [user type], I want [goal] so that [benefit]."

## 🎭 User Personas

### Primary Persona: Individual User (Emma)
- **Age**: 28
- **Occupation**: Software Engineer
- **Tech Savviness**: High
- **Goals**: Track expenses, prevent fraud, manage budget
- **Pain Points**: Manual transaction tracking, undetected fraudulent charges

---

## 🔐 Epic 1: Authentication & Authorization

### US-001: User Registration
**As a** new user  
**I want to** create an account with my email and password  
**So that** I can securely access the FinGaurd platform

**Acceptance Criteria:**
- [ ] User can navigate to registration page
- [ ] Email must be unique and valid format
- [ ] Password must meet security requirements (min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char)
- [ ] Password is hashed before storage (BCrypt)
- [ ] Confirmation email is sent (future enhancement)
- [ ] User account is created in the database
- [ ] Appropriate error messages for validation failures
- [ ] Success message displayed upon registration

**API Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "email": "emma@example.com",
  "password": "SecurePass123!",
  "firstName": "Emma",
  "lastName": "Johnson"
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "email": "emma@example.com",
  "firstName": "Emma",
  "lastName": "Johnson",
  "createdAt": "2025-10-09T10:30:00Z"
}
```

**Priority:** P0 (Critical)  
**Story Points:** 5

---

### US-002: User Login
**As a** registered user  
**I want to** log in with my email and password  
**So that** I can access my financial data securely

**Acceptance Criteria:**
- [ ] User can navigate to login page
- [ ] System validates email and password
- [ ] JWT token is generated upon successful authentication
- [ ] Token includes user ID and expiration time
- [ ] Last login timestamp is updated in database
- [ ] Invalid credentials return appropriate error
- [ ] Account lockout after 5 failed attempts (future enhancement)

**API Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "emma@example.com",
  "password": "SecurePass123!"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "email": "emma@example.com",
    "firstName": "Emma",
    "lastName": "Johnson"
  }
}
```

**Priority:** P0 (Critical)  
**Story Points:** 5

---

### US-003: User Logout
**As a** logged-in user  
**I want to** log out of my account  
**So that** my session is terminated and data remains secure

**Acceptance Criteria:**
- [ ] User can click logout button
- [ ] JWT token is invalidated on client side
- [ ] User is redirected to login page
- [ ] Subsequent requests with old token are rejected
- [ ] Logout event is logged in audit trail

**API Endpoint:** `POST /api/auth/logout`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "message": "Logout successful"
}
```

**Priority:** P1 (High)  
**Story Points:** 2

---

## 👤 Epic 2: User Profile Management

### US-004: View Profile
**As a** logged-in user  
**I want to** view my profile information  
**So that** I can see my account details

**Acceptance Criteria:**
- [ ] User can navigate to profile page
- [ ] Displays email, name, account creation date
- [ ] Displays last login timestamp
- [ ] Requires valid JWT token
- [ ] Returns 401 for unauthenticated requests

**API Endpoint:** `GET /api/users/me`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "email": "emma@example.com",
  "firstName": "Emma",
  "lastName": "Johnson",
  "createdAt": "2025-09-01T10:00:00Z",
  "lastLogin": "2025-10-09T10:30:00Z",
  "isActive": true
}
```

**Priority:** P2 (Medium)  
**Story Points:** 2

---

### US-005: Update Profile
**As a** logged-in user  
**I want to** update my profile information  
**So that** I can keep my account details current

**Acceptance Criteria:**
- [ ] User can update first name and last name
- [ ] Email cannot be changed (requires separate flow)
- [ ] Changes are validated before saving
- [ ] Updated timestamp is recorded
- [ ] Success confirmation is displayed
- [ ] Requires authentication

**API Endpoint:** `PUT /api/users/me`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "firstName": "Emma",
  "lastName": "Smith"
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "email": "emma@example.com",
  "firstName": "Emma",
  "lastName": "Smith",
  "updatedAt": "2025-10-09T11:00:00Z"
}
```

**Priority:** P2 (Medium)  
**Story Points:** 3

---

## 💰 Epic 3: Transaction Management

### US-006: Create Transaction
**As a** logged-in user  
**I want to** record a new transaction (income or expense)  
**So that** I can track my financial activities

**Acceptance Criteria:**
- [ ] User can enter amount, type, category, description, and date
- [ ] Amount must be positive number with 2 decimal places
- [ ] Type must be either "INCOME" or "EXPENSE"
- [ ] Category is selected from predefined list
- [ ] Description is optional (max 500 characters)
- [ ] Transaction date defaults to current date/time
- [ ] Transaction is automatically sent to fraud detection service
- [ ] Fraud risk score is calculated and stored
- [ ] User receives immediate feedback on fraud detection
- [ ] Transaction is saved to database
- [ ] User's balance is updated accordingly

**API Endpoint:** `POST /api/transactions`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "amount": 150.00,
  "transactionType": "EXPENSE",
  "category": "groceries",
  "description": "Weekly grocery shopping at Whole Foods",
  "transactionDate": "2025-10-09T14:30:00Z"
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "userId": "uuid",
  "amount": 150.00,
  "transactionType": "EXPENSE",
  "category": "groceries",
  "description": "Weekly grocery shopping at Whole Foods",
  "transactionDate": "2025-10-09T14:30:00Z",
  "createdAt": "2025-10-09T14:30:05Z",
  "isFraudFlagged": false,
  "fraudRiskScore": 0.15
}
```

**Categories:**
- groceries
- dining
- transportation
- utilities
- entertainment
- healthcare
- shopping
- salary
- freelance
- investment
- other

**Priority:** P0 (Critical)  
**Story Points:** 8

---

### US-007: View Transaction History
**As a** logged-in user  
**I want to** view my transaction history  
**So that** I can see all my past financial activities

**Acceptance Criteria:**
- [ ] User can view paginated list of transactions
- [ ] Transactions are sorted by date (newest first)
- [ ] Each transaction shows all key details
- [ ] Pagination controls are available
- [ ] Default page size is 20 transactions
- [ ] User can only see their own transactions
- [ ] Fraud-flagged transactions are visually highlighted

**API Endpoint:** `GET /api/transactions?page=0&size=20&sort=transactionDate,desc`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "amount": 150.00,
      "transactionType": "EXPENSE",
      "category": "groceries",
      "description": "Weekly grocery shopping",
      "transactionDate": "2025-10-09T14:30:00Z",
      "isFraudFlagged": false,
      "fraudRiskScore": 0.15
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```

**Priority:** P0 (Critical)  
**Story Points:** 5

---

### US-008: Filter Transactions
**As a** logged-in user  
**I want to** filter transactions by date range, category, or type  
**So that** I can analyze specific financial activities

**Acceptance Criteria:**
- [ ] User can filter by date range (start and end date)
- [ ] User can filter by category
- [ ] User can filter by transaction type (INCOME/EXPENSE)
- [ ] Multiple filters can be applied simultaneously
- [ ] Filters can be cleared to show all transactions
- [ ] Filtered results are paginated
- [ ] Results maintain sort order

**API Endpoint:** `GET /api/transactions?startDate=2025-10-01&endDate=2025-10-09&category=groceries&type=EXPENSE`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK` (same structure as US-007)

**Priority:** P1 (High)  
**Story Points:** 5

---

### US-009: View Transaction Details
**As a** logged-in user  
**I want to** view detailed information for a specific transaction  
**So that** I can see all transaction data including fraud analysis

**Acceptance Criteria:**
- [ ] User can click on transaction to view details
- [ ] Shows all transaction fields
- [ ] Shows fraud detection analysis details
- [ ] Shows transaction timestamp
- [ ] User can only view their own transactions
- [ ] Returns 404 if transaction not found
- [ ] Returns 403 if transaction belongs to another user

**API Endpoint:** `GET /api/transactions/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "userId": "uuid",
  "amount": 150.00,
  "transactionType": "EXPENSE",
  "category": "groceries",
  "description": "Weekly grocery shopping at Whole Foods",
  "transactionDate": "2025-10-09T14:30:00Z",
  "createdAt": "2025-10-09T14:30:05Z",
  "updatedAt": "2025-10-09T14:30:05Z",
  "isFraudFlagged": false,
  "fraudRiskScore": 0.15,
  "fraudAnalysis": {
    "analyzedAt": "2025-10-09T14:30:06Z",
    "detectedAnomalies": [],
    "modelVersion": "1.0.0"
  }
}
```

**Priority:** P2 (Medium)  
**Story Points:** 3

---

### US-010: Update Transaction
**As a** logged-in user  
**I want to** update a transaction  
**So that** I can correct errors or add missing information

**Acceptance Criteria:**
- [ ] User can edit amount, category, description
- [ ] Transaction type cannot be changed (requires delete/recreate)
- [ ] Transaction date can be updated
- [ ] Updated transaction is re-analyzed for fraud
- [ ] Updated timestamp is recorded
- [ ] User can only update their own transactions
- [ ] Original transaction data is logged for audit

**API Endpoint:** `PUT /api/transactions/{id}`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "amount": 155.50,
  "category": "groceries",
  "description": "Weekly grocery shopping at Whole Foods (updated)"
}
```

**Response:** `200 OK` (same structure as US-009)

**Priority:** P2 (Medium)  
**Story Points:** 5

---

### US-011: Delete Transaction
**As a** logged-in user  
**I want to** delete a transaction  
**So that** I can remove incorrect or duplicate entries

**Acceptance Criteria:**
- [ ] User can delete a transaction
- [ ] Confirmation prompt is shown
- [ ] Transaction is soft-deleted (marked inactive)
- [ ] User's balance is recalculated
- [ ] User can only delete their own transactions
- [ ] Deletion is logged in audit trail
- [ ] Deleted transaction cannot be retrieved via API

**API Endpoint:** `DELETE /api/transactions/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `204 No Content`

**Priority:** P2 (Medium)  
**Story Points:** 3

---

### US-012: View Account Balance
**As a** logged-in user  
**I want to** see my current account balance  
**So that** I know my financial status

**Acceptance Criteria:**
- [ ] Balance is calculated as: (Total Income - Total Expenses)
- [ ] Balance is displayed on dashboard
- [ ] Balance updates in real-time after transactions
- [ ] Shows separate totals for income and expenses
- [ ] Can filter balance by date range
- [ ] Only includes non-flagged transactions (or show both)

**API Endpoint:** `GET /api/transactions/stats`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "currentBalance": 2850.50,
  "totalIncome": 5000.00,
  "totalExpenses": 2149.50,
  "transactionCount": 45,
  "period": {
    "start": "2025-09-01T00:00:00Z",
    "end": "2025-10-09T23:59:59Z"
  }
}
```

**Priority:** P0 (Critical)  
**Story Points:** 5

---

## 🚨 Epic 4: Fraud Detection

### US-013: Automatic Fraud Detection
**As a** user  
**I want** the system to automatically detect suspicious transactions  
**So that** I can be alerted to potential fraud

**Acceptance Criteria:**
- [ ] Every new transaction is automatically analyzed
- [ ] Analysis happens in near real-time (< 2 seconds)
- [ ] ML model calculates risk score (0.0 to 1.0)
- [ ] Transactions with risk score > 0.7 are flagged
- [ ] Flagged transactions are marked in database
- [ ] Analysis results are stored in MongoDB
- [ ] User is notified if transaction is flagged
- [ ] System continues to process transaction even if fraud service is down

**Internal API:** Java Service → Python Fraud Service

**Fraud Detection Criteria:**
- Unusually large amount compared to user's history
- High transaction frequency in short time
- Unusual category for user
- Transaction at unusual time
- Geographic anomaly (future enhancement)

**Priority:** P0 (Critical)  
**Story Points:** 13

---

### US-014: View Fraud Alerts
**As a** logged-in user  
**I want to** see all transactions flagged as potentially fraudulent  
**So that** I can review and take action

**Acceptance Criteria:**
- [ ] User can view list of flagged transactions
- [ ] Each alert shows transaction details and risk score
- [ ] Alerts are sorted by risk score (highest first)
- [ ] User can mark alert as "reviewed"
- [ ] User can confirm transaction as legitimate
- [ ] User can confirm transaction as fraud and delete
- [ ] Filter options: all, reviewed, unreviewed

**API Endpoint:** `GET /api/transactions?fraudFlagged=true`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "amount": 2500.00,
      "transactionType": "EXPENSE",
      "category": "shopping",
      "transactionDate": "2025-10-09T03:00:00Z",
      "isFraudFlagged": true,
      "fraudRiskScore": 0.85,
      "fraudAnalysis": {
        "detectedAnomalies": [
          "Unusually large amount",
          "Transaction at unusual time"
        ]
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 3,
    "totalPages": 1
  }
}
```

**Priority:** P1 (High)  
**Story Points:** 5

---

## 📊 Epic 5: Reporting & Analytics

### US-015: View Spending by Category
**As a** logged-in user  
**I want to** view my spending broken down by category  
**So that** I can understand where my money goes

**Acceptance Criteria:**
- [ ] Shows expenses grouped by category
- [ ] Displays total amount and percentage for each category
- [ ] Can filter by date range
- [ ] Shows top 5 categories prominently
- [ ] Includes visualization (chart/graph - future frontend)
- [ ] Only includes EXPENSE transactions
- [ ] Excludes fraud-flagged transactions

**API Endpoint:** `GET /api/transactions/stats/by-category?startDate=2025-10-01&endDate=2025-10-31`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "period": {
    "start": "2025-10-01T00:00:00Z",
    "end": "2025-10-31T23:59:59Z"
  },
  "totalExpenses": 2149.50,
  "categories": [
    {
      "category": "groceries",
      "total": 600.00,
      "percentage": 27.91,
      "transactionCount": 8
    },
    {
      "category": "dining",
      "total": 450.00,
      "percentage": 20.94,
      "transactionCount": 15
    },
    {
      "category": "transportation",
      "total": 300.00,
      "percentage": 13.96,
      "transactionCount": 12
    }
  ]
}
```

**Priority:** P2 (Medium)  
**Story Points:** 8

---

## 📝 Epic 6: Audit & Logging

### US-016: Audit Trail
**As a** system administrator  
**I want** all user actions to be logged  
**So that** we can track activity for security and compliance

**Acceptance Criteria:**
- [ ] All API requests are logged
- [ ] Logs include: user ID, action, timestamp, IP address
- [ ] Logs are stored in MongoDB
- [ ] Failed authentication attempts are logged
- [ ] Sensitive data (passwords) is not logged
- [ ] Logs are retained for 90 days
- [ ] Logs can be queried by user, date, or action type

**Internal Logging - No User-Facing API**

**MongoDB Collection: audit_logs**
```json
{
  "_id": ObjectId,
  "userId": "uuid",
  "action": "CREATE_TRANSACTION",
  "resource": "/api/transactions",
  "method": "POST",
  "statusCode": 201,
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "timestamp": ISODate("2025-10-09T14:30:00Z"),
  "metadata": {
    "transactionId": "uuid",
    "amount": 150.00
  }
}
```

**Priority:** P1 (High)  
**Story Points:** 8

---

## 🎯 Story Priority Summary

### P0 - Critical (Must Have for MVP)
- US-001: User Registration
- US-002: User Login
- US-006: Create Transaction
- US-007: View Transaction History
- US-012: View Account Balance
- US-013: Automatic Fraud Detection

### P1 - High (Should Have)
- US-003: User Logout
- US-008: Filter Transactions
- US-014: View Fraud Alerts
- US-016: Audit Trail

### P2 - Medium (Nice to Have)
- US-004: View Profile
- US-005: Update Profile
- US-009: View Transaction Details
- US-010: Update Transaction
- US-011: Delete Transaction
- US-015: View Spending by Category

---

**Total Story Points:** 98  
**Estimated Sprints:** 4-5 (2-week sprints)  
**MVP Story Points:** 46 (P0 stories only)

**Document Version:** 1.0  
**Last Updated:** Phase 0 - Foundation  
**Status:** User Stories Defined ✅

