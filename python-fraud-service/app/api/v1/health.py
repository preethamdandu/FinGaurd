"""
Health check endpoints
"""

from fastapi import APIRouter
from datetime import datetime, timezone

from app.services.mongodb_client import mongodb_client
from app.ml.model_manager import model_manager

router = APIRouter()


@router.get("/health")
async def health_check():
    """
    Health check endpoint for monitoring.
    """
    return {
        "status": "healthy",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "service": "fraud-detection",
    }


@router.get("/ready")
async def readiness_check():
    """
    Readiness check - verifies service dependencies.
    """
    db_status = "connected" if mongodb_client.is_connected else "disconnected"
    ml_status = "loaded" if model_manager.is_loaded else "not_loaded"

    # Service is ready if at least rule-based detection is available
    # (MongoDB and ML are optional enhancements)
    overall_status = "ready"

    return {
        "status": overall_status,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "checks": {
            "database": db_status,
            "ml_model": ml_status,
            "fraud_rules": "active",
        },
    }
