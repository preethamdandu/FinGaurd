"""
MongoDB client for persisting fraud analysis results and audit logs.
"""

import logging
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from app.core.config import settings

logger = logging.getLogger(__name__)

# Optional import: gracefully degrade if motor is not available
try:
    from motor.motor_asyncio import AsyncIOMotorClient
    MOTOR_AVAILABLE = True
except ImportError:
    MOTOR_AVAILABLE = False
    logger.warning("motor package not available; MongoDB logging is disabled")


class MongoDBClient:
    """
    Async MongoDB client for fraud detection audit logging.
    
    Collections:
    - fraud_analyses: Stores every fraud analysis result
    - fraud_alerts: Stores high-risk transaction alerts
    """

    def __init__(self):
        self._client = None
        self._db = None
        self._connected = False

    async def connect(self) -> bool:
        """Establish connection to MongoDB."""
        if not MOTOR_AVAILABLE:
            logger.info("MongoDB disabled (motor not installed)")
            return False

        try:
            self._client = AsyncIOMotorClient(
                settings.MONGODB_URL,
                maxPoolSize=settings.MONGODB_MAX_CONNECTIONS,
                minPoolSize=settings.MONGODB_MIN_CONNECTIONS,
                serverSelectionTimeoutMS=5000,
            )
            # Verify connection
            await self._client.admin.command("ping")
            self._db = self._client[settings.MONGODB_DB_NAME]
            self._connected = True
            logger.info("Connected to MongoDB: %s", settings.MONGODB_DB_NAME)

            # Ensure indexes
            await self._ensure_indexes()
            return True

        except Exception as e:
            logger.warning("Failed to connect to MongoDB: %s", e)
            self._connected = False
            return False

    async def close(self):
        """Close MongoDB connection."""
        if self._client:
            self._client.close()
            self._connected = False
            logger.info("MongoDB connection closed")

    @property
    def is_connected(self) -> bool:
        return self._connected

    async def log_analysis(self, analysis_result: Dict[str, Any]) -> Optional[str]:
        """
        Persist a fraud analysis result to MongoDB.
        
        Returns the inserted document ID or None if logging fails.
        """
        if not self._connected or self._db is None:
            return None

        try:
            doc = {
                **analysis_result,
                "logged_at": datetime.now(timezone.utc),
            }
            result = await self._db.fraud_analyses.insert_one(doc)
            return str(result.inserted_id)
        except Exception as e:
            logger.error("Failed to log fraud analysis: %s", e)
            return None

    async def log_alert(self, analysis_result: Dict[str, Any]) -> Optional[str]:
        """
        Log a high-risk fraud alert for review.
        """
        if not self._connected or self._db is None:
            return None

        try:
            doc = {
                **analysis_result,
                "alert_status": "pending",
                "reviewed": False,
                "created_at": datetime.now(timezone.utc),
            }
            result = await self._db.fraud_alerts.insert_one(doc)
            logger.warning(
                "Fraud alert created: transaction=%s, risk_score=%s",
                analysis_result.get("transaction_id"),
                analysis_result.get("risk_score"),
            )
            return str(result.inserted_id)
        except Exception as e:
            logger.error("Failed to log fraud alert: %s", e)
            return None

    async def _ensure_indexes(self):
        """Create indexes for efficient querying."""
        if self._db is None:
            return
        try:
            await self._db.fraud_analyses.create_index("transaction_id")
            await self._db.fraud_analyses.create_index("logged_at")
            await self._db.fraud_analyses.create_index(
                [("risk_score", -1)],
            )
            await self._db.fraud_alerts.create_index("alert_status")
            await self._db.fraud_alerts.create_index("created_at")
            logger.info("MongoDB indexes ensured")
        except Exception as e:
            logger.warning("Failed to create MongoDB indexes: %s", e)


# Singleton instance
mongodb_client = MongoDBClient()
