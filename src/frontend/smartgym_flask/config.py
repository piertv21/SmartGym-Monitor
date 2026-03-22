import os


class Config:
    SECRET_KEY = os.getenv("FLASK_SECRET_KEY", "dev")
    AUTH_SERVICE_BASE_URL = os.getenv("AUTH_SERVICE_BASE_URL", "http://localhost:8080/auth-service")
    GATEWAY_BASE_URL = os.getenv(
        "GATEWAY_BASE_URL",
        AUTH_SERVICE_BASE_URL.rsplit("/auth-service", maxsplit=1)[0],
    )
    GATEWAY_CLIENT_ID = os.getenv("GATEWAY_CLIENT_ID", "smartgym-client")
    GATEWAY_CLIENT_SECRET = os.getenv("GATEWAY_CLIENT_SECRET", "Smart-Parking")
    AUTH_TIMEOUT_SECONDS = float(os.getenv("AUTH_TIMEOUT_SECONDS", "5"))


class TestingConfig(Config):
    TESTING = True
    WTF_CSRF_ENABLED = False

