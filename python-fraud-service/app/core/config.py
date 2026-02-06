"""
Configuration settings for the Fraud Detection Service
"""

from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    Application settings loaded from environment variables
    """
    
    # Application
    APP_NAME: str = "FinGaurd Fraud Detection Service"
    ENVIRONMENT: str = "development"  # development, staging, production
    DEBUG: bool = True
    
    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    
    # MongoDB
    MONGODB_URL: str = "mongodb://localhost:27017"
    MONGODB_DB_NAME: str = "fingaurd_fraud"
    MONGODB_MAX_CONNECTIONS: int = 100
    MONGODB_MIN_CONNECTIONS: int = 10
    
    # Machine Learning
    ML_MODEL_PATH: str = "./models/fraud_detector.pkl"
    ML_MODEL_VERSION: str = "1.0.0"
    FRAUD_THRESHOLD: float = 0.7  # Risk score threshold for flagging
    
    # Feature Engineering
    LOOKBACK_DAYS: int = 30  # Days to look back for user transaction history
    MIN_TRANSACTIONS_FOR_TRAINING: int = 100
    
    # API Security
    API_KEY: str = "dev-api-key-change-in-production"
    
    # CORS
    CORS_ORIGINS: List[str] = [
        "http://localhost:8080",
        "http://localhost:3000",
    ]
    
    # Logging
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: str = "json"
    
    # External Services
    JAVA_SERVICE_URL: str = "http://localhost:8080"
    
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
    )


# Create global settings instance
settings = Settings()

