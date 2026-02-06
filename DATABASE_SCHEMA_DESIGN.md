# FinGaurd Database Schema Design

## Overview

This document describes the comprehensive database schema design for the FinGaurd financial tracking and fraud detection system. The schema is designed to support both PostgreSQL (production) and H2 (development) databases.

## Design Principles

### 1. **Scalability**
- UUID primary keys for distributed systems compatibility
- Comprehensive indexing strategy for optimal query performance
- Partitioning-ready design for future growth

### 2. **Data Integrity**
- Comprehensive CHECK constraints for data validation
- Foreign key relationships with appropriate cascade rules
- NOT NULL constraints where data is required

### 3. **Security**
- Password hash storage with BCrypt minimum length validation
- Email format validation with regex constraints
- Account lockout mechanisms for failed login attempts

### 4. **Audit Trail**
- Automatic timestamp management with triggers
- Created/updated by tracking for accountability
- Comprehensive logging of all changes

### 5. **Extensibility**
- Flexible transaction categorization system
- Support for multiple currencies and locations
- Fraud detection framework ready for ML integration

## Table Structure

### 1. Users Table

**Purpose**: Store user account information and authentication data

**Key Features**:
- **Authentication**: Username, email, password hash with BCrypt validation
- **Profile**: Name, phone, date of birth with age validation (13+ years)
- **Security**: Email verification, password reset tokens, failed login tracking
- **Preferences**: Timezone, currency, language settings
- **Roles**: USER, ADMIN, MODERATOR with proper constraints

**Important Constraints**:
```sql
-- Email format validation
email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'

-- Username format (alphanumeric + underscore, 3-50 chars)
username ~* '^[a-zA-Z0-9_]{3,50}$'

-- Password hash minimum length (BCrypt requirement)
length(password_hash) >= 60

-- Age validation (minimum 13 years)
date_of_birth <= CURRENT_DATE - INTERVAL '13 years'
```

### 2. Transactions Table

**Purpose**: Store financial transaction records with comprehensive fraud detection capabilities

**Key Features**:
- **Transaction Details**: Amount, type, category, description, merchant info
- **Location Data**: GPS coordinates, address, country/state/city
- **Payment Info**: Payment method, account details, bank information
- **Fraud Detection**: Risk scores, confidence levels, reason codes, manual review flags
- **Status Tracking**: Pending, completed, failed, cancelled, refunded
- **Multi-Currency**: Support for original amount and currency with exchange rates

**Transaction Types**:
- `INCOME`: Salary, freelance, investment returns
- `EXPENSE`: Purchases, bills, services
- `TRANSFER`: Account-to-account transfers
- `INVESTMENT`: Stock purchases, bonds, crypto
- `LOAN`: Loan payments, repayments

**Fraud Detection Fields**:
```sql
is_fraud_flagged BOOLEAN DEFAULT false,
fraud_risk_score DECIMAL(5, 4), -- 0.0000 to 1.0000
fraud_confidence_level VARCHAR(20), -- LOW, MEDIUM, HIGH, CRITICAL
fraud_reason_codes TEXT[], -- Array of reason codes
manual_review_required BOOLEAN DEFAULT false,
manual_review_status VARCHAR(20), -- PENDING, APPROVED, REJECTED, ESCALATED
```

### 3. Categories Table

**Purpose**: Standardized transaction categorization with hierarchical support

**Key Features**:
- **Hierarchical**: Parent-child category relationships
- **Visual**: Icon and color support for UI
- **Flexible**: Active/inactive status, sort ordering
- **Extensible**: Easy to add new categories

**Default Categories**:
- **Expenses**: Food & Dining, Transportation, Shopping, Entertainment, Bills & Utilities, Healthcare, Education, Travel
- **Income**: Salary, Freelance, Investment, Business
- **Other**: Miscellaneous transactions

### 4. Accounts Table (Future Enhancement)

**Purpose**: Store user's financial accounts for comprehensive tracking

**Key Features**:
- **Account Types**: Checking, Savings, Credit Card, Investment, Loan, Other
- **Security**: Masked account numbers
- **Multi-Currency**: Balance tracking in different currencies
- **Status**: Active/inactive account management

## Indexing Strategy

### Performance Indexes

**Users Table**:
```sql
-- Authentication lookups
idx_users_email, idx_users_username

-- Active user filtering
idx_users_active (partial index on is_active = true)

-- Role-based queries
idx_users_role

-- Audit queries
idx_users_created_at, idx_users_last_login

-- Security tokens
idx_users_email_verification, idx_users_password_reset
```

