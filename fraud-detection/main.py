from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime, timezone

app = FastAPI(title="FinGaurd Fraud Detection", version="1.0.0")


class Transaction(BaseModel):
    user_id: int
    amount: float = Field(..., gt=0, description="Transaction amount")
    timestamp: str = Field(..., description="ISO-8601 timestamp, e.g. 2025-10-24T22:00:00Z")


class FraudResponse(BaseModel):
    is_fraudulent: bool
    reason: Optional[str] = None
    risk_score: float = 0.0


@app.post("/detect", response_model=FraudResponse)
async def detect(transaction: Transaction):
    # Stateless rules
    # Rule 1: High single amount
    if transaction.amount > 5000:
        return FraudResponse(
            is_fraudulent=True,
            reason="High single transaction amount",
            risk_score=0.9,
        )

    # Parse timestamp (ISO-8601 with optional Z)
    ts = transaction.timestamp
    if ts.endswith("Z"):
        ts = ts.replace("Z", "+00:00")
    try:
        dt = datetime.fromisoformat(ts)
    except Exception:
        # If parsing fails, treat as non-fraudulent but indicate parsing issue
        return FraudResponse(is_fraudulent=False, reason="Timestamp parse error", risk_score=0.1)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)

    # Rule 2: Suspicious time (midnight to 5 AM) -> hours 0..4
    if 0 <= dt.hour < 5:
        return FraudResponse(
            is_fraudulent=True,
            reason="Suspicious transaction time",
            risk_score=0.6,
        )

    # Default: not fraudulent
    return FraudResponse(is_fraudulent=False)


@app.get("/health")
async def health():
    return {"status": "ok"}
