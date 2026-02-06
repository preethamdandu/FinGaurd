"""
Fraud detection related schemas
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any
from datetime import datetime


class FraudAnalysisResult(BaseModel):
    """
    Detailed fraud analysis result
    """
    risk_score: float = Field(..., ge=0, le=1)
    is_fraud: bool
    anomaly_scores: Dict[str, float] = Field(default_factory=dict)
    features: Dict[str, Any] = Field(default_factory=dict)
    model_version: str


class ModelInfo(BaseModel):
    """
    Information about the fraud detection model
    """
    version: str
    model_type: str
    last_trained: datetime
    features: List[str]
    threshold: float
    accuracy_metrics: Dict[str, float] = Field(default_factory=dict)


class TrainingRequest(BaseModel):
    """
    Request to trigger model training
    """
    force_retrain: bool = Field(default=False)
    min_samples: int = Field(default=100)

