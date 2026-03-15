import os


class Config:
    SECRET_KEY = os.getenv("FLASK_SECRET_KEY", "dev")
    AUTH_SERVICE_BASE_URL = os.getenv("AUTH_SERVICE_BASE_URL", "http://localhost:8081")
    AUTH_TIMEOUT_SECONDS = float(os.getenv("AUTH_TIMEOUT_SECONDS", "5"))


class TestingConfig(Config):
    TESTING = True
    WTF_CSRF_ENABLED = False

