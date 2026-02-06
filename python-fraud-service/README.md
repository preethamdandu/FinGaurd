# FinGaurd Fraud Detection Service (Python)

FastAPI microservice for machine learning-based fraud detection on financial transactions.

## 📋 Overview

This service provides intelligent fraud detection capabilities:
- Real-time transaction analysis using machine learning
- Anomaly detection with Isolation Forest algorithm
- Risk scoring and fraud flagging
- Transaction logging to MongoDB
- RESTful API for integration with Java core service

## 🏗️ Project Structure

```
python-fraud-service/
├── app/
│   ├── __init__.py
│   ├── main.py                  # FastAPI application entry point
│   ├── api/                     # API routes
│   │   ├── __init__.py
│   │   └── v1/
│   │       ├── __init__.py
│   │       ├── fraud.py         # Fraud detection endpoints
│   │       └── health.py        # Health check endpoints
│   ├── core/                    # Core application components
│   │   ├── __init__.py
│   │   ├── config.py            # Configuration settings
│   │   └── logging_config.py   # Logging setup
│   ├── schemas/                 # Pydantic models
│   │   ├── __init__.py
│   │   ├── transaction.py
│   │   └── fraud.py
│   ├── services/                # Business logic
│   │   ├── __init__.py
│   │   ├── fraud_detector.py   # Main fraud detection service
│   │   └── mongodb_client.py   # MongoDB connection
│   ├── ml/                      # Machine learning components
│   │   ├── __init__.py
│   │   ├── model_manager.py    # Model loading/saving
│   │   ├── feature_engineering.py
│   │   └── trainer.py          # Model training
│   └── utils/                   # Utility functions
│       └── __init__.py
├── models/                      # Saved ML models
│   └── .gitkeep
├── tests/                       # Unit and integration tests
│   ├── __init__.py
│   ├── test_api/
│   └── test_services/
├── requirements.txt             # Python dependencies
├── pyproject.toml              # Poetry configuration
├── Dockerfile
├── .env.example
└── README.md
```

## 🚀 Getting Started

### Prerequisites

- Python 3.10 or higher
- pip or Poetry
- MongoDB 6.0+ (or Docker)
- Virtual environment tool (venv, virtualenv, or conda)

### Local Development Setup

1. **Navigate to service directory**
```bash
cd python-fraud-service
```

2. **Create virtual environment**
```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies**

Using pip:
```bash
pip install -r requirements.txt
```

Using Poetry:
```bash
poetry install
poetry shell
```

4. **Set up environment variables**
```bash
cp .env.example .env
# Edit .env with your configuration
```

5. **Start MongoDB (using Docker)**
```bash
docker run -d \
  --name fingaurd-mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_DATABASE=fingaurd_fraud \
  mongo:7-jammy
```

6. **Run the application**
```bash
# Development mode with auto-reload
uvicorn app.main:app --reload --port 8000

# Or using Python directly
python -m app.main
```

The service will start on `http://localhost:8000`

### Using Docker

1. **Build Docker image**
```bash
docker build -t fingaurd-fraud-service:latest .
```

2. **Run container**
```bash
docker run -d \
  --name fingaurd-fraud \
  -p 8000:8000 \
  -e MONGODB_URL=mongodb://host.docker.internal:27017 \
  fingaurd-fraud-service:latest
```

## 📡 API Endpoints

### Health & Status
```
GET    /                     - Service information
GET    /api/health           - Health check
GET    /api/ready            - Readiness check
```

### Fraud Detection
```
POST   /api/fraud/analyze    - Analyze single transaction
POST   /api/fraud/batch      - Analyze multiple transactions
GET    /api/fraud/models     - Get model information
POST   /api/fraud/train      - Trigger model retraining
```

### API Documentation (Development)
```
GET    /docs                 - Swagger UI
GET    /redoc                - ReDoc documentation
GET    /api-docs             - OpenAPI JSON schema
```

## 🔍 API Usage Examples

### Analyze Transaction

```bash
curl -X POST "http://localhost:8000/api/fraud/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "123e4567-e89b-12d3-a456-426614174000",
    "user_id": "987e6543-e21b-12d3-a456-426614174111",
    "amount": 150.00,
    "transaction_type": "EXPENSE",
    "category": "groceries",
    "transaction_date": "2025-10-09T14:30:00Z",
    "description": "Weekly grocery shopping"
  }'
```

**Response:**
```json
{
  "transaction_id": "123e4567-e89b-12d3-a456-426614174000",
  "risk_score": 0.15,
  "is_fraud": false,
  "detected_anomalies": [],
  "confidence": 0.85,
  "model_version": "1.0.0",
  "analyzed_at": "2025-10-09T14:30:05Z"
}
```

