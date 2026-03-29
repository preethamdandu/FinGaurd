"""
Aggressive API tests: validation, legacy /detect, error paths, light load.
"""

import os
import uuid
from datetime import datetime, timezone

import pytest
from fastapi.testclient import TestClient

os.environ.setdefault("ENVIRONMENT", "test")
os.environ.setdefault("MONGODB_URL", "mongodb://127.0.0.1:27017")
os.environ.setdefault("LOG_FORMAT", "text")

from app.main import app  # noqa: E402


@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c


def test_root_lists_service(client):
    r = client.get("/")
    assert r.status_code == 200
    assert r.json()["status"] == "running"


def test_legacy_health(client):
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json().get("status") == "ok"


def test_detect_rejects_non_positive_amount(client):
    r = client.post("/detect", json={"user_id": 1, "amount": 0, "timestamp": "2025-01-01T12:00:00Z"})
    assert r.status_code == 422


def test_detect_rejects_missing_fields(client):
    r = client.post("/detect", json={"user_id": 1})
    assert r.status_code == 422


def test_detect_accepts_valid_payload(client):
    r = client.post(
        "/detect",
        json={"user_id": 42, "amount": 99.5, "timestamp": datetime.now(timezone.utc).isoformat()},
    )
    assert r.status_code == 200
    body = r.json()
    assert "is_fraudulent" in body
    assert "risk_score" in body
    assert isinstance(body["risk_score"], (int, float))
    assert 0 <= float(body["risk_score"]) <= 1


def test_audit_accepts_payload(client):
    r = client.post("/api/audit", json={"event": "test", "detail": "brutal"})
    assert r.status_code == 200
    assert r.json().get("status") == "received"


def test_analyze_requires_valid_uuid(client):
    r = client.post(
        "/api/fraud/analyze",
        json={
            "transaction_id": "not-a-uuid",
            "user_id": str(uuid.uuid4()),
            "amount": "10.00",
            "transaction_type": "EXPENSE",
            "category": "other",
            "transaction_date": datetime.now(timezone.utc).isoformat(),
        },
    )
    assert r.status_code == 422


def test_analyze_happy_path(client):
    tid = str(uuid.uuid4())
    uid = str(uuid.uuid4())
    r = client.post(
        "/api/fraud/analyze",
        json={
            "transaction_id": tid,
            "user_id": uid,
            "amount": "250.00",
            "transaction_type": "EXPENSE",
            "category": "dining",
            "transaction_date": datetime.now(timezone.utc).isoformat(),
            "description": "integration stress",
        },
    )
    assert r.status_code == 200
    body = r.json()
    assert body["transaction_id"] == tid
    assert "risk_score" in body


def test_rapid_sequential_detect_calls(client):
    """Hammer /detect sequentially (TestClient is not thread-safe)."""
    for i in range(64):
        r = client.post(
            "/detect",
            json={
                "user_id": i,
                "amount": 10.0 + (i % 50),
                "timestamp": datetime.now(timezone.utc).isoformat(),
            },
        )
        assert r.status_code == 200, r.text


def test_batch_analyze_empty_list(client):
    r = client.post("/api/fraud/batch", json=[])
    assert r.status_code == 200
    assert r.json() == []
