# FinGaurd 🛡️

A cloud-native financial management and fraud detection system built with microservices architecture.

## 🎯 Project Overview

FinGaurd is a comprehensive financial management platform that combines transaction tracking with intelligent fraud detection. The system leverages a microservices architecture deployed on AWS, utilizing both Java and Python services to deliver secure, scalable financial operations.

## 🚀 MVP Features

### Core Functionalities

1. **User Management**
   - Secure user registration and authentication
   - JWT-based authorization
   - Profile management

2. **Transaction Management**
   - Record financial transactions (income/expenses)
   - View transaction history with filtering and pagination
   - Transaction categorization
   - Real-time balance tracking

3. **Fraud Detection**
   - Automated anomaly detection on transactions
   - Risk scoring system
   - Fraud alerts and notifications

4. **Audit & Logging**
   - Comprehensive activity logging
   - Audit trails for compliance

## 📋 User Stories

### Authentication & Authorization
- **US-001**: As a user, I want to create an account with email and password so that I can access the system securely.
- **US-002**: As a user, I want to log in securely with my credentials so that I can access my financial data.
- **US-003**: As a user, I want to log out of my account so that my data remains secure.

### Transaction Management
- **US-004**: As a user, I want to record a new transaction (income or expense) so that I can track my finances.
- **US-005**: As a user, I want to view my transaction history so that I can see my spending patterns.
- **US-006**: As a user, I want to filter transactions by date, category, or amount so that I can analyze specific financial activities.
- **US-007**: As a user, I want to see my current balance so that I know my financial status.

### Fraud Detection
- **US-008**: As a user, I want the system to automatically detect suspicious transactions so that I can be alerted to potential fraud.
- **US-009**: As a user, I want to receive notifications about flagged transactions so that I can take immediate action.

### Reporting & Analytics
- **US-010**: As a user, I want to view spending reports by category so that I can understand my financial habits.

## 🏗️ Architecture

FinGaurd follows a microservices architecture pattern:

```
┌─────────────────┐
│  React Frontend │
│  (port 3000)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  AWS API        │
│  Gateway        │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│   Java Core Service (Spring Boot)       │
│   - User Management                     │
│   - Transaction Management              │
│   - Authentication/Authorization        │
└──────┬──────────────────────┬───────────┘
       │                      │
       ▼                      ▼
┌──────────────┐    ┌──────────────────┐
│  PostgreSQL  │    │  Python Fraud    │
│   Database   │    │  Detection       │
│              │    │  Service         │
└──────────────┘    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │    MongoDB       │
                    │  (Logs & Audit)  │
                    └──────────────────┘
```

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed system design.

## 🛠️ Technology Stack

### Backend Services
- **Java Service**: Spring Boot 3.x, Spring Security, Spring Data JPA
- **Python Service**: FastAPI, Scikit-learn (fraud detection ML)

### Databases
- **PostgreSQL**: Primary relational database for users and transactions
- **MongoDB**: Document store for logs and audit trails

### Infrastructure
- **AWS Services**: API Gateway, ECS/Fargate, RDS, DocumentDB, CloudWatch
- **Containerization**: Docker
- **Infrastructure as Code**: Terraform
- **CI/CD**: GitHub Actions

### Development Tools
- Maven/Gradle (Java build tool)
- Poetry/pip (Python dependency management)
- Docker Desktop
- Postman (API testing)

## 📁 Project Structure

```
FinGaurd/
├── README.md
├── ARCHITECTURE.md
├── SETUP.md
├── USER_STORIES.md
├── docs/
│   ├── api/API_REFERENCE.md
│   └── deployment/DEPLOYMENT_GUIDE.md
├── java-service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── python-fraud-service/
│   ├── app/
│   ├── requirements.txt
│   └── Dockerfile
├── terraform/
│   ├── modules/
│   ├── environments/
│   └── main.tf
└── .github/
    └── workflows/
        ├── ci.yml
        └── deploy.yml
```

## 🚦 Development Roadmap

### Phase 0: Foundation & System Design ✅
- Define MVP features and user stories
- Create architectural blueprint
- Set up development environment

### Phase 1: Local Development ✅
- Java service: Auth, user management, full transaction CRUD
- Transaction filtering, statistics, category analytics
- Python fraud detection: rule-based + ML (Isolation Forest)
- MongoDB audit logging, health checks

### Phase 2: Containerization ✅
- Multi-stage Dockerfiles (Java & Python)
- Docker Compose with PostgreSQL, MongoDB, both services
- Health checks, inter-service communication

### Phase 3: Cloud Infrastructure ✅
- Full Terraform config: VPC, subnets, RDS, DocumentDB, ECS Fargate, ALB
- CloudWatch logging and alerting
- Security groups, IAM roles, Secrets Manager
- Environment configs (dev / prod)

### Phase 4: CI/CD & Monitoring ✅
- GitHub Actions CI: build, test, lint, Docker verify
- GitHub Actions Deploy: GHCR push + Render webhooks
- Render.com deployment config

### Phase 5: Polish & Production ✅
- API reference documentation
- Deployment guide (Docker / Render / AWS)
- Consolidated Docker configs
- Security best practices documented

## 🎯 Getting Started

To set up your development environment, please follow the instructions in [SETUP.md](./SETUP.md).

## 📝 License

This project is part of a learning initiative for cloud-native application development.

## 👥 Contributing

This is a personal learning project. Feel free to fork and adapt for your own learning journey!

---

**Status**: All Phases Complete ✅