### Get Model Information

```bash
curl -X GET "http://localhost:8000/api/fraud/models"
```

**Response:**
```json
{
  "model_version": "1.0.0",
  "model_type": "Isolation Forest",
  "last_trained": "2025-10-01T00:00:00Z",
  "features": [
    "amount",
    "hour_of_day",
    "day_of_week",
    "transaction_velocity",
    "amount_zscore"
  ],
  "threshold": 0.7
}
```

## 🤖 Machine Learning

### Fraud Detection Algorithm

The service uses an **Isolation Forest** algorithm for anomaly detection:

1. **Feature Engineering**
   - Transaction amount normalization
   - Temporal features (hour, day of week)
   - User spending patterns (mean, std deviation)
   - Transaction velocity (frequency)
   - Z-score calculation

2. **Anomaly Detection**
   - Isolation Forest identifies outliers
   - Statistical thresholds (3-sigma rule)
   - Pattern-based rules

3. **Risk Scoring**
   - Score range: 0.0 (safe) to 1.0 (high risk)
   - Threshold: 0.7+ triggers fraud flag
   - Confidence score provided

### Model Training

To retrain the model:

```bash
curl -X POST "http://localhost:8000/api/fraud/train"
```

Model files are saved to `./models/` directory.

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `ENVIRONMENT` | Environment (development/production) | `development` |
| `PORT` | Server port | `8000` |
| `MONGODB_URL` | MongoDB connection string | `mongodb://localhost:27017` |
| `MONGODB_DB_NAME` | Database name | `fingaurd_fraud` |
| `ML_MODEL_PATH` | Path to ML model file | `./models/fraud_detector.pkl` |
| `FRAUD_THRESHOLD` | Risk score threshold | `0.7` |
| `API_KEY` | API key for authentication | `dev-api-key-change-in-production` |
| `LOG_LEVEL` | Logging level | `INFO` |

### MongoDB Collections

**fraud_logs**
```javascript
{
  _id: ObjectId,
  transaction_id: UUID,
  user_id: UUID,
  risk_score: Double,
  detected_anomalies: Array,
  features: Object,
  model_version: String,
  analyzed_at: ISODate
}
```

## 🧪 Testing

### Run all tests
```bash
pytest
```

### Run with coverage
```bash
pytest --cov=app --cov-report=html
# Open htmlcov/index.html in browser
```

### Run specific test file
```bash
pytest tests/test_api/test_fraud.py
```

## 📝 Code Quality

### Format code with Black
```bash
black app/
```

### Sort imports with isort
```bash
isort app/
```

### Lint with flake8
```bash
flake8 app/
```

### Type checking with mypy
```bash
mypy app/
```

## 🔒 Security Considerations

- API key authentication (implement in production)
- Input validation with Pydantic
- No sensitive data in logs
- MongoDB connection security
- HTTPS for production deployment

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8000/api/health
```

### Readiness Check
```bash
curl http://localhost:8000/api/ready
```

### Logs

Logs are output in JSON format (configurable):
```json
{
  "timestamp": "2025-10-09T14:30:00Z",
  "level": "INFO",
  "name": "app.api.v1.fraud",
  "message": "Transaction analyzed"
}
```

## 🐳 Docker Compose

For local development with all services:

```yaml
# See ../docker-compose.yml at project root
docker-compose up -d
```

## 🚀 Deployment

### Production Considerations

1. **Use production ASGI server**
```bash
gunicorn app.main:app \
  --workers 4 \
  --worker-class uvicorn.workers.UvicornWorker \
  --bind 0.0.0.0:8000
```

2. **Environment variables**
- Set `ENVIRONMENT=production`
- Use secure `API_KEY`
- Configure `MONGODB_URL` with authentication
- Set proper `LOG_LEVEL`

3. **Model persistence**
- Store models in persistent volume
- Version control model files
- Implement model versioning

## 🔍 Troubleshooting

### Port already in use
```bash
# Change port
export PORT=8001
uvicorn app.main:app --port 8001
```

### MongoDB connection issues
```bash
# Check MongoDB is running
docker ps | grep mongo

# Test connection
python -c "from pymongo import MongoClient; print(MongoClient('mongodb://localhost:27017').server_info())"
```

### Import errors
```bash
# Ensure virtual environment is activated
source venv/bin/activate

# Reinstall dependencies
pip install -r requirements.txt
```

## 📚 Additional Resources

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Scikit-learn Documentation](https://scikit-learn.org/)
- [MongoDB Python Driver](https://pymongo.readthedocs.io/)
- [Pydantic Documentation](https://docs.pydantic.dev/)

---

**Version:** 0.1.0  
**Last Updated:** Phase 0 - Foundation

