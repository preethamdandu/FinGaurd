# FinGaurd – Development Environment
aws_region         = "us-east-1"
environment        = "dev"
project_name       = "fingaurd"

# Networking
vpc_cidr           = "10.0.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b"]

# Database (smallest instances for cost savings)
db_instance_class  = "db.t3.micro"
db_name            = "fingaurd_db"
db_username        = "fingaurd_admin"
db_password        = "CHANGE_ME_IN_SECRETS_MANAGER"

# ECS (minimal resources for dev)
ecs_task_cpu    = "256"
ecs_task_memory = "512"
