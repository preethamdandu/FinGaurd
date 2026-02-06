# FinGaurd Database Schema Diagram

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                           USERS TABLE                          │
├─────────────────────────────────────────────────────────────────┤
│  id (UUID, PK)                                                  │
│  username (VARCHAR(50), UNIQUE, NOT NULL)                      │
│  email (VARCHAR(255), UNIQUE, NOT NULL)                        │
│  password_hash (VARCHAR(255), NOT NULL)                        │
│  first_name (VARCHAR(100), NOT NULL)                           │
│  last_name (VARCHAR(100), NOT NULL)                            │
│  phone (VARCHAR(20))                                           │
│  date_of_birth (DATE)                                          │
│  role (VARCHAR(20), DEFAULT 'USER')                            │
│  is_active (BOOLEAN, DEFAULT true)                             │
│  is_verified (BOOLEAN, DEFAULT false)                          │
│  email_verification_token (VARCHAR(255))                       │
│  password_reset_token (VARCHAR(255))                           │
│  password_reset_expires (TIMESTAMP)                            │
│  failed_login_attempts (INTEGER, DEFAULT 0)                    │
│  account_locked_until (TIMESTAMP)                              │
│  last_password_change (TIMESTAMP)                              │
│  created_at (TIMESTAMP)                                        │
│  updated_at (TIMESTAMP)                                        │
│  last_login (TIMESTAMP)                                        │
│  timezone (VARCHAR(50))                                        │
│  currency (VARCHAR(3))                                         │
│  language (VARCHAR(5))                                         │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │ 1:N
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       TRANSACTIONS TABLE                       │
├─────────────────────────────────────────────────────────────────┤
│  id (UUID, PK)                                                  │
│  user_id (UUID, FK → users.id)                                 │
│  amount (DECIMAL(15,2), NOT NULL)                              │
│  transaction_type (VARCHAR(20), NOT NULL)                      │
│  category (VARCHAR(50))                                        │
│  subcategory (VARCHAR(50))                                     │
│  description (TEXT)                                             │
│  merchant_name (VARCHAR(255))                                  │
│  merchant_category_code (VARCHAR(10))                          │
│  location_latitude (DECIMAL(10,8))                             │
│  location_longitude (DECIMAL(11,8))                            │
│  location_address (TEXT)                                       │
│  country (VARCHAR(2))                                          │
│  state (VARCHAR(50))                                           │
│  city (VARCHAR(100))                                           │
│  payment_method (VARCHAR(50))                                  │
│  account_number_masked (VARCHAR(20))                           │
│  bank_name (VARCHAR(100))                                      │
│  transaction_date (TIMESTAMP, NOT NULL)                        │
│  posted_date (TIMESTAMP)                                       │
│  settlement_date (TIMESTAMP)                                   │
│  is_fraud_flagged (BOOLEAN, DEFAULT false)                     │
│  fraud_risk_score (DECIMAL(5,4))                               │
│  fraud_confidence_level (VARCHAR(20))                          │
│  fraud_reason_codes (TEXT[]/VARCHAR(500))                      │
│  manual_review_required (BOOLEAN, DEFAULT false)               │
│  manual_review_status (VARCHAR(20))                            │
│  manual_review_notes (TEXT)                                    │
│  status (VARCHAR(20), DEFAULT 'COMPLETED')                     │
│  processing_fee (DECIMAL(10,2))                                │
│  exchange_rate (DECIMAL(10,6))                                 │
│  original_amount (DECIMAL(15,2))                               │
│  original_currency (VARCHAR(3))                                │
│  reference_number (VARCHAR(100))                               │
│  transaction_reference (VARCHAR(100))                          │
│  parent_transaction_id (UUID, FK → transactions.id)            │
│  recurring_transaction_id (UUID)                               │
│  created_at (TIMESTAMP)                                        │
│  updated_at (TIMESTAMP)                                        │
│  created_by (UUID, FK → users.id)                              │
│  updated_by (UUID, FK → users.id)                              │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │ N:1 (optional)
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       CATEGORIES TABLE                         │
├─────────────────────────────────────────────────────────────────┤
│  id (UUID, PK)                                                  │
│  name (VARCHAR(50), UNIQUE, NOT NULL)                          │
│  parent_category_id (UUID, FK → categories.id)                 │
│  description (TEXT)                                             │
│  icon (VARCHAR(100))                                           │
│  color (VARCHAR(7))                                            │
│  is_active (BOOLEAN, DEFAULT true)                             │
│  sort_order (INTEGER, DEFAULT 0)                               │
│  created_at (TIMESTAMP)                                        │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │ 1:N
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         ACCOUNTS TABLE                         │
├─────────────────────────────────────────────────────────────────┤
│  id (UUID, PK)                                                  │
│  user_id (UUID, FK → users.id)                                 │
│  account_name (VARCHAR(100), NOT NULL)                         │
│  account_type (VARCHAR(50), NOT NULL)                          │
│  bank_name (VARCHAR(100))                                      │
│  account_number_masked (VARCHAR(20))                           │
│  balance (DECIMAL(15,2), DEFAULT 0)                            │
│  currency (VARCHAR(3), DEFAULT 'USD')                          │
│  is_active (BOOLEAN, DEFAULT true)                             │
│  created_at (TIMESTAMP)                                        │
│  updated_at (TIMESTAMP)                                        │
└─────────────────────────────────────────────────────────────────┘
```

## Key Relationships

### 1. **Users → Transactions (1:N)**
- One user can have many transactions
- Foreign key: `transactions.user_id` → `users.id`
- Cascade delete: When user is deleted, all their transactions are deleted

### 2. **Users → Accounts (1:N)**
- One user can have many accounts
- Foreign key: `accounts.user_id` → `users.id`
- Cascade delete: When user is deleted, all their accounts are deleted

### 3. **Categories (Self-Referencing)**
- Categories can have parent categories (hierarchical structure)
- Foreign key: `categories.parent_category_id` → `categories.id`
- Self-delete: When parent category is deleted, children become orphaned

### 4. **Transactions (Self-Referencing)**
- Transactions can have parent transactions (for splits, refunds, etc.)
- Foreign key: `transactions.parent_transaction_id` → `transactions.id`
- Self-delete: When parent transaction is deleted, children become orphaned

### 5. **Audit Trail Relationships**
- `transactions.created_by` → `users.id`
- `transactions.updated_by` → `users.id`
- Track who created/modified transactions

## Transaction Types

```
INCOME ────┬─── Salary
           ├─── Freelance
           ├─── Investment
           └─── Business

