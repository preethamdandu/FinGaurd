# FinGaurd – Production Environment
aws_region         = "us-east-1"
environment        = "prod"
project_name       = "fingaurd"

# Networking
vpc_cidr           = "10.0.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b"]

# Database (production-grade instances)
db_instance_class  = "db.t3.small"
db_name            = "fingaurd_db"
db_username        = "fingaurd_admin"
db_password        = "CHANGE_ME_IN_SECRETS_MANAGER"

# ECS (higher resources for production)
ecs_task_cpu    = "512"
ecs_task_memory = "1024"
