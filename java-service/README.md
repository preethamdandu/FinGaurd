# FinGaurd Core Service (Java)

Spring Boot microservice for user management, authentication, and transaction processing.

## 📋 Overview

This service provides the core functionality for the FinGaurd platform:
- User registration and authentication (JWT-based)
- Transaction CRUD operations
- Integration with Python fraud detection service
- RESTful API endpoints

## 🏗️ Project Structure

```
java-service/
├── src/
│   ├── main/
│   │   ├── java/com/fingaurd/
│   │   │   ├── FinGaurdApplication.java
│   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── TransactionController.java
│   │   │   ├── service/         # Business logic
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   └── FraudDetectionClient.java
│   │   │   ├── repository/      # Data access layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── TransactionRepository.java
│   │   │   ├── model/           # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   └── Transaction.java
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── security/        # Security components
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── exception/       # Custom exceptions
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── util/            # Utility classes
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── migration/   # Database migration scripts
│   └── test/
│       └── java/com/fingaurd/   # Unit and integration tests
├── Dockerfile
├── pom.xml
└── README.md
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 15+ (or H2 for development)
- Docker (optional)

### Local Development Setup

1. **Clone the repository**
```bash
cd java-service
```

2. **Configure database**

For H2 (in-memory database):
```bash
# Use dev profile (H2 enabled by default)
export SPRING_PROFILES_ACTIVE=dev
```

For PostgreSQL:
```bash
# Start PostgreSQL using Docker
docker run -d \
  --name fingaurd-postgres \
  -e POSTGRES_DB=fingaurd_db \
  -e POSTGRES_USER=fingaurd_user \
  -e POSTGRES_PASSWORD=changeme \
  -p 5432:5432 \
  postgres:15-alpine
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run the application**
```bash
# With H2 database (dev profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# With PostgreSQL (default profile)
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Using Docker

1. **Build Docker image**
```bash
docker build -t fingaurd-core-service:latest .
```

2. **Run container**
```bash
docker run -d \
  --name fingaurd-core \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  fingaurd-core-service:latest
```

## 📡 API Endpoints

### Authentication
```
POST   /api/auth/register   - Register new user
POST   /api/auth/login      - User login
POST   /api/auth/logout     - User logout
```

### User Management
```
GET    /api/users/me        - Get current user profile
PUT    /api/users/me        - Update user profile
```

### Transaction Management
```
POST   /api/transactions           - Create transaction
GET    /api/transactions           - Get all transactions (paginated)
GET    /api/transactions/{id}      - Get transaction by ID
PUT    /api/transactions/{id}      - Update transaction
DELETE /api/transactions/{id}      - Delete transaction
GET    /api/transactions/stats     - Get transaction statistics
GET    /api/transactions/stats/by-category - Get spending by category
```

### Health Check
```
GET    /actuator/health     - Service health status
GET    /actuator/info       - Application info
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/fingaurd_db` |
| `DATABASE_USERNAME` | Database username | `fingaurd_user` |
| `DATABASE_PASSWORD` | Database password | `changeme` |
| `JWT_SECRET` | Secret key for JWT signing | (set in application.yml) |
| `FRAUD_SERVICE_URL` | Fraud detection service URL | `http://localhost:8000` |

### Application Profiles

- **default**: Production-like settings with PostgreSQL
- **dev**: Development settings with H2 in-memory database
- **prod**: Production settings with external configuration

## 🧪 Testing

### Run all tests
```bash
mvn test
```

### Run specific test class
```bash
mvn test -Dtest=UserServiceTest
```

### Generate test coverage report
```bash
mvn clean test jacoco:report
# Report available at: target/site/jacoco/index.html
```

## 🔐 Security

### JWT Authentication

All endpoints (except `/api/auth/register` and `/api/auth/login`) require JWT authentication:

```bash
# Include JWT token in Authorization header
curl -H "Authorization: Bearer <your-jwt-token>" \
     http://localhost:8080/api/users/me
```

### Password Security

- Passwords are hashed using BCrypt with strength 12
- Minimum password requirements enforced
- No plaintext password storage

## 🐳 Docker Compose

For local development with all dependencies:

```yaml
# See ../docker-compose.yml at project root
docker-compose up -d
```

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN
);
```

### Transactions Table
```sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_fraud_flagged BOOLEAN,
    fraud_risk_score DECIMAL(5,4)
);
```

## 🛠️ Development Tools

### H2 Console (Dev Profile)
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:fingaurd_dev
Username: sa
Password: (empty)
```

### Swagger UI (Future)
```
URL: http://localhost:8080/swagger-ui.html
```

## 📝 Logging

Logs are written to:
- Console (stdout)
- `logs/spring-boot-logger.log` (file)

Log levels can be configured in `application.yml`:
```yaml
logging:
  level:
    com.fingaurd: DEBUG
    org.springframework.security: DEBUG
```

## 🚀 Deployment

### Building for production
```bash
mvn clean package -DskipTests
```

The JAR file will be created at: `target/fingaurd-core-service-0.1.0-SNAPSHOT.jar`

### Running production JAR
```bash
java -jar target/fingaurd-core-service-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --jwt.secret=${JWT_SECRET} \
  --spring.datasource.url=${DATABASE_URL}
```

## 🔍 Troubleshooting

### Port already in use
```bash
# Change port in application.yml or use environment variable
export SERVER_PORT=8081
mvn spring-boot:run
```

### Database connection issues
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check connection
psql -h localhost -U fingaurd_user -d fingaurd_db
```

### JWT token errors
- Ensure JWT_SECRET is set and consistent
- Check token expiration time
- Verify Authorization header format: `Bearer <token>`

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)

---

**Version:** 0.1.0  
**Last Updated:** Phase 0 - Foundation

