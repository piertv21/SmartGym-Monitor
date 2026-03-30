from flask import current_app, g

from smartgym_flask.services.user_service import UserService


def get_user_service() -> UserService:
    if "user_service" not in g:
        g.user_service = UserService(
            base_url=current_app.config["AUTH_SERVICE_BASE_URL"],
            gateway_base_url=current_app.config["GATEWAY_BASE_URL"],
            gateway_client_id=current_app.config["GATEWAY_CLIENT_ID"],
            gateway_client_secret=current_app.config["GATEWAY_CLIENT_SECRET"],
            timeout=current_app.config.get("AUTH_TIMEOUT_SECONDS", 5),
        )
    return g.user_service


def teardown_user_service(exception=None):
    g.pop("user_service", None)

