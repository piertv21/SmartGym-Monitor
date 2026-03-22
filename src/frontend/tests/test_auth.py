from smartgym_flask import create_app


def test_login_page_is_reachable(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class DummyResponse:
        status_code = 200

        @staticmethod
        def json():
            return {}

    monkeypatch.setattr(
        "smartgym_flask.routes.auth.get_user_service",
        lambda: type(
            "S",
            (),
            {
                "generate_gateway_token": lambda self: "token-123",
                "login": lambda self, credentials, gateway_token: DummyResponse(),
                "logout": lambda self, username, gateway_token: DummyResponse(),
                "user_exists": lambda self, username, gateway_token: True,
                "base_url": "http://test-auth",
            },
        )(),
    )

    response = client.get("/login")

    assert response.status_code == 200

    with client.session_transaction() as session:
        assert session.get("gateway_token") == "token-123"


def test_login_success_redirects_to_dashboard(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class DummyResponse:
        status_code = 200

        @staticmethod
        def json():
            return {}

    monkeypatch.setattr(
        "smartgym_flask.routes.auth.get_user_service",
        lambda: type(
            "S",
            (),
            {
                "generate_gateway_token": lambda self: "token-123",
                "login": lambda self, credentials, gateway_token: DummyResponse(),
                "logout": lambda self, username, gateway_token: DummyResponse(),
                "user_exists": lambda self, username, gateway_token: True,
                "base_url": "http://test-auth",
            },
        )(),
    )

    response = client.post(
        "/login",
        data={"username": "ADMIN", "password": "ADMIN"},
        follow_redirects=False,
    )

    assert response.status_code == 302
    assert response.headers["Location"].endswith("/dashboard")

