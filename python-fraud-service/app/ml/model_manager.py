"""
ML Model Manager for fraud detection.

Manages the lifecycle of the Isolation Forest anomaly detection model,
including loading, prediction, and retraining.
"""

import logging
import os
from typing import Any, Dict, List, Optional

import numpy as np

from app.core.config import settings

logger = logging.getLogger(__name__)

# Optional imports for ML
try:
    import joblib
    from sklearn.ensemble import IsolationForest
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False
    logger.warning("scikit-learn not available; ML model features disabled")


class ModelManager:
    """
    Manages the Isolation Forest model for anomaly-based fraud detection.
    
    Features used:
    - amount: Transaction amount
    - hour_of_day: Hour of the transaction (0-23)
    - day_of_week: Day of the week (0-6)
    - amount_log: Log-transformed amount for better distribution
    """

    FEATURE_NAMES = [
        "amount",
        "hour_of_day",
        "day_of_week",
        "amount_log",
    ]

    def __init__(self):
        self._model: Optional[Any] = None
        self._loaded = False
        self._version = settings.ML_MODEL_VERSION

    @property
    def is_loaded(self) -> bool:
        return self._loaded

    @property
    def version(self) -> str:
        return self._version

    async def load_model(self) -> bool:
        """
        Load the trained model from disk, or create a default model
        if no trained model exists.
        """
        if not SKLEARN_AVAILABLE:
            logger.info("ML model disabled (scikit-learn not installed)")
            return False

        model_path = settings.ML_MODEL_PATH

        if os.path.exists(model_path):
            try:
                self._model = joblib.load(model_path)
                self._loaded = True
                logger.info("ML model loaded from %s", model_path)
                return True
            except Exception as e:
                logger.error("Failed to load model from %s: %s", model_path, e)

        # Create a default untrained model
        logger.info("No pre-trained model found; creating default Isolation Forest")
        self._model = IsolationForest(
            n_estimators=100,
            contamination=0.05,
            random_state=42,
            n_jobs=-1,
        )

        # Train on synthetic normal data so the model can make predictions
        self._train_on_synthetic_data()
        self._loaded = True

        # Save the default model
        self._save_model()

        return True

    def predict(self, features: Dict[str, float]) -> Optional[float]:
        """
        Predict anomaly score for a single transaction.
        
        Returns a score between 0 and 1 where higher = more anomalous,
        or None if the model is not available.
        """
        if not self._loaded or self._model is None:
            return None

        try:
            feature_vector = np.array(
                [[features.get(name, 0.0) for name in self.FEATURE_NAMES]]
            )
            # Isolation Forest returns -1 for anomalies, 1 for normal
            raw_score = self._model.decision_function(feature_vector)[0]
            # Convert to 0-1 scale: more negative = higher anomaly score
            # decision_function returns values roughly in [-0.5, 0.5] range
            anomaly_score = max(0.0, min(1.0, 0.5 - raw_score))
            return round(anomaly_score, 4)

        except Exception as e:
            logger.error("ML prediction error: %s", e)
            return None

    def extract_features(
        self,
        amount: float,
        hour_of_day: int,
        day_of_week: int,
    ) -> Dict[str, float]:
        """
        Extract the feature vector for a transaction.
        """
        return {
            "amount": amount,
            "hour_of_day": float(hour_of_day),
            "day_of_week": float(day_of_week),
            "amount_log": float(np.log1p(abs(amount))),
        }

    async def retrain(self, training_data: List[Dict[str, float]]) -> bool:
        """
        Retrain the model with new data.
        """
        if not SKLEARN_AVAILABLE:
            logger.warning("Cannot retrain: scikit-learn not available")
            return False

        if len(training_data) < settings.MIN_TRANSACTIONS_FOR_TRAINING:
            logger.warning(
                "Not enough training data: %d (need %d)",
                len(training_data),
                settings.MIN_TRANSACTIONS_FOR_TRAINING,
            )
            return False

        try:
            X = np.array(
                [[d.get(name, 0.0) for name in self.FEATURE_NAMES] for d in training_data]
            )

            self._model = IsolationForest(
                n_estimators=100,
                contamination=0.05,
                random_state=42,
                n_jobs=-1,
            )
            self._model.fit(X)
            self._loaded = True

            self._save_model()
            logger.info("Model retrained with %d samples", len(training_data))
            return True

        except Exception as e:
            logger.error("Model retraining failed: %s", e)
            return False

    def get_model_info(self) -> Dict[str, Any]:
        """Get information about the current model."""
        return {
            "model_version": self._version,
            "model_type": "Isolation Forest",
            "is_loaded": self._loaded,
            "features": self.FEATURE_NAMES,
            "threshold": settings.FRAUD_THRESHOLD,
            "sklearn_available": SKLEARN_AVAILABLE,
        }

    def _train_on_synthetic_data(self):
        """Train on synthetic normal transaction data."""
        rng = np.random.RandomState(42)
        n_samples = 1000

        # Simulate normal transactions
        amounts = rng.lognormal(mean=4.0, sigma=1.5, size=n_samples)  # ~$55 median
        hours = rng.choice(range(8, 22), size=n_samples)  # Business hours
        days = rng.choice(range(0, 7), size=n_samples)
        amount_logs = np.log1p(amounts)

        X = np.column_stack([amounts, hours, days, amount_logs])
        self._model.fit(X)
        logger.info("Model trained on %d synthetic samples", n_samples)

    def _save_model(self):
        """Save model to disk."""
        try:
            model_dir = os.path.dirname(settings.ML_MODEL_PATH)
            if model_dir:
                os.makedirs(model_dir, exist_ok=True)
            joblib.dump(self._model, settings.ML_MODEL_PATH)
            logger.info("Model saved to %s", settings.ML_MODEL_PATH)
        except Exception as e:
            logger.error("Failed to save model: %s", e)


# Singleton instance
model_manager = ModelManager()