EXPENSE ───┬─── Food & Dining
           ├─── Transportation
           ├─── Shopping
           ├─── Entertainment
           ├─── Bills & Utilities
           ├─── Healthcare
           ├─── Education
           └─── Travel

OTHER ─────┬─── TRANSFER (account-to-account)
           ├─── INVESTMENT (stock purchases, etc.)
           └─── LOAN (loan payments, repayments)
```

## Fraud Detection Flow

```
Transaction Input
       │
       ▼
┌─────────────────┐
│ Risk Assessment │ ◄─── ML Models
└─────────────────┘
       │
       ▼
┌─────────────────┐
│ Risk Score      │
│ (0.0000-1.0000) │
└─────────────────┘
       │
       ▼
┌─────────────────┐
│ Confidence      │
│ Level           │
│ (LOW/MED/HIGH/  │
│  CRITICAL)      │
└─────────────────┘
       │
       ▼
┌─────────────────┐
│ Manual Review   │ ◄─── Flag if needed
│ Required?       │
└─────────────────┘
       │
       ▼
┌─────────────────┐
│ Status:         │
│ • PENDING       │
│ • APPROVED      │
│ • REJECTED      │
│ • ESCALATED     │
└─────────────────┘
```

## Index Strategy

### Primary Indexes
```
users: idx_users_email, idx_users_username
transactions: idx_transactions_user_id, idx_transactions_date
categories: idx_categories_parent, idx_categories_active
accounts: idx_accounts_user_id, idx_accounts_type
```

### Performance Indexes
```
users: idx_users_active (partial), idx_users_role
transactions: idx_transactions_fraud (partial), idx_transactions_user_date
```

### Composite Indexes
```
transactions: idx_transactions_user_type, idx_transactions_status
```

## Data Flow

```
User Registration ──► Users Table
       │
       ▼
User Login ─────────► Authentication
       │
       ▼
Transaction Input ──► Transactions Table
       │
       ▼
Fraud Detection ────► Risk Assessment
       │
       ▼
Manual Review ──────► Approval/Rejection
       │
       ▼
Transaction Complete
```

This schema design provides a comprehensive foundation for financial tracking with advanced fraud detection capabilities, ensuring scalability, security, and maintainability.

