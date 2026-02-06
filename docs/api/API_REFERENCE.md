# FinGaurd API Reference

Base URL: `http://localhost:8080` (local) or your deployed ALB DNS.

All authenticated endpoints require the header:
```
Authorization: Bearer <JWT_TOKEN>
```

---

## Authentication

### POST /api/auth/signup
Register a new user.

**Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "isActive": true,
  "createdAt": "2025-10-09T10:30:00"
}
```

### POST /api/auth/login
Authenticate and receive a JWT token.

**Request:**
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### POST /api/auth/logout
Logout (stateless - handled client-side by discarding token).

**Response:** `204 No Content`

---

## User Profile

### GET /api/users/me
Get current user's profile. Requires authentication.

**Response (200):**
```json
{
  "id": "uuid",
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "fullName": "John Doe",
  "role": "USER",
  "isActive": true,
  "createdAt": "2025-10-09T10:30:00",
  "lastLogin": "2025-10-10T08:00:00"
}
```

### PUT /api/users/me
Update current user's profile. All fields are optional.

**Request:**
```json
{
  "firstName": "John",
  "lastName": "Smith",
  "username": "johnsmith"
}
```

**Response (200):** Same as GET /api/users/me

---

## Transactions

### POST /api/transactions
Create a new transaction. Automatically analyzed for fraud.

**Request:**
```json
{
  "amount": 150.00,
  "transactionType": "EXPENSE",
  "category": "groceries",
  "description": "Weekly shopping at Whole Foods",
  "transactionDate": "2025-10-09T14:30:00"
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "amount": 150.00,
  "transactionType": "EXPENSE",
  "category": "groceries",
  "description": "Weekly shopping at Whole Foods",
  "transactionDate": "2025-10-09T14:30:00",
  "createdAt": "2025-10-09T14:30:05",
  "isFraudFlagged": false,
  "fraudRiskScore": 0.05
}
```

### GET /api/transactions
List transactions with pagination and filtering.

**Query Parameters:**

| Parameter     | Type     | Default          | Description                      |
|---------------|----------|------------------|----------------------------------|
| page          | int      | 0                | Page number (0-based)            |
| size          | int      | 20               | Page size (1-100)                |
| sortBy        | string   | transactionDate  | Sort field                       |
| sortDir       | string   | desc             | Sort direction (asc/desc)        |
| type          | string   | —                | INCOME or EXPENSE                |
| category      | string   | —                | Category name                    |
| startDate     | datetime | —                | ISO date-time lower bound        |
| endDate       | datetime | —                | ISO date-time upper bound        |
| fraudFlagged  | boolean  | —                | Filter by fraud flag             |

**Response (200):** Spring Page of TransactionResponse

### GET /api/transactions/{id}
Get a single transaction by ID.

### PUT /api/transactions/{id}
Update a transaction. All fields are optional.

**Request:**
```json
{
  "amount": 155.50,
  "category": "groceries",
  "description": "Updated description"
}
```

### DELETE /api/transactions/{id}
Delete a transaction.

**Response:** `204 No Content`

---

## Statistics & Analytics

### GET /api/transactions/stats
Account balance and summary statistics.

**Query Parameters:** `startDate`, `endDate` (optional, defaults to last 30 days)

**Response (200):**
```json
{
  "totalIncome": 5000.00,
  "totalExpenses": 2149.50,
  "currentBalance": 2850.50,
  "totalTransactions": 45,
  "incomeTransactions": 12,
  "expenseTransactions": 33,
  "fraudTransactions": 2,
  "averageIncome": 416.67,
  "averageExpense": 65.14,
  "periodStart": "2025-09-09T00:00:00",
  "periodEnd": "2025-10-09T23:59:59"
}
```

### GET /api/transactions/stats/by-category
Spending broken down by category.

**Query Parameters:** `startDate`, `endDate` (optional)

**Response (200):**
```json
{
  "groceries": {
    "incomeCount": 0,
    "incomeTotal": 0,
    "expenseCount": 8,
    "expenseTotal": 600.00
  },
  "dining": {
    "incomeCount": 0,
    "incomeTotal": 0,
    "expenseCount": 15,
    "expenseTotal": 450.00
  }
}
```

### GET /api/transactions/summary
Quick summary with top categories.

**Query Parameters:** `days` (default: 30, max: 365)

### GET /api/transactions/fraud
Fraud-flagged transactions.

**Query Parameters:** `page`, `size`

---

## Fraud Detection Service

Base URL: `http://localhost:8000`

### POST /api/fraud/analyze
Analyze a single transaction.

### POST /api/fraud/batch
Analyze multiple transactions.

### GET /api/fraud/models
Get current ML model info.

### POST /api/fraud/train
Trigger model retraining.

### GET /api/health
Health check.

### GET /api/ready
Readiness check (includes MongoDB & ML status).

### POST /detect
Legacy endpoint (used internally by the Java service).

---

## Error Responses

All errors follow a consistent format:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "timestamp": "2025-10-09T14:30:00",
  "details": {
    "email": "Email must be valid"
  }
}
```

| Status | Meaning                              |
|--------|--------------------------------------|
| 400    | Bad request / validation error       |
| 401    | Unauthorized (missing/invalid token) |
| 404    | Resource not found                   |
| 409    | Conflict (duplicate email/username)  |
| 500    | Internal server error                |