**Transactions Table**:
```sql
-- User-based queries
idx_transactions_user_id, idx_transactions_user_date, idx_transactions_user_type

-- Time-based queries
idx_transactions_date, idx_transactions_created_at

-- Categorization
idx_transactions_category, idx_transactions_type

-- Fraud detection
idx_transactions_fraud (partial index), idx_transactions_risk_score

-- Merchant and location
idx_transactions_merchant, idx_transactions_location

-- Status and processing
idx_transactions_status, idx_transactions_reference
```

## Security Features

### 1. **Password Security**
- BCrypt hashing with minimum strength validation
- Password reset token system with expiration
- Failed login attempt tracking with account lockout

### 2. **Data Validation**
- Email format validation with regex
- Phone number format validation
- Coordinate validation for GPS data
- Currency code validation (3-letter ISO codes)

### 3. **Access Control**
- Role-based permissions (USER, ADMIN, MODERATOR)
- Account activation and verification system
- Audit trail for all data modifications

## Fraud Detection Framework

### 1. **Risk Assessment**
- **Risk Score**: 0.0000 to 1.0000 decimal precision
- **Confidence Level**: LOW, MEDIUM, HIGH, CRITICAL
- **Reason Codes**: Array of specific fraud indicators

### 2. **Manual Review Process**
- Automatic flagging for high-risk transactions
- Manual review workflow with status tracking
- Escalation procedures for critical cases

### 3. **Extensible Design**
- Ready for machine learning model integration
- Support for multiple fraud detection algorithms
- Historical data for model training

## Performance Optimizations

### 1. **Partial Indexes**
```sql
-- Only index active users
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = true;

-- Only index fraud-flagged transactions
CREATE INDEX idx_transactions_fraud ON transactions(is_fraud_flagged) 
WHERE is_fraud_flagged = true;
```

### 2. **Composite Indexes**
```sql
-- User + date queries (common pattern)
CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date);

-- User + type queries
CREATE INDEX idx_transactions_user_type ON transactions(user_id, transaction_type);
```

### 3. **Covering Indexes**
- Indexes designed to cover common query patterns
- Reduced I/O for frequently accessed data

## Views and Reporting

### 1. **User Transaction Summary**
```sql
-- Aggregated user statistics
SELECT user_id, username, email,
       COUNT(*) as total_transactions,
       SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END) as total_income,
       SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END) as total_expenses,
       COUNT(CASE WHEN is_fraud_flagged = true THEN 1 END) as fraud_flagged_count
FROM users u LEFT JOIN transactions t ON u.id = t.user_id
WHERE u.is_active = true
GROUP BY u.id, u.username, u.email;
```

### 2. **Monthly Transaction Trends**
```sql
-- Time-series analysis for trends
SELECT user_id, DATE_TRUNC('month', transaction_date) as month,
       transaction_type, COUNT(*), SUM(amount), AVG(amount)
FROM transactions
WHERE status = 'COMPLETED'
GROUP BY user_id, DATE_TRUNC('month', transaction_date), transaction_type;
```

## Database Compatibility

### PostgreSQL (Production)
- Full feature support including arrays, regex, extensions
- Advanced indexing with partial indexes
- Comprehensive trigger system
- Full-text search capabilities

### H2 (Development)
- Simplified constraints for compatibility
- String-based arrays instead of native arrays
- Basic trigger support
- Compatible with Spring Boot testing

## Migration Strategy

### 1. **Version Control**
- Schema files tracked in version control
- Migration scripts for production deployments
- Rollback procedures documented

### 2. **Testing**
- H2 schema for unit testing
- PostgreSQL schema for integration testing
- Performance testing with realistic data volumes

### 3. **Deployment**
- Automated schema validation
- Data integrity checks
- Performance baseline establishment

## Future Enhancements

### 1. **Advanced Features**
- Multi-tenant support
- Real-time notifications
- Advanced analytics and reporting
- Machine learning model integration

### 2. **Performance**
- Table partitioning for large datasets
- Read replicas for reporting
- Caching strategies
- Query optimization

### 3. **Security**
- Row-level security (RLS)
- Encryption at rest
- Advanced audit logging
- Compliance reporting

## Maintenance

### 1. **Regular Tasks**
- Index maintenance and optimization
- Statistics updates
- Vacuum and analyze operations
- Performance monitoring

### 2. **Monitoring**
- Query performance tracking
- Index usage analysis
- Storage utilization monitoring
- Alert systems for anomalies

This schema design provides a solid foundation for a scalable, secure, and maintainable financial tracking system with comprehensive fraud detection capabilities.

