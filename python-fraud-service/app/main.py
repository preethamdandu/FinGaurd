"""
FinGaurd Fraud Detection Service

FastAPI application for detecting fraudulent transactions using
rule-based heuristics and machine learning (Isolation Forest).
"""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional
import logging

from app.core.config import settings
from app.core.logging_config import setup_logging
from app.api.v1 import fraud, health
from app.services.fraud_detector import fraud_service
from app.services.mongodb_client import mongodb_client
from app.ml.model_manager import model_manager

# Setup logging
setup_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan event handler for startup and shutdown.
    """
    # Startup
    logger.info("Starting FinGaurd Fraud Detection Service...")
    logger.info("Environment: %s", settings.ENVIRONMENT)

    # Initialize MongoDB connection (non-critical: service works without it)
    mongo_ok = await mongodb_client.connect()
    if mongo_ok:
        logger.info("MongoDB connection established")
    else:
        logger.info("Running without MongoDB (audit logging disabled)")

    # Load ML model (non-critical: rule-based detection always works)
    ml_ok = await model_manager.load_model()
    if ml_ok:
        logger.info("ML model loaded successfully")
    else:
        logger.info("Running without ML model (rule-based detection only)")

    yield

    # Shutdown
    logger.info("Shutting down Fraud Detection Service...")
    await mongodb_client.close()
    logger.info("Shutdown complete")


# Create FastAPI application
app = FastAPI(
    title="FinGaurd Fraud Detection Service",
    description="Machine learning-based fraud detection for financial transactions",
    version="0.1.0",
    lifespan=lifespan,
    docs_url="/docs" if settings.ENVIRONMENT != "production" else None,
    redoc_url="/redoc" if settings.ENVIRONMENT != "production" else None,
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include versioned API routers
app.include_router(health.router, prefix="/api", tags=["health"])
app.include_router(fraud.router, prefix="/api/fraud", tags=["fraud"])


# ----- Legacy /detect endpoint (Java service compatibility) -----

class LegacyTransaction(BaseModel):
    user_id: int
    amount: float = Field(..., gt=0, description="Transaction amount")
    timestamp: str = Field(..., description="ISO-8601 timestamp")


class LegacyFraudResponse(BaseModel):
    is_fraudulent: bool
    reason: Optional[str] = None
    risk_score: float = 0.0


@app.post("/detect", response_model=LegacyFraudResponse)
async def legacy_detect(transaction: LegacyTransaction):
    """
    Legacy fraud detection endpoint.
    
    Maintained for backward compatibility with the Java core service
    which calls POST /detect with {user_id, amount, timestamp}.
    """
    result = fraud_service.analyze_legacy(
        user_id=transaction.user_id,
        amount=transaction.amount,
        timestamp=transaction.timestamp,
    )

    # Log to MongoDB
    try:
        await mongodb_client.log_analysis({
            "endpoint": "/detect",
            "user_id": transaction.user_id,
            **result,
        })
    except Exception:
        pass

    return LegacyFraudResponse(**result)


@app.get("/health")
async def legacy_health():
    """Legacy health endpoint (Java service compatibility)."""
    return {"status": "ok"}


# ----- Audit trail endpoint (receives logs from Java service) -----

@app.post("/api/audit")
async def receive_audit_log(entry: dict):
    """
    Receive and persist audit log entries sent by the Java service.
    Stores them in the MongoDB audit_logs collection (US-016).
    """
    try:
        if mongodb_client.is_connected and mongodb_client._db is not None:
            await mongodb_client._db.audit_logs.insert_one(entry)
    except Exception:
        pass  # best-effort
    return {"status": "received"}


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "FinGaurd Fraud Detection Service",
        "version": "0.1.0",
        "status": "running",
        "docs": "/docs" if settings.ENVIRONMENT != "production" else "disabled",
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.ENVIRONMENT == "development",
        log_level="info",
    )
