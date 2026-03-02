import os


class Config:
    """
    In SPE style, configuration is externalized via env vars (12-factor-ish),
    so the same artifact runs in different environments with no code changes.
    """

    SECRET_KEY = os.getenv("FLASK_SECRET_KEY", "dev")
    AUTH_SERVICE_BASE_URL = os.getenv("AUTH_SERVICE_BASE_URL", "http://localhost:8081")