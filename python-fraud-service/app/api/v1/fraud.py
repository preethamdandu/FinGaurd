"""
Fraud detection API endpoints
"""

from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional
from decimal import Decimal
from datetime import datetime
import logging

from app.schemas.transaction import TransactionAnalysisRequest, TransactionAnalysisResponse
from app.schemas.fraud import FraudAnalysisResult
from app.services.fraud_detector import fraud_service
from app.services.mongodb_client import mongodb_client
from app.ml.model_manager import model_manager
from app.core.config import settings

logger = logging.getLogger(__name__)
router = APIRouter()


# ----- Legacy schema (used by the Java service's /detect call) -----

class LegacyTransactionRequest(BaseModel):
    """Matches what the Java service sends to /detect."""
    user_id: int
    amount: float = Field(..., gt=0)
    timestamp: str = Field(..., description="ISO-8601 timestamp")


class LegacyFraudResponse(BaseModel):
    is_fraudulent: bool
    reason: Optional[str] = None
    risk_score: float = 0.0


# ----- Endpoints -----

@router.post("/analyze", response_model=TransactionAnalysisResponse)
async def analyze_transaction(request: TransactionAnalysisRequest):
    """
    Analyze a single transaction for fraud.

    Uses a multi-factor scoring system:
    - Amount risk analysis
    - Time-of-day risk
    - Transaction velocity monitoring
    - Category risk assessment
    - Pattern/description heuristics
    - ML anomaly detection (Isolation Forest)
    """
    try:
        logger.info("Analyzing transaction: %s", request.transaction_id)

        # Run rule-based fraud detection
        result = fraud_service.analyze_transaction(
            transaction_id=request.transaction_id,
            user_id=str(request.user_id),
            amount=request.amount,
            transaction_type=request.transaction_type,
            category=request.category,
            transaction_date=request.transaction_date,
            description=request.description,
        )

        # Optionally blend in ML model score
        if model_manager.is_loaded:
            features = model_manager.extract_features(
                amount=float(request.amount),
                hour_of_day=request.transaction_date.hour if request.transaction_date else 12,
                day_of_week=request.transaction_date.weekday() if request.transaction_date else 0,
            )
            ml_score = model_manager.predict(features)
            if ml_score is not None:
                # Blend: 70% rule-based, 30% ML
                blended = result["risk_score"] * 0.7 + ml_score * 0.3
                result["risk_score"] = round(blended, 4)
                result["is_fraud"] = result["risk_score"] >= settings.FRAUD_THRESHOLD

        # Build response
        response = TransactionAnalysisResponse(
            transaction_id=request.transaction_id,
            risk_score=result["risk_score"],
            is_fraud=result["is_fraud"],
            detected_anomalies=result["detected_anomalies"],
            confidence=result["confidence"],
            model_version=result["model_version"],
        )

        # Persist to MongoDB (non-blocking fire-and-forget)
        try:
            await mongodb_client.log_analysis(result)
            if result["is_fraud"]:
                await mongodb_client.log_alert(result)
        except Exception:
            pass  # Don't let logging failures affect the response

        logger.info(
            "Transaction %s analyzed: risk_score=%.4f, is_fraud=%s",
            request.transaction_id,
            response.risk_score,
            response.is_fraud,
        )
        return response

    except Exception as e:
        logger.error("Error analyzing transaction: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to analyze transaction",
        )


@router.post("/batch", response_model=List[TransactionAnalysisResponse])
async def analyze_batch(requests: List[TransactionAnalysisRequest]):
    """
    Analyze multiple transactions in batch.
    """
    try:
        logger.info("Analyzing batch of %d transactions", len(requests))

        results = []
        for req in requests:
            result = fraud_service.analyze_transaction(
                transaction_id=req.transaction_id,
                user_id=str(req.user_id),
                amount=req.amount,
                transaction_type=req.transaction_type,
                category=req.category,
                transaction_date=req.transaction_date,
                description=req.description,
            )

            response = TransactionAnalysisResponse(
                transaction_id=req.transaction_id,
                risk_score=result["risk_score"],
                is_fraud=result["is_fraud"],
                detected_anomalies=result["detected_anomalies"],
                confidence=result["confidence"],
                model_version=result["model_version"],
            )
            results.append(response)

            # Log to MongoDB
            try:
                await mongodb_client.log_analysis(result)
                if result["is_fraud"]:
                    await mongodb_client.log_alert(result)
            except Exception:
                pass

        return results

    except Exception as e:
        logger.error("Error analyzing batch: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to analyze batch",
        )


@router.get("/models")
async def get_model_info():
    """
    Get information about the current fraud detection model.
    """
    return model_manager.get_model_info()


@router.post("/train")
async def trigger_training():
    """
    Trigger model retraining (admin only).
    """
    logger.info("Model training triggered")

    if not model_manager.is_loaded:
        return {
            "status": "error",
            "message": "ML model is not available. Ensure scikit-learn is installed.",
        }

    # In a real system this would be a background job pulling data from the DB.
    # For now, we re-initialize with the synthetic baseline.
    success = await model_manager.load_model()

    return {
        "status": "training_completed" if success else "training_failed",
        "message": "Model has been retrained" if success else "Training failed",
        "model_info": model_manager.get_model_info(),
    }
