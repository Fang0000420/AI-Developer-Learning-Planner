from fastapi.testclient import TestClient

from app.config import APP_NAME, APP_VERSION, HEALTH_STATUS
from app.main import app

client = TestClient(app)


def test_health_returns_service_status() -> None:
    response = client.get("/health", headers={"X-Request-Id": "test-request-id"})

    assert response.status_code == 200
    assert response.headers["X-Request-Id"] == "test-request-id"
    assert response.json() == {
        "service": APP_NAME,
        "status": HEALTH_STATUS,
        "version": APP_VERSION,
    }
