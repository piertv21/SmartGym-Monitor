from smartgym_flask import create_app


def test_login_page_is_reachable(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class DummyResponse:
        status_code = 200

        @staticmethod
        def json():
            return {"accessToken": "jwt-token-123", "tokenType": "Bearer", "expiresIn": 86400}

    monkeypatch.setattr(
        "smartgym_flask.routes.auth.get_user_service",
        lambda: type(
            "S",
            (),
            {
                "login": lambda self, credentials: DummyResponse(),
                "logout": lambda self, access_token: DummyResponse(),
                "user_exists": lambda self, username, access_token: True,
                "base_url": "http://test-auth",
            },
        )(),
    )

    response = client.get("/login")

    assert response.status_code == 200

    with client.session_transaction() as session:
        assert session.get("access_token") is None


def test_login_success_redirects_to_dashboard(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class DummyResponse:
        status_code = 200

        @staticmethod
        def json():
            return {"accessToken": "jwt-token-123", "tokenType": "Bearer", "expiresIn": 86400}

    monkeypatch.setattr(
        "smartgym_flask.routes.auth.get_user_service",
        lambda: type(
            "S",
            (),
            {
                "login": lambda self, credentials: DummyResponse(),
                "logout": lambda self, access_token: DummyResponse(),
                "user_exists": lambda self, username, access_token: True,
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

    with client.session_transaction() as session:
        assert session.get("user") == "ADMIN"
        assert session.get("access_token") == "jwt-token-123"


def test_main_pages_have_consistent_title_and_description(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    monkeypatch.setattr(
        "smartgym_flask.routes.dashboard.get_user_service",
        lambda: type(
            "S",
            (),
            {
                "user_exists": lambda self, username, access_token: True,
                "base_url": "http://test-auth",
            },
        )(),
    )

    with client.session_transaction() as session:
        session["user"] = "ADMIN"
        session["access_token"] = "jwt-token-123"

    for route in ("/dashboard", "/live", "/history"):
        response = client.get(route)
        html = response.get_data(as_text=True)

        assert response.status_code == 200
        assert 'class="page-title' in html
        assert 'class="page-description' in html


