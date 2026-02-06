"""
Fraud Detection Service

Implements rule-based and ML-assisted fraud detection for financial transactions.
"""

import logging
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from typing import Dict, List, Optional, Tuple
from uuid import UUID

from app.core.config import settings

logger = logging.getLogger(__name__)


class FraudDetectionService:
    """
    Comprehensive fraud detection service combining:
    - Rule-based heuristics (always active)
    - Transaction velocity monitoring
    - Amount anomaly detection
    - Time-of-day risk analysis
    - Category risk scoring
    """

    # In-memory tracking for transaction velocity per user
    _user_transactions: Dict[str, List[datetime]] = {}

    # Risk weights for combining factors
    WEIGHT_AMOUNT = 0.30
    WEIGHT_TIME = 0.15
    WEIGHT_VELOCITY = 0.25
    WEIGHT_CATEGORY = 0.15
    WEIGHT_PATTERN = 0.15

    # Thresholds
    HIGH_AMOUNT_THRESHOLD = Decimal("5000.00")
    VERY_HIGH_AMOUNT_THRESHOLD = Decimal("25000.00")
    VELOCITY_WINDOW_SECONDS = 60
    VELOCITY_MAX_TRANSACTIONS = 3

    # High-risk categories
    HIGH_RISK_CATEGORIES = {
        "cryptocurrency", "gambling", "adult services",
        "cash advance", "international transfer", "wire transfer",
    }

    MEDIUM_RISK_CATEGORIES = {
        "investment", "foreign exchange", "money order",
        "peer transfer", "gift card",
    }

    def analyze_transaction(
        self,
        transaction_id: Optional[UUID],
        user_id: str,
        amount: Decimal,
        transaction_type: str = "EXPENSE",
        category: str = "",
        transaction_date: Optional[datetime] = None,
        description: Optional[str] = None,
    ) -> dict:
        """
        Analyze a single transaction for fraud.

        Returns a dict with:
            - risk_score (float): 0.0 to 1.0
            - is_fraud (bool): True if risk_score >= threshold
            - detected_anomalies (list[str]): Human-readable risk factors
            - confidence (float): Confidence in the assessment
            - details (dict): Per-factor breakdown
        """
        try:
            if transaction_date is None:
                transaction_date = datetime.now(timezone.utc)
            elif transaction_date.tzinfo is None:
                transaction_date = transaction_date.replace(tzinfo=timezone.utc)

            anomalies: List[str] = []
            factor_scores: Dict[str, float] = {}

            # Factor 1: Amount risk
            amount_score = self._score_amount(amount)
            factor_scores["amount"] = amount_score
            if amount_score > 0.3:
                anomalies.append(f"High transaction amount: ${amount}")

            # Factor 2: Time-of-day risk
            time_score = self._score_time(transaction_date)
            factor_scores["time_of_day"] = time_score
            if time_score > 0.3:
                anomalies.append(
                    f"Suspicious transaction time: {transaction_date.strftime('%H:%M')} UTC"
                )

            # Factor 3: Transaction velocity
            velocity_score = self._score_velocity(user_id, transaction_date)
            factor_scores["velocity"] = velocity_score
            if velocity_score > 0.3:
                anomalies.append("Rapid transaction activity detected")

            # Factor 4: Category risk
            category_score = self._score_category(category)
            factor_scores["category"] = category_score
            if category_score > 0.3:
                anomalies.append(f"High-risk category: {category}")

            # Factor 5: Pattern / description analysis
            pattern_score = self._score_pattern(description, amount)
            factor_scores["pattern"] = pattern_score
            if pattern_score > 0.3:
                anomalies.append("Suspicious transaction pattern")

            # Weighted combination
            risk_score = (
                self.WEIGHT_AMOUNT * amount_score
                + self.WEIGHT_TIME * time_score
                + self.WEIGHT_VELOCITY * velocity_score
                + self.WEIGHT_CATEGORY * category_score
                + self.WEIGHT_PATTERN * pattern_score
            )

            # Clamp to [0, 1]
            risk_score = max(0.0, min(1.0, risk_score))

            is_fraud = risk_score >= settings.FRAUD_THRESHOLD

            # Confidence is higher when more factors agree
            non_zero_factors = sum(1 for s in factor_scores.values() if s > 0.1)
            if is_fraud:
                confidence = min(0.99, 0.6 + (non_zero_factors * 0.08))
            else:
                confidence = min(0.99, 0.7 + ((5 - non_zero_factors) * 0.06))

            result = {
                "transaction_id": str(transaction_id) if transaction_id else None,
                "risk_score": round(risk_score, 4),
                "is_fraud": is_fraud,
                "detected_anomalies": anomalies,
                "confidence": round(confidence, 4),
                "model_version": settings.ML_MODEL_VERSION,
                "details": factor_scores,
            }

            logger.info(
                "Transaction %s analyzed: risk_score=%.4f, is_fraud=%s, anomalies=%d",
                transaction_id,
                risk_score,
                is_fraud,
                len(anomalies),
            )

            return result

        except Exception as e:
            logger.error("Error analyzing transaction %s: %s", transaction_id, e)
            # Fail-safe: do not flag as fraud on analysis error
            return {
                "transaction_id": str(transaction_id) if transaction_id else None,
                "risk_score": 0.0,
                "is_fraud": False,
                "detected_anomalies": ["Analysis error occurred"],
                "confidence": 0.0,
                "model_version": settings.ML_MODEL_VERSION,
                "details": {},
            }

    def analyze_legacy(
        self, user_id: int, amount: float, timestamp: str
    ) -> dict:
        """
        Legacy /detect endpoint compatibility.
        
        Accepts the simpler payload the Java service sends and returns
        the format it expects (is_fraudulent, reason, risk_score).
        """
        try:
            # Parse ISO-8601 timestamp
            ts = timestamp
            if ts.endswith("Z"):
                ts = ts.replace("Z", "+00:00")
            try:
                dt = datetime.fromisoformat(ts)
            except Exception:
                dt = datetime.now(timezone.utc)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=timezone.utc)

            result = self.analyze_transaction(
                transaction_id=None,
                user_id=str(user_id),
                amount=Decimal(str(amount)),
                transaction_date=dt,
            )

            # Map to legacy response format
            reason = None
            if result["detected_anomalies"]:
                reason = "; ".join(result["detected_anomalies"])

            return {
                "is_fraudulent": result["is_fraud"],
                "reason": reason,
                "risk_score": result["risk_score"],
            }

        except Exception as e:
            logger.error("Error in legacy fraud analysis: %s", e)
            return {
                "is_fraudulent": False,
                "reason": "Analysis error",
                "risk_score": 0.0,
            }

    # ---- Private scoring methods ----

    def _score_amount(self, amount: Decimal) -> float:
        """Score risk based on transaction amount."""
        if amount >= self.VERY_HIGH_AMOUNT_THRESHOLD:
            return 1.0
        elif amount >= self.HIGH_AMOUNT_THRESHOLD:
            # Linear scale from 0.5 to 1.0 between thresholds
            range_size = float(
                self.VERY_HIGH_AMOUNT_THRESHOLD - self.HIGH_AMOUNT_THRESHOLD
            )
            position = float(amount - self.HIGH_AMOUNT_THRESHOLD)
            return 0.5 + (position / range_size) * 0.5
        elif amount >= Decimal("1000.00"):
            # Moderate risk for amounts $1000-$5000
            return float(amount - Decimal("1000.00")) / 4000.0 * 0.3
        return 0.0

    def _score_time(self, dt: datetime) -> float:
        """Score risk based on time of day (UTC)."""
        hour = dt.hour
        # Highest risk: midnight to 5 AM
        if 0 <= hour < 5:
            return 0.8
        # Moderate risk: 5-6 AM and 11 PM-midnight
        elif hour == 5 or hour == 23:
            return 0.4
        return 0.0

    def _score_velocity(self, user_id: str, now: datetime) -> float:
        """Score risk based on transaction velocity."""
        cutoff = now - timedelta(seconds=self.VELOCITY_WINDOW_SECONDS)

        history = self._user_transactions.get(user_id, [])
        # Prune old entries
        history = [t for t in history if t > cutoff]
        history.append(now)
        self._user_transactions[user_id] = history

        count = len(history)
        if count > self.VELOCITY_MAX_TRANSACTIONS * 2:
            return 1.0
        elif count > self.VELOCITY_MAX_TRANSACTIONS:
            # Scale between threshold and 2x threshold
            excess = count - self.VELOCITY_MAX_TRANSACTIONS
            return min(1.0, 0.5 + (excess / self.VELOCITY_MAX_TRANSACTIONS) * 0.5)
        return 0.0

    def _score_category(self, category: str) -> float:
        """Score risk based on transaction category."""
        cat_lower = (category or "").lower().strip()
        if cat_lower in self.HIGH_RISK_CATEGORIES:
            return 0.9
        elif cat_lower in self.MEDIUM_RISK_CATEGORIES:
            return 0.4
        return 0.0

    def _score_pattern(self, description: Optional[str], amount: Decimal) -> float:
        """Score risk based on transaction pattern/description heuristics."""
        score = 0.0

        if description:
            desc_lower = description.lower()
            # Suspicious keywords
            suspicious_words = [
                "urgent", "wire", "bitcoin", "crypto", "offshore",
                "anonymous", "untraceable", "dark", "hack",
            ]
            matches = sum(1 for word in suspicious_words if word in desc_lower)
            if matches >= 2:
                score = 0.8
            elif matches == 1:
                score = 0.4

        # Round-number amounts are slightly more suspicious (common in fraud)
        if amount > Decimal("100") and amount == amount.to_integral_value():
            score = max(score, 0.15)

        return min(1.0, score)


# Singleton instance
fraud_service = FraudDetectionService()
