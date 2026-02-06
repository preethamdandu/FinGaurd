# FinGaurd Quick Start Guide

Get FinGaurd up and running on your local machine in under 10 minutes!

## ⚡ Prerequisites

Before starting, ensure you have:
- ✅ Java 17+
- ✅ Python 3.10+
- ✅ Docker Desktop installed and running
- ✅ Git configured

## 🚀 Quick Setup (Using Docker Compose)

### Step 1: Clone the Repository

```bash
# If you haven't set up git remote yet
cd /Users/preethamdandu/Desktop/FinGaurd
```

### Step 2: Initialize Project

```bash
# Run initialization script
make init

# Or manually:
cp python-fraud-service/env.example python-fraud-service/.env
mkdir -p python-fraud-service/models
```

### Step 3: Start All Services

```bash
# Build and start all services (Java, Python, PostgreSQL, MongoDB)
docker-compose up --build -d

# View logs
docker-compose logs -f

# Or use make commands
make build
make start
make logs
```

### Step 4: Verify Services

Wait ~30 seconds for services to start, then check:

```bash
# Java service health check
curl http://localhost:8080/actuator/health

# Python service health check
curl http://localhost:8000/api/health

# Or use make command
make health
```

Expected output:
```json
{
  "status": "healthy",
  "timestamp": "2025-10-09T...",
  "service": "fraud-detection"
}
```

### Step 5: Test the APIs

**Register a new user:**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!"
  }'
```

Save the `accessToken` from the response!

**Create a transaction:**
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "amount": 150.00,
    "transactionType": "EXPENSE",
    "category": "groceries",
    "description": "Weekly shopping"
  }'
```

## 🛠️ Development Mode (Without Docker)

### Option 1: Using H2 In-Memory Database (Easiest)

**Java Service:**
```bash
cd java-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

This uses H2 database (no PostgreSQL needed!) and has fraud detection disabled.

### Option 2: Using Local Databases

**Start databases:**
```bash
# PostgreSQL
docker run -d --name fingaurd-postgres \
  -e POSTGRES_DB=fingaurd_db \
  -e POSTGRES_USER=fingaurd_user \
  -e POSTGRES_PASSWORD=changeme \
  -p 5432:5432 postgres:15-alpine

# MongoDB
docker run -d --name fingaurd-mongodb \
  -p 27017:27017 mongo:7-jammy
```

**Start Java service:**
```bash
cd java-service
mvn spring-boot:run
```

**Start Python service:**
```bash
cd python-fraud-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## 🎯 What's Next?

Now that you have FinGaurd running:

### 1. Explore the APIs

**Interactive API Documentation:**
- Java Service: http://localhost:8080/swagger-ui.html (coming in Phase 1)
- Python Service: http://localhost:8000/docs

### 2. Check Database UIs (Optional)

Start with dev profile:
```bash
docker-compose --profile dev up -d
```

- **MongoDB Express**: http://localhost:8081 (admin/admin123)
- **pgAdmin**: http://localhost:5050 (admin@fingaurd.com/admin123)
- **H2 Console** (if using dev profile): http://localhost:8080/h2-console

### 3. View Logs

```bash
# All services
make logs

# Specific service
make logs-java
make logs-python
```

### 4. Access Databases Directly

```bash
# PostgreSQL
make db-postgres

# MongoDB
make db-mongodb
```

## 🧪 Testing

**Java Service:**
```bash
cd java-service
mvn test
```

**Python Service:**
```bash
cd python-fraud-service
pytest
```

## 🛑 Stopping Services

```bash
# Stop all services
docker-compose down

# Or
make stop

# Stop and remove volumes (clean slate)
docker-compose down -v

# Or
make clean
```

## 📱 API Testing with Postman

1. Import the Postman collection (coming soon in `docs/postman/`)
2. Set environment variables:
   - `BASE_URL`: `http://localhost:8080`
   - `FRAUD_URL`: `http://localhost:8000`
   - `TOKEN`: (obtained from login)

## 🐛 Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port
export SERVER_PORT=8081
```

### Docker Issues

```bash
# Restart Docker Desktop

# Clean Docker system
docker system prune -a

# Rebuild from scratch
make clean
make build
make start
```

### Database Connection Errors

```bash
# Check containers are running
docker ps

# Restart specific service
docker-compose restart postgres
docker-compose restart mongodb
```

### Java Build Errors

```bash
# Clean and rebuild
cd java-service
mvn clean install -U
```

### Python Import Errors

```bash
# Recreate virtual environment
cd python-fraud-service
rm -rf venv
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## 📚 Next Steps

1. ✅ **Phase 0 Complete**: Foundation set up
2. ➡️ **Phase 1**: Implement core features (see [USER_STORIES.md](./USER_STORIES.md))
3. 📖 Read [ARCHITECTURE.md](./ARCHITECTURE.md) for system design
4. 👨‍💻 Review [CONTRIBUTING.md](./CONTRIBUTING.md) for development guidelines

## 💡 Helpful Commands

```bash
# View all available commands
make help

# Health check
make health

# View logs
make logs
make logs-java
make logs-python

# Run tests
make test-java
make test-python

# Shell access
make shell-java
make shell-python

# Database access
make db-postgres
make db-mongodb
```

## 🎓 Learning Resources

- **Spring Boot**: https://spring.io/guides
- **FastAPI**: https://fastapi.tiangolo.com/tutorial/
- **Docker**: https://docs.docker.com/get-started/
- **AWS**: https://aws.amazon.com/getting-started/

---

**Happy Coding!** 🚀

If you encounter any issues, please check:
1. [SETUP.md](./SETUP.md) for detailed setup instructions
2. [Troubleshooting section](#-troubleshooting) above
3. GitHub Issues for known problems

