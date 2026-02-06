from datetime import datetime, timedelta, timezone
from typing import Dict, List
import logging

class FraudDetectionEngine:
    def __init__(self):
        # In-memory tracking of recent transaction timestamps per user
        self.user_transactions: Dict[str, List[datetime]] = {}
        self.logger = logging.getLogger(__name__)

    def analyze_transaction(self, user_id: str, amount: float, timestamp: str) -> Dict:
        try:
            # Parse ISO8601 timestamp; ensure timezone-aware
            trans_time = self._parse_iso_timestamp(timestamp)

            # Rule 1: high single amount
            if amount > 5000:
                return {
                    "is_fraudulent": True,
                    "reason": "High single transaction amount",
                    "risk_score": 0.9,
                }

            # Rule 2: rapid transactions in last minute
            if self._is_rapid_activity(user_id, trans_time):
                return {
                    "is_fraudulent": True,
                    "reason": "Too many transactions in the last minute",
                    "risk_score": 0.8,
                }

            # Default: not fraudulent
            return {
                "is_fraudulent": False,
                "reason": "No rules matched",
                "risk_score": 0.1,
            }
        except Exception as e:
            self.logger.error(f"Error analyzing transaction: {e}")
            # Fail-safe: do not mark fraudulent on analysis error
            return {
                "is_fraudulent": False,
                "reason": "Analysis error",
                "risk_score": 0.5,
            }

    def _is_rapid_activity(self, user_id: str, now_ts: datetime) -> bool:
        cutoff = now_ts - timedelta(minutes=1)
        history = self.user_transactions.get(user_id, [])

        # Keep only last-minute entries
        history = [t for t in history if t > cutoff]
        history.append(now_ts)
        self.user_transactions[user_id] = history

        # More than 3 transactions in the last minute → fraudulent
        return len(history) > 3

    def _parse_iso_timestamp(self, ts: str) -> datetime:
        # Accepts e.g. "2025-10-24T22:00:00Z" or with offset
        if ts.endswith("Z"):
            ts = ts.replace("Z", "+00:00")
        dt = datetime.fromisoformat(ts)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt
