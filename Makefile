.PHONY: help build start stop restart clean logs test

# Default target
help:
	@echo "FinGaurd Development Commands"
	@echo "=============================="
	@echo "make build          - Build all Docker containers"
	@echo "make start          - Start all services"
	@echo "make start-dev      - Start all services including dev tools"
	@echo "make stop           - Stop all services"
	@echo "make restart        - Restart all services"
	@echo "make logs           - View logs from all services"
	@echo "make logs-java      - View Java service logs"
	@echo "make logs-python    - View Python service logs"
	@echo "make clean          - Remove all containers and volumes"
	@echo "make test-java      - Run Java service tests"
	@echo "make test-python    - Run Python service tests"
	@echo "make shell-java     - Open shell in Java container"
	@echo "make shell-python   - Open shell in Python container"

# Build all services
build:
	docker-compose build

# Start all services
start:
	docker-compose up -d

# Start with dev tools (mongo-express, pgadmin)
start-dev:
	docker-compose --profile dev up -d

# Stop all services
stop:
	docker-compose down

# Restart all services
restart:
	docker-compose restart

# View logs
logs:
	docker-compose logs -f

logs-java:
	docker-compose logs -f java-service

logs-python:
	docker-compose logs -f python-fraud-service

logs-postgres:
	docker-compose logs -f postgres

logs-mongodb:
	docker-compose logs -f mongodb

# Clean everything
clean:
	docker-compose down -v
	docker system prune -f

# Run tests
test-java:
	cd java-service && mvn test

test-python:
	cd python-fraud-service && pytest

# Open shell
shell-java:
	docker exec -it fingaurd-java-service /bin/sh

shell-python:
	docker exec -it fingaurd-fraud-service /bin/bash

# Database access
db-postgres:
	docker exec -it fingaurd-postgres psql -U fingaurd_user -d fingaurd_db

db-mongodb:
	docker exec -it fingaurd-mongodb mongosh fingaurd_fraud

# Check service health
health:
	@echo "Checking service health..."
	@curl -s http://localhost:8080/actuator/health | python3 -m json.tool || echo "Java service not responding"
	@curl -s http://localhost:8000/api/health | python3 -m json.tool || echo "Python service not responding"

# Initialize project (first time setup)
init:
	@echo "Initializing FinGaurd project..."
	cp python-fraud-service/env.example python-fraud-service/.env
	mkdir -p python-fraud-service/models
	touch python-fraud-service/models/.gitkeep
	@echo "Initialization complete!"

