###############################################################################
# Outputs
###############################################################################

# ── Networking ───────────────────────────────────────────────────────────────

output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = aws_subnet.private[*].id
}

# ── Load Balancer ────────────────────────────────────────────────────────────

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = aws_lb.main.zone_id
}

output "api_url" {
  description = "Base URL for the FinGaurd API"
  value       = "http://${aws_lb.main.dns_name}"
}

# ── Database ─────────────────────────────────────────────────────────────────

output "rds_endpoint" {
  description = "Endpoint of the RDS PostgreSQL instance"
  value       = aws_db_instance.postgres.endpoint
}

output "rds_address" {
  description = "Address of the RDS PostgreSQL instance"
  value       = aws_db_instance.postgres.address
}

output "docdb_endpoint" {
  description = "Endpoint of the DocumentDB cluster"
  value       = aws_docdb_cluster.main.endpoint
}

# ── ECS ──────────────────────────────────────────────────────────────────────

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.main.name
}

output "java_service_name" {
  description = "Name of the Java ECS service"
  value       = aws_ecs_service.java_service.name
}

output "fraud_service_name" {
  description = "Name of the fraud detection ECS service"
  value       = aws_ecs_service.fraud_service.name
}

# ── Monitoring ───────────────────────────────────────────────────────────────

output "java_log_group" {
  description = "CloudWatch log group for Java service"
  value       = aws_cloudwatch_log_group.java_service.name
}

output "fraud_log_group" {
  description = "CloudWatch log group for fraud service"
  value       = aws_cloudwatch_log_group.fraud_service.name
}
