# FinGaurd Architecture Documentation

## 🏛️ System Overview

FinGaurd implements a cloud-native microservices architecture designed for scalability, maintainability, and security. The system is composed of two primary services communicating through REST APIs, backed by polyglot persistence.

## 🎨 High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         Client Layer                              │
│  (Postman for MVP, Future: React/Angular Web Application)        │
└────────────────────────────────┬─────────────────────────────────┘
                                 │ HTTPS
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                      AWS API Gateway                              │
│  - Request routing                                                │
│  - Rate limiting                                                  │
│  - Request/Response transformation                                │
│  - API key management                                             │
└────────────────────────────────┬─────────────────────────────────┘
                                 │ HTTP/REST
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│              Java Core Service (Spring Boot)                      │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  Controllers Layer                                      │     │
│  │  - UserController                                       │     │
│  │  - TransactionController                                │     │
│  │  - AuthController                                       │     │
│  └────────────────────────────────────────────────────────┘     │
│                             │                                     │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  Service Layer                                          │     │
│  │  - UserService                                          │     │
│  │  - TransactionService                                   │     │
│  │  - AuthService                                          │     │
│  │  - FraudClientService (HTTP Client)                     │     │
│  └────────────────────────────────────────────────────────┘     │
│                             │                                     │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  Security Layer                                         │     │
│  │  - JWT Authentication Filter                            │     │
│  │  - Password Encoder (BCrypt)                            │     │
│  │  - Authorization Rules                                  │     │
│  └────────────────────────────────────────────────────────┘     │
│                             │                                     │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  Repository Layer (Spring Data JPA)                     │     │
│  │  - UserRepository                                       │     │
│  │  - TransactionRepository                                │     │
│  └────────────────────────────────────────────────────────┘     │
└─────────┬──────────────────────────────────────┬─────────────────┘
          │                                      │ HTTP/REST
          │ JDBC                                 │
          ▼                                      ▼
┌─────────────────────┐          ┌──────────────────────────────────┐
│   PostgreSQL DB     │          │  Python Fraud Detection Service  │
│  ┌───────────────┐  │          │  ┌────────────────────────────┐ │
│  │  Users Table  │  │          │  │  FastAPI Application       │ │
│  │  - id         │  │          │  │  - Anomaly Detection API   │ │
│  │  - email      │  │          │  │  - Model Training API      │ │
│  │  - password   │  │          │  └────────────────────────────┘ │
│  │  - name       │  │          │                                  │
│  │  - created_at │  │          │  ┌────────────────────────────┐ │
│  └───────────────┘  │          │  │  ML Engine                 │ │
│                     │          │  │  - Isolation Forest        │ │
│  ┌───────────────┐  │          │  │  - Statistical Analysis    │ │
│  │ Transactions  │  │          │  │  - Feature Engineering     │ │
│  │  - id         │  │          │  └────────────────────────────┘ │
│  │  - user_id    │  │          │                                  │
│  │  - amount     │  │          │  ┌────────────────────────────┐ │
│  │  - type       │  │          │  │  Logging Service           │ │
│  │  - category   │  │          │  │  - MongoDB Client          │ │
│  │  - timestamp  │  │          │  └────────────────────────────┘ │
│  │  - description│  │          └──────────────┬───────────────────┘
│  │  - fraud_flag │  │                         │ MongoDB Driver
│  └───────────────┘  │                         ▼
└─────────────────────┘          ┌──────────────────────────────────┐
                                 │       MongoDB                     │
                                 │  ┌────────────────────────────┐  │
                                 │  │  fraud_logs Collection     │  │
                                 │  │  - transaction_id          │  │
                                 │  │  - risk_score              │  │
                                 │  │  - detected_anomalies      │  │
                                 │  │  - timestamp               │  │
                                 │  └────────────────────────────┘  │
                                 │                                  │
                                 │  ┌────────────────────────────┐  │
                                 │  │  audit_logs Collection     │  │
                                 │  │  - user_id                 │  │
                                 │  │  - action                  │  │
                                 │  │  - timestamp               │  │
                                 │  │  - metadata                │  │
                                 │  └────────────────────────────┘  │
                                 └──────────────────────────────────┘
