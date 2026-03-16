from smartgym_flask import create_app


def test_api_health_returns_ok():
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    response = client.get("/api/health")

    assert response.status_code == 200
    assert response.get_json() == {"status": "ok"}

