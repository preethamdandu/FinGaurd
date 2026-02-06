"""
Transaction-related Pydantic schemas
"""

from pydantic import BaseModel, Field, UUID4
from typing import Optional, List
from datetime import datetime, timezone
from decimal import Decimal


class TransactionAnalysisRequest(BaseModel):
    """
    Request model for transaction fraud analysis
    """
    transaction_id: UUID4 = Field(..., description="Unique transaction identifier")
    user_id: UUID4 = Field(..., description="User ID who made the transaction")
    amount: Decimal = Field(..., gt=0, description="Transaction amount")
    transaction_type: str = Field(..., description="INCOME or EXPENSE")
    category: str = Field(..., description="Transaction category")
    transaction_date: datetime = Field(..., description="Transaction timestamp")
    description: Optional[str] = Field(None, description="Transaction description")
    
    class Config:
        json_schema_extra = {
            "example": {
                "transaction_id": "123e4567-e89b-12d3-a456-426614174000",
                "user_id": "987e6543-e21b-12d3-a456-426614174111",
                "amount": 150.00,
                "transaction_type": "EXPENSE",
                "category": "groceries",
                "transaction_date": "2025-10-09T14:30:00Z",
                "description": "Weekly grocery shopping"
            }
        }


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


class TransactionAnalysisResponse(BaseModel):
    """
    Response model for transaction fraud analysis
    """
    transaction_id: UUID4 = Field(..., description="Transaction identifier")
    risk_score: float = Field(..., ge=0, le=1, description="Fraud risk score (0-1)")
    is_fraud: bool = Field(..., description="Whether transaction is flagged as fraud")
    detected_anomalies: List[str] = Field(
        default_factory=list,
        description="List of detected anomalies"
    )
    confidence: float = Field(..., ge=0, le=1, description="Model confidence score")
    model_version: str = Field(..., description="Version of ML model used")
    analyzed_at: datetime = Field(default_factory=_utc_now)
    
    class Config:
        json_schema_extra = {
            "example": {
                "transaction_id": "123e4567-e89b-12d3-a456-426614174000",
                "risk_score": 0.15,
                "is_fraud": False,
                "detected_anomalies": [],
                "confidence": 0.85,
                "model_version": "1.0.0",
                "analyzed_at": "2025-10-09T14:30:05Z"
            }
        }

