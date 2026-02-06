# FinGaurd Terraform Infrastructure

Infrastructure as Code (IaC) for deploying FinGaurd to AWS.

## 📋 Overview

This directory contains Terraform configurations for provisioning AWS infrastructure:
- VPC with public and private subnets
- RDS PostgreSQL instance
- DocumentDB (MongoDB-compatible) cluster
- ECS Fargate for container orchestration
- Application Load Balancer
- API Gateway
- CloudWatch logging and monitoring
- Security groups and IAM roles

## 🏗️ Structure

```
terraform/
├── README.md
├── main.tf                    # Root module
├── variables.tf               # Input variables
├── outputs.tf                 # Output values
├── versions.tf                # Terraform and provider versions
├── environments/              # Environment-specific configs
│   ├── dev/
│   │   ├── main.tf
│   │   ├── terraform.tfvars
│   │   └── backend.tf
│   ├── staging/
│   └── prod/
└── modules/                   # Reusable modules
    ├── networking/
    │   ├── main.tf
    │   ├── variables.tf
    │   └── outputs.tf
    ├── database/
    │   ├── rds.tf
    │   ├── documentdb.tf
    │   └── ...
    ├── compute/
    │   ├── ecs.tf
    │   ├── fargate.tf
    │   └── ...
    ├── api-gateway/
    └── monitoring/
```

## 🚀 Getting Started

### Prerequisites

- Terraform >= 1.5.0
- AWS CLI configured with credentials
- AWS account with appropriate permissions

### Initialize Terraform

```bash
cd terraform/environments/dev
terraform init
```

### Plan Infrastructure

```bash
terraform plan -var-file=terraform.tfvars
```

### Apply Configuration

```bash
terraform apply -var-file=terraform.tfvars
```

### Destroy Infrastructure

```bash
terraform destroy -var-file=terraform.tfvars
```

## 🔧 Configuration

### Environment Variables

Create `terraform.tfvars` file:

```hcl
# AWS Configuration
aws_region = "us-east-1"
environment = "dev"

# Project
project_name = "fingaurd"

# Networking
vpc_cidr = "10.0.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b"]

# Database
db_instance_class = "db.t3.micro"
db_name = "fingaurd_db"
db_username = "fingaurd_admin"

# ECS
ecs_task_cpu = "256"
ecs_task_memory = "512"
```

## 📚 Modules

### Networking Module

Creates VPC, subnets, NAT gateways, and routing tables.

### Database Module

Provisions RDS PostgreSQL and DocumentDB clusters.

### Compute Module

Sets up ECS cluster, task definitions, and Fargate services.

### API Gateway Module

Configures API Gateway with routes to ECS services.

### Monitoring Module

Sets up CloudWatch logs, metrics, and alarms.

## 🔒 Security

- All secrets stored in AWS Secrets Manager
- Security groups with minimal access
- Private subnets for databases
- IAM roles with least privilege

## 💰 Cost Estimation

Development environment (~$30-50/month):
- RDS t3.micro: ~$15/month
- DocumentDB t3.medium: ~$30/month
- ECS Fargate: ~$10/month
- NAT Gateway: ~$32/month
- Other services: ~$5/month

**Total: ~$92/month**

Use AWS Free Tier where applicable to reduce costs.

## 📝 Notes

- Start with development environment
- Use remote state (S3 + DynamoDB) for production
- Implement proper state locking

---

**Status:** Phase 3 - Complete ✅

