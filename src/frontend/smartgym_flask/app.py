from __future__ import annotations

import os

from flask import Flask

from smartgym_flask.config import Config
from smartgym_flask.extensions import teardown_status_service, teardown_user_service
from smartgym_flask.routes import api_bp, auth_bp, dashboard_bp


def create_app(config_overrides: dict | None = None) -> Flask:
    app = Flask(__name__, template_folder="templates", static_folder="static")
    app.config.from_object(Config)

    if config_overrides:
        app.config.update(config_overrides)

    app.register_blueprint(auth_bp)
    app.register_blueprint(dashboard_bp)
    app.register_blueprint(api_bp)
    app.teardown_appcontext(teardown_user_service)
    app.teardown_appcontext(teardown_status_service)

    return app


app = create_app()

if __name__ == "__main__":
    port = int(os.getenv("PORT", "5001"))
    app.run(host="0.0.0.0", port=port, debug=False)