```

## 🔧 Component Details

### 1. Java Core Service (Spring Boot)

**Responsibilities:**
- User authentication and authorization
- Transaction CRUD operations
- Business logic orchestration
- API endpoint exposure
- Integration with fraud detection service

**Technology Stack:**
- Spring Boot 3.x
- Spring Security (JWT authentication)
- Spring Data JPA
- PostgreSQL Driver
- Maven/Gradle
- Lombok
- MapStruct (DTO mapping)

**Key Endpoints:**
```
POST   /api/auth/register      - User registration
POST   /api/auth/login         - User login
POST   /api/auth/logout        - User logout

GET    /api/users/me           - Get current user profile
PUT    /api/users/me           - Update user profile

POST   /api/transactions       - Create new transaction
GET    /api/transactions       - Get all transactions (paginated)
GET    /api/transactions/{id}  - Get transaction by ID
PUT    /api/transactions/{id}  - Update transaction
DELETE /api/transactions/{id}  - Delete transaction
GET    /api/transactions/stats - Get transaction statistics
```

**Database Schema (PostgreSQL):**
```sql
-- Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Transactions Table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- 'INCOME' or 'EXPENSE'
    category VARCHAR(50),
    description TEXT,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_fraud_flagged BOOLEAN DEFAULT false,
    fraud_risk_score DECIMAL(5, 4),
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_category (category)
);
```

### 2. Python Fraud Detection Service (FastAPI)

**Responsibilities:**
- Analyze transactions for anomalies
- Calculate fraud risk scores
- Train and update ML models
- Log fraud detection results to MongoDB

**Technology Stack:**
- FastAPI
- Scikit-learn (Isolation Forest, One-Class SVM)
- Pandas & NumPy
- PyMongo (MongoDB client)
- Pydantic (data validation)

**Key Endpoints:**
```
POST   /api/fraud/analyze      - Analyze single transaction
POST   /api/fraud/batch        - Analyze multiple transactions
GET    /api/fraud/models       - Get model information
POST   /api/fraud/train        - Trigger model retraining
GET    /api/health             - Health check endpoint
```

**Fraud Detection Algorithm:**
1. **Feature Engineering**
   - Transaction amount normalization
   - Time-based features (hour, day of week)
   - User spending patterns
   - Transaction velocity (frequency)

2. **Anomaly Detection**
   - Isolation Forest for outlier detection
   - Statistical thresholds (mean ± 3σ)
   - Rule-based checks (suspicious patterns)

3. **Risk Scoring**
   - Score range: 0.0 (safe) to 1.0 (high risk)
   - Threshold: 0.7+ triggers fraud flag

**MongoDB Collections:**
```javascript
// fraud_logs
{
  _id: ObjectId,
  transaction_id: UUID,
  user_id: UUID,
  risk_score: Double,
  detected_anomalies: Array,
  features: Object,
  model_version: String,
  analyzed_at: ISODate
}

// audit_logs
{
  _id: ObjectId,
  user_id: UUID,
  action: String,
  resource: String,
  timestamp: ISODate,
  ip_address: String,
  metadata: Object
}
```

### 3. AWS API Gateway

**Configuration:**
- REST API type
- Regional endpoint
- Request validation
- CORS configuration
- Rate limiting: 1000 requests/minute per IP
- API keys for service-to-service communication

### 4. Data Flow Example: Creating a Transaction

```
1. Client → API Gateway
   POST /api/transactions
   Headers: { Authorization: "Bearer <JWT>" }
   Body: { amount: 150.00, type: "EXPENSE", category: "groceries" }

2. API Gateway → Java Service
   Routes request to Java Core Service

3. Java Service - Authentication
   - JWT filter validates token
   - Extracts user ID from token

4. Java Service - Business Logic
   - Validates transaction data
   - Saves transaction to PostgreSQL
   - Transaction gets temporary ID

5. Java Service → Python Fraud Service
   POST /api/fraud/analyze
   Body: { transaction_id, user_id, amount, category, timestamp }

6. Python Fraud Service - Analysis
   - Loads user's transaction history
   - Extracts features
   - Runs through ML model
   - Calculates risk score

7. Python Fraud Service → MongoDB
   - Logs fraud analysis results
   - Stores features and score

8. Python Fraud Service → Java Service
   Response: { risk_score: 0.23, is_fraud: false }

9. Java Service - Update
   - Updates transaction with fraud data
   - Commits to PostgreSQL

10. Java Service → API Gateway → Client
    Response: { id, amount, type, category, is_fraud_flagged, created_at }
