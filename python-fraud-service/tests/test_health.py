"""Smoke tests for public HTTP endpoints (no auth)."""

import os

# Configure before importing the app (settings load at import time).
os.environ.setdefault("ENVIRONMENT", "test")
os.environ.setdefault("MONGODB_URL", "mongodb://127.0.0.1:27017")
os.environ.setdefault("LOG_FORMAT", "text")

from fastapi.testclient import TestClient

from app.main import app


def test_health_returns_ok():
    with TestClient(app) as client:
        response = client.get("/api/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "healthy"
        assert body["service"] == "fraud-detection"


def test_ready_returns_json():
    with TestClient(app) as client:
        response = client.get("/api/ready")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "ready"
        assert "checks" in body
