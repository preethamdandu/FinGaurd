# FinGaurd Development Environment Setup

This guide will walk you through setting up your complete development environment for the FinGaurd project.

## 📋 Prerequisites Checklist

Before starting, ensure you have the following:
- [ ] macOS, Linux, or Windows (with WSL2)
- [ ] Administrator/sudo access
- [ ] Stable internet connection
- [ ] At least 10GB free disk space

## 🔧 Step 1: Install Core Development Tools

### 1.1 Java Development Kit (JDK)

**Install Java 17 or Later:**

**Option A: Using SDKMAN (Recommended for macOS/Linux)**
```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 17
sdk install java 17.0.9-tem

# Verify installation
java -version
javac -version
```

**Option B: Using Homebrew (macOS)**
```bash
brew install openjdk@17

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify
java -version
```

**Option C: Manual Download**
- Download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
- Follow installer instructions
- Set `JAVA_HOME` environment variable

**Verify Installation:**
```bash
java -version
# Expected output: java version "17.0.x" or higher
```

### 1.2 Python

**Install Python 3.10 or Later:**

**macOS:**
```bash
# Using Homebrew
brew install python@3.11

# Verify installation
python3 --version
pip3 --version
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install python3.11 python3.11-venv python3-pip

# Verify
python3 --version
```

