# FinGaurd Deployment Guide

## Deployment Options

FinGaurd supports three deployment targets:

| Target               | Use Case              | Cost        |
|----------------------|-----------------------|-------------|
| Docker Compose       | Local development     | Free        |
| Render.com           | Quick cloud hosting   | Free tier   |
| AWS (ECS/Fargate)    | Production workloads  | ~$90/month  |

---

## 1. Local Development (Docker Compose)

### Prerequisites
- Docker Desktop installed and running
- At least 4 GB of RAM allocated to Docker

### Quick Start

```bash
# Initialize the project
make init

# Build and start all services
docker compose up --build -d

# Verify everything is running
make health

# View logs
make logs
```

### Services Launched

| Service               | Port  | URL                          |
|-----------------------|-------|------------------------------|
| Java API              | 8080  | http://localhost:8080        |
| Fraud Detection API   | 8000  | http://localhost:8000/docs   |
| PostgreSQL            | 5432  | —                            |
| MongoDB               | 27017 | —                            |
| pgAdmin (dev profile) | 5050  | http://localhost:5050        |
| Mongo Express (dev)   | 8081  | http://localhost:8081        |

### Using Dev Tools

```bash
# Start with pgAdmin and Mongo Express
docker compose --profile dev up -d
```

---

## 2. Render.com Deployment

### Prerequisites
- Render account (https://render.com)
- GitHub repository connected

### Setup

1. Push your code to GitHub.
2. Go to Render Dashboard > **New** > **Blueprint**.
3. Connect your repository and select the `render.yaml` file.
4. Render will auto-detect the services.
5. Set the following secrets in the Render dashboard:
   - `DATABASE_URL` – PostgreSQL connection string
   - `DATABASE_USERNAME` – DB username
   - `DATABASE_PASSWORD` – DB password
   - `JWT_SECRET` – 256-bit secret key
   - `MONGODB_URL` – MongoDB connection string

### Environment Variables

The `render.yaml` file defines all services. Key environment variables are
injected from Render's secret management.

---

## 3. AWS Deployment (ECS/Fargate + Terraform)

### Prerequisites
- AWS CLI configured (`aws configure`)
- Terraform >= 1.5.0 installed
- Docker installed (for building images)

### Step 1: Create ECR Repositories

```bash
aws ecr create-repository --repository-name fingaurd-dev-java-service
aws ecr create-repository --repository-name fingaurd-dev-fraud-service
```

### Step 2: Build and Push Images

```bash
# Login to ECR
aws ecr get-login-password | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Build and push Java service
docker build -t fingaurd-java-service ./java-service
docker tag fingaurd-java-service:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/fingaurd-dev-java-service:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/fingaurd-dev-java-service:latest

# Build and push fraud service
docker build -t fingaurd-fraud-service ./python-fraud-service
docker tag fingaurd-fraud-service:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/fingaurd-dev-fraud-service:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/fingaurd-dev-fraud-service:latest
```

### Step 3: Provision Infrastructure

```bash
cd terraform

# Initialize Terraform
terraform init

# Preview changes
terraform plan -var-file=environments/dev/terraform.tfvars

# Apply (type "yes" to confirm)
terraform apply -var-file=environments/dev/terraform.tfvars
```

### Step 4: Verify Deployment

```bash
# Get the ALB URL
terraform output api_url

# Test health endpoints
curl $(terraform output -raw api_url)/actuator/health
curl $(terraform output -raw api_url)/api/health
```

### AWS Architecture

```
Internet → ALB → ECS Fargate (Java + Python)
                        ↓               ↓
                  RDS PostgreSQL   DocumentDB (MongoDB)
```

All services run in private subnets. Only the ALB is publicly accessible.

### Tear Down

```bash
terraform destroy -var-file=environments/dev/terraform.tfvars
```

---

## CI/CD Pipeline

The GitHub Actions pipeline (`.github/workflows/`) automates:

1. **CI** (`ci.yml`) – Runs on every push and PR:
   - Java: Build + unit tests with Maven
   - Python: Lint with ruff + pytest
   - Docker: Verify images build successfully

2. **Deploy** (`deploy.yml`) – Runs on push to `main`:
   - Runs CI first
   - Builds and pushes Docker images to GHCR
   - Triggers Render deployment webhooks

### Required GitHub Secrets

| Secret                     | Description                       |
|----------------------------|-----------------------------------|
| `RENDER_DEPLOY_HOOK_JAVA`  | Render deploy webhook (Java)      |
| `RENDER_DEPLOY_HOOK_PYTHON`| Render deploy webhook (Python)    |

---

## Security Checklist

Before going to production:

- [ ] Change all default passwords in docker-compose
- [ ] Use AWS Secrets Manager for sensitive values
- [ ] Enable HTTPS/TLS on the ALB (ACM certificate)
- [ ] Generate a strong JWT secret (256-bit minimum)
- [ ] Enable RDS encryption at rest
- [ ] Restrict security group ingress rules
- [ ] Enable CloudWatch alerting
- [ ] Set up log retention policies
- [ ] Review IAM roles for least privilege
- [ ] Enable MFA on AWS root account

---

## Monitoring

### CloudWatch Logs
- Java service: `/ecs/fingaurd-{env}/java-service`
- Fraud service: `/ecs/fingaurd-{env}/fraud-service`

### CloudWatch Alarms (auto-provisioned by Terraform)
- ECS service CPU > 80%
- RDS CPU > 80%

### Health Endpoints
- Java: `GET /actuator/health`
- Python: `GET /api/health` and `GET /api/ready`

### Local Monitoring
```bash
# Service health
make health

# Service logs
make logs
make logs-java
make logs-python
```
