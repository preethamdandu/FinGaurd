#!/bin/bash

# FinGaurd Setup Verification Script
# This script checks if all required tools are installed

echo "╔═══════════════════════════════════════════════════╗"
echo "║   FinGaurd Development Environment Verification   ║"
echo "╚═══════════════════════════════════════════════════╝"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Helper function to check command
check_command() {
    local cmd=$1
    local name=$2
    local required=$3
    
    if command -v "$cmd" &> /dev/null; then
        version=$($cmd --version 2>&1 | head -n 1)
        echo -e "${GREEN}✓${NC} $name: $version"
        ((PASSED++))
        return 0
    else
        if [ "$required" = "required" ]; then
            echo -e "${RED}✗${NC} $name: Not installed (REQUIRED)"
            ((FAILED++))
        else
            echo -e "${YELLOW}⚠${NC} $name: Not installed (Optional)"
            ((WARNINGS++))
        fi
        return 1
    fi
}

echo "=== Core Development Tools ==="
check_command "java" "Java" "required"
check_command "python3" "Python" "required"
check_command "node" "Node.js" "optional"

echo ""
echo "=== Build Tools ==="
check_command "mvn" "Maven" "required"
check_command "gradle" "Gradle" "optional"

echo ""
echo "=== Containerization ==="
check_command "docker" "Docker" "required"

if command -v docker &> /dev/null; then
    if docker ps &> /dev/null; then
        echo -e "${GREEN}✓${NC} Docker daemon is running"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} Docker daemon is not running"
        ((FAILED++))
    fi
fi

# Check docker-compose (could be standalone or plugin)
if docker compose version &> /dev/null 2>&1; then
    version=$(docker compose version)
    echo -e "${GREEN}✓${NC} Docker Compose: $version"
    ((PASSED++))
elif command -v docker-compose &> /dev/null; then
    version=$(docker-compose --version)
    echo -e "${GREEN}✓${NC} Docker Compose: $version"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Docker Compose: Not installed (REQUIRED)"
    ((FAILED++))
fi

echo ""
echo "=== Cloud & Infrastructure ==="
check_command "aws" "AWS CLI" "required"
check_command "terraform" "Terraform" "required"

echo ""
echo "=== Database Clients ==="
check_command "psql" "PostgreSQL Client" "optional"
check_command "mongosh" "MongoDB Shell" "optional"

echo ""
echo "=== Version Control ==="
check_command "git" "Git" "required"

# Check git configuration
if command -v git &> /dev/null; then
    git_name=$(git config --global user.name)
    git_email=$(git config --global user.email)
    
    if [ -n "$git_name" ] && [ -n "$git_email" ]; then
        echo -e "${GREEN}✓${NC} Git configured: $git_name <$git_email>"
        ((PASSED++))
    else
        echo -e "${YELLOW}⚠${NC} Git not configured (run 'git config --global user.name' and 'git config --global user.email')"
        ((WARNINGS++))
    fi
fi

echo ""
echo "=== AWS Configuration ==="
if [ -f ~/.aws/credentials ]; then
    echo -e "${GREEN}✓${NC} AWS credentials found"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC} AWS credentials not found (run 'aws configure')"
    ((WARNINGS++))
fi

echo ""
echo "=== Python Packages ==="
if command -v pip3 &> /dev/null; then
    echo -e "${GREEN}✓${NC} pip3 installed"
    ((PASSED++))
    
    if command -v virtualenv &> /dev/null; then
        echo -e "${GREEN}✓${NC} virtualenv installed"
        ((PASSED++))
    else
        echo -e "${YELLOW}⚠${NC} virtualenv not installed (optional, can use python3 -m venv)"
        ((WARNINGS++))
    fi
else
    echo -e "${RED}✗${NC} pip3 not installed"
    ((FAILED++))
fi

echo ""
echo "=== Project Structure ==="

# Check if key directories exist
dirs=("java-service" "python-fraud-service" "terraform")
for dir in "${dirs[@]}"; do
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✓${NC} Directory exists: $dir"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} Directory missing: $dir"
        ((FAILED++))
    fi
done

# Check if key files exist
files=("README.md" "ARCHITECTURE.md" "SETUP.md" "docker-compose.yml" "Makefile")
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} File exists: $file"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} File missing: $file"
        ((FAILED++))
    fi
done

echo ""
echo "╔═══════════════════════════════════════════════════╗"
echo "║                   Summary                         ║"
echo "╚═══════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}Passed:${NC} $PASSED"
echo -e "${RED}Failed:${NC} $FAILED"
echo -e "${YELLOW}Warnings:${NC} $WARNINGS"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All required tools are installed!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Review QUICKSTART.md to start the application"
    echo "2. Read ARCHITECTURE.md to understand the system"
    echo "3. Check USER_STORIES.md for development tasks"
    echo ""
    echo "To start all services: make start"
    exit 0
else
    echo -e "${RED}✗ Some required tools are missing.${NC}"
    echo ""
    echo "Please refer to SETUP.md for installation instructions."
    exit 1
fi