**Windows:**
- Download from [python.org](https://www.python.org/downloads/)
- During installation, check "Add Python to PATH"

**Set up Python Virtual Environment:**
```bash
# Install virtualenv
pip3 install virtualenv

# Verify
virtualenv --version
```

### 1.3 Node.js & npm (for future frontend)

**Using nvm (Node Version Manager) - Recommended:**
```bash
# Install nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash

# Reload shell
source ~/.zshrc  # or ~/.bashrc

# Install Node.js LTS
nvm install --lts
nvm use --lts

# Verify
node --version
npm --version
```

## 🏗️ Step 2: Install Build Tools

### 2.1 Maven

**macOS:**
```bash
brew install maven

# Verify
mvn -version
```

**Linux:**
```bash
sudo apt install maven

# Verify
mvn -version
```

**Manual Installation:**
```bash
# Download Maven
wget https://dlcdn.apache.org/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz

# Extract
tar xzf apache-maven-3.9.5-bin.tar.gz
sudo mv apache-maven-3.9.5 /opt/maven

# Add to PATH
echo 'export PATH=/opt/maven/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# Verify
mvn -version
```

### 2.2 Gradle (Alternative to Maven)

```bash
# macOS
brew install gradle

# Linux
sdk install gradle

# Verify
gradle --version
```

## 🐳 Step 3: Install Docker

### 3.1 Docker Desktop

**macOS:**
1. Download [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/)
2. Install the .dmg file
3. Launch Docker Desktop
4. Verify installation:
```bash
docker --version
docker-compose --version
```

**Windows:**
1. Install WSL2 first
2. Download [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
3. Follow installation wizard
4. Enable WSL2 backend

**Linux (Ubuntu):**
```bash
# Update package index
sudo apt-get update

# Install dependencies
sudo apt-get install ca-certificates curl gnupg lsb-release

# Add Docker's official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker

# Verify
docker --version
docker compose version
```

### 3.2 Test Docker Installation

```bash
# Run hello-world container
docker run hello-world

# Should see "Hello from Docker!" message
```

## ☁️ Step 4: Set Up AWS

### 4.1 Create AWS Account

1. Visit [AWS Free Tier](https://aws.amazon.com/free/)
2. Click "Create a Free Account"
3. Follow registration process:
   - Provide email and password
   - Enter account information
   - Provide payment method (required, but free tier won't charge)
   - Verify identity
   - Select Basic Support Plan (Free)

### 4.2 Install AWS CLI

**macOS:**
```bash
# Using Homebrew
brew install awscli

# Verify
aws --version
```

**Linux:**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Verify
aws --version
```

**Windows:**
- Download and run the [AWS CLI MSI installer](https://awscli.amazonaws.com/AWSCLIV2.msi)

### 4.3 Configure AWS CLI

1. **Create IAM User:**
   - Log into AWS Console
   - Navigate to IAM → Users → Add User
   - User name: `fingaurd-dev`
   - Access type: Programmatic access
   - Attach policies: `AdministratorAccess` (for development)
   - Save Access Key ID and Secret Access Key

2. **Configure CLI:**
```bash
aws configure

# Enter when prompted:
AWS Access Key ID: [Your Access Key]
AWS Secret Access Key: [Your Secret Key]
Default region name: us-east-1  # or your preferred region
Default output format: json
```

3. **Verify Configuration:**
```bash
aws sts get-caller-identity

# Should return your account information
```

### 4.4 Create AWS Resources Budget Alert (Optional but Recommended)

```bash
# Set up billing alert to avoid unexpected charges
aws budgets create-budget \
    --account-id $(aws sts get-caller-identity --query Account --output text) \
    --budget file://budget.json

# Create budget.json first (see AWS documentation)
```

## 🏗️ Step 5: Install Terraform

### 5.1 Install Terraform CLI

**macOS:**
```bash
brew tap hashicorp/tap
brew install hashicorp/tap/terraform

# Verify
terraform --version
```

**Linux:**
```bash
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg

echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list

sudo apt update && sudo apt install terraform

# Verify
terraform --version
```

**Windows:**
- Download from [Terraform Downloads](https://www.terraform.io/downloads)
- Extract and add to PATH

### 5.2 Terraform AWS Provider Setup

Create a test file to verify Terraform works with AWS:
```bash
# Create test directory
mkdir -p ~/terraform-test
cd ~/terraform-test

# Create main.tf
cat > main.tf << 'EOF'
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# Test data source
data "aws_caller_identity" "current" {}

output "account_id" {
  value = data.aws_caller_identity.current.account_id
}
EOF

# Initialize and test
terraform init
terraform plan

# Clean up
cd ~
rm -rf ~/terraform-test
```

## 🛠️ Step 6: Install IDE and Extensions

### 6.1 Visual Studio Code (Recommended)

**Download and Install:**
- Download from [code.visualstudio.com](https://code.visualstudio.com/)
- Install for your operating system

**Install Essential Extensions:**
```bash
# Install via command line
code --install-extension vscjava.vscode-java-pack
code --install-extension ms-python.python
code --install-extension ms-azuretools.vscode-docker
code --install-extension hashicorp.terraform
code --install-extension redhat.vscode-yaml
code --install-extension esbenp.prettier-vscode
code --install-extension dbaeumer.vscode-eslint
code --install-extension rangav.vscode-thunder-client  # Postman alternative
```

**Recommended Extensions:**
- Extension Pack for Java (Microsoft)
- Python (Microsoft)
- Docker (Microsoft)
- Terraform (HashiCorp)
- YAML (Red Hat)
- REST Client or Thunder Client
- GitLens
- Prettier - Code formatter

### 6.2 IntelliJ IDEA (Alternative for Java)

**Community Edition (Free):**
- Download from [JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
- Install and set up Java SDK
- Install plugins: Docker, AWS Toolkit, Terraform

## 📦 Step 7: Install Database Tools

### 7.1 PostgreSQL Client

**macOS:**
```bash
brew install postgresql@15

# This installs psql client
psql --version
```

**Linux:**
```bash
sudo apt install postgresql-client
psql --version
```

### 7.2 MongoDB Client

**macOS:**
```bash
brew tap mongodb/brew
brew install mongodb-community-shell

# Verify
mongosh --version
```

**Linux:**
```bash
# Import MongoDB public GPG key
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -

# Create list file
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Install mongosh
sudo apt-get update
sudo apt-get install -y mongodb-mongosh

# Verify
mongosh --version
```

### 7.3 Database GUI Tools (Optional)

**DBeaver (Universal Database Tool):**
```bash
# macOS
brew install --cask dbeaver-community

# Linux - download from https://dbeaver.io/download/
```

**MongoDB Compass (MongoDB GUI):**
- Download from [MongoDB Compass](https://www.mongodb.com/products/compass)

## 🔌 Step 8: Install API Testing Tools

### 8.1 Postman

**Download and Install:**
- Visit [Postman Downloads](https://www.postman.com/downloads/)
- Install for your OS
- Create a free account

**Alternative: cURL (Command Line)**
```bash
# Already installed on macOS and most Linux distributions
curl --version

# If not installed on Linux:
sudo apt install curl
```

### 8.2 HTTPie (Modern cURL Alternative)

```bash
# macOS
brew install httpie

# Linux
sudo apt install httpie

# Python (all platforms)
pip3 install httpie

# Verify
http --version
```

## 🔐 Step 9: Set Up Git and GitHub

### 9.1 Install Git

**macOS:**
```bash
# Git comes pre-installed, but update via Homebrew
brew install git

# Verify
git --version
```

**Linux:**
```bash
sudo apt install git
git --version
```

### 9.2 Configure Git

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Verify
git config --list
```

### 9.3 Set Up SSH Key for GitHub

```bash
# Generate SSH key
ssh-keygen -t ed25519 -C "your.email@example.com"

# Press Enter to accept default location
# Enter passphrase (optional but recommended)

# Start ssh-agent
eval "$(ssh-agent -s)"

# Add SSH key to agent
ssh-add ~/.ssh/id_ed25519

# Copy public key
cat ~/.ssh/id_ed25519.pub
# Copy the entire output
```

**Add SSH Key to GitHub:**
1. Go to GitHub → Settings → SSH and GPG keys
2. Click "New SSH key"
3. Paste your public key
4. Save

**Test Connection:**
```bash
ssh -T git@github.com
# Should see: "Hi username! You've successfully authenticated..."
```

### 9.4 Create GitHub Repository

1. Go to [GitHub](https://github.com/new)
2. Repository name: `FinGaurd`
3. Description: "Cloud-native financial management and fraud detection system"
4. Visibility: Private (recommended) or Public
5. **DO NOT** initialize with README (we already have one)
6. Click "Create repository"

### 9.5 Initialize Local Repository and Push

```bash
cd /Users/preethamdandu/Desktop/FinGaurd

# Initialize git repository
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit: Phase 0 - Foundation complete"

# Add remote repository (replace YOUR_USERNAME)
git remote add origin git@github.com:YOUR_USERNAME/FinGaurd.git

# Push to GitHub
git branch -M main
git push -u origin main
```

## ✅ Step 10: Verify Complete Setup

Run this verification script to check all installations:

```bash
#!/bin/bash

echo "=== FinGaurd Setup Verification ==="
echo ""

# Java
echo "✓ Java:"
java -version 2>&1 | head -1

# Python
echo "✓ Python:"
python3 --version

# Node.js
echo "✓ Node.js:"
node --version

# Maven
echo "✓ Maven:"
mvn -version | head -1

# Docker
echo "✓ Docker:"
docker --version

# AWS CLI
echo "✓ AWS CLI:"
aws --version

# Terraform
echo "✓ Terraform:"
terraform --version | head -1

# PostgreSQL Client
echo "✓ PostgreSQL:"
psql --version

# MongoDB Shell
echo "✓ MongoDB Shell:"
mongosh --version

# Git
echo "✓ Git:"
git --version

echo ""
echo "=== All tools installed successfully! ==="
```

Save as `verify-setup.sh`, make executable, and run:
```bash
chmod +x verify-setup.sh
./verify-setup.sh
```

## 📚 Additional Resources

### Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [AWS Documentation](https://docs.aws.amazon.com/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Docker Documentation](https://docs.docker.com/)

### Learning Resources
- [AWS Free Tier](https://aws.amazon.com/free/)
- [AWS Training](https://aws.amazon.com/training/)
- [Spring Boot Tutorial](https://spring.io/guides)
- [FastAPI Tutorial](https://fastapi.tiangolo.com/tutorial/)

## 🆘 Troubleshooting

### Common Issues

**Issue: `java: command not found`**
```bash
# Check JAVA_HOME
echo $JAVA_HOME

# Set JAVA_HOME (add to ~/.zshrc or ~/.bashrc)
export JAVA_HOME=/path/to/java
export PATH=$JAVA_HOME/bin:$PATH
```

**Issue: Docker permission denied**
```bash
# Add user to docker group (Linux)
sudo usermod -aG docker $USER
newgrp docker
```

**Issue: AWS CLI not configured**
```bash
# Reconfigure
aws configure

# Check credentials
cat ~/.aws/credentials
```

**Issue: Terraform fails to authenticate**
```bash
# Verify AWS credentials
aws sts get-caller-identity

# Re-run terraform init
terraform init -upgrade
```

## 🎉 Next Steps

Once your environment is set up:

1. ✅ Phase 0 Complete - Foundation Ready
2. ➡️ Move to **Phase 1: Local Development**
   - Create Java Spring Boot application
   - Implement user authentication
   - Build transaction management APIs
   - Develop Python fraud detection service
   - Set up local databases

---

**Setup Status**: Environment Ready ✅  
**Last Updated**: Phase 0  
**Next Phase**: Phase 1 - Local Development

