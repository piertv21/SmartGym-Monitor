from smartgym_flask import create_app


def test_api_health_returns_ok():
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    response = client.get("/api/health")

    assert response.status_code == 200
    assert response.get_json() == {"status": "ok"}


def test_api_statuses_requires_login_session():
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    response = client.get("/api/statuses")

    assert response.status_code == 401
    assert response.get_json() == {"error": "Unauthorized"}


def test_api_statuses_returns_normalized_data(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class DummyResponse:
        status_code = 200

        @staticmethod
        def json():
            return {
                "list": [
                    {
                        "map": {
                            "deviceId": "sensor-2",
                            "deviceType": "SENSOR",
                            "online": False,
                            "statusDetail": "DOWN",
                            "timeStamp": "2026-04-05T10:20:37Z",
                        }
                    },
                    {
                        "map": {
                            "deviceId": "sensor-1",
                            "deviceType": "SENSOR",
                            "online": True,
                            "statusDetail": "UP",
                            "timeStamp": "2026-04-05T10:20:37Z",
                        }
                    },
                ]
            }

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_status_service",
        lambda: type("S", (), {"fetch_statuses": lambda self, access_token: DummyResponse()})(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.get("/api/statuses")
    payload = response.get_json()

    assert response.status_code == 200
    assert payload["count"] == 2
    assert payload["statuses"][0]["deviceId"] == "sensor-1"
    assert payload["statuses"][1]["deviceId"] == "sensor-2"


def test_toggle_maintenance_returns_backend_conflict_message(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class DummyResponse:
        status_code = 409

        @staticmethod
        def json():
            return {"message": "cannot set maintenance while occupied: machine-1"}

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_machine_service",
        lambda: type("S", (), {"set_maintenance": lambda self, access_token, machine_id, active: DummyResponse()})(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.post("/api/maintenance/toggle", json={"machineId": "machine-1", "active": True})

    assert response.status_code == 409
    assert response.get_json() == {"error": "cannot set maintenance while occupied: machine-1"}