```

## 🔒 Security Architecture

### Authentication
- **JWT (JSON Web Tokens)**: Stateless authentication
- **Token Expiry**: 24 hours
- **Refresh Tokens**: 7 days (future enhancement)

### Authorization
- Role-based access control (RBAC)
- User can only access their own data
- Service-to-service authentication via API keys

### Data Security
- Passwords: BCrypt hashing (strength 12)
- HTTPS/TLS for all communications
- Database encryption at rest (AWS RDS)
- Secrets management via AWS Secrets Manager

### API Security
- Rate limiting
- Request validation
- SQL injection prevention (parameterized queries)
- XSS protection

## 📊 Scalability Considerations

### Horizontal Scaling
- **Java Service**: Stateless design, can scale to multiple instances
- **Python Service**: Stateless, load balanced
- **Databases**: Read replicas for PostgreSQL, MongoDB sharding

### Caching Strategy (Future)
- Redis for session management
- Cache frequently accessed user data
- Cache fraud detection models

### Async Processing (Future)
- Amazon SQS for transaction processing queue
- Decouple fraud detection from transaction creation
- Event-driven architecture with EventBridge

## 🌐 AWS Deployment Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    AWS Cloud                             │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │  VPC (10.0.0.0/16)                             │    │
│  │                                                 │    │
│  │  ┌──────────────────┐  ┌──────────────────┐   │    │
│  │  │  Public Subnet   │  │  Public Subnet   │   │    │
│  │  │  10.0.1.0/24     │  │  10.0.2.0/24     │   │    │
│  │  │  (AZ-1)          │  │  (AZ-2)          │   │    │
│  │  │                  │  │                  │   │    │
│  │  │  NAT Gateway     │  │  NAT Gateway     │   │    │
│  │  └──────────────────┘  └──────────────────┘   │    │
│  │                                                 │    │
│  │  ┌──────────────────┐  ┌──────────────────┐   │    │
│  │  │  Private Subnet  │  │  Private Subnet  │   │    │
│  │  │  10.0.10.0/24    │  │  10.0.11.0/24    │   │    │
│  │  │  (AZ-1)          │  │  (AZ-2)          │   │    │
│  │  │                  │  │                  │   │    │
│  │  │  ECS Tasks:      │  │  ECS Tasks:      │   │    │
│  │  │  - Java Service  │  │  - Java Service  │   │    │
│  │  │  - Python Svc    │  │  - Python Svc    │   │    │
│  │  └──────────────────┘  └──────────────────┘   │    │
│  │                                                 │    │
│  │  ┌──────────────────┐  ┌──────────────────┐   │    │
│  │  │  Data Subnet     │  │  Data Subnet     │   │    │
│  │  │  10.0.20.0/24    │  │  10.0.21.0/24    │   │    │
│  │  │  (AZ-1)          │  │  (AZ-2)          │   │    │
│  │  │                  │  │                  │   │    │
│  │  │  RDS Primary     │  │  RDS Standby     │   │    │
│  │  │  DocumentDB      │  │  DocumentDB      │   │    │
│  │  └──────────────────┘  └──────────────────┘   │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │  Application Load Balancer                      │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │  API Gateway                                    │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## 🔍 Monitoring & Observability

### Metrics (AWS CloudWatch)
- Request count and latency
- Error rates (4xx, 5xx)
- Database connection pool metrics
- Fraud detection processing time

### Logging
- Application logs → CloudWatch Logs
- Structured JSON logging
- Correlation IDs for request tracing

### Alerting
- High error rate alerts
- Database connection failures
- Service health check failures
- Fraud detection threshold breaches

## 🧪 Testing Strategy

### Unit Tests
- Service layer logic
- ML model accuracy
- Utility functions

### Integration Tests
- API endpoint testing
- Database operations
- Service-to-service communication

### End-to-End Tests
- Full user workflows
- Authentication flows
- Transaction creation with fraud detection

## 📈 Future Enhancements

1. **Event-Driven Architecture**
   - Implement Amazon EventBridge
   - Async processing with SQS/SNS

2. **Advanced Analytics**
   - Real-time dashboards
   - Spending insights
   - Budget recommendations

3. **Enhanced Fraud Detection**
   - Deep learning models
   - Real-time streaming analysis
   - User behavior profiling

4. **Multi-tenancy**
   - Support for business accounts
   - Team collaboration features

5. **Mobile Applications**
   - iOS and Android native apps
   - Push notifications

---

**Document Version**: 1.0  
**Last Updated**: Phase 0 - Foundation  
**Status**: Architecture Approved ✅

