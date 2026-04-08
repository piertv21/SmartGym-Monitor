from flask import current_app, g

from smartgym_flask.services.status_service import StatusService
from smartgym_flask.services.user_service import UserService
from smartgym_flask.services.analytics_service import AnalyticsService
from smartgym_flask.services.machine_service import MachineService


def get_user_service() -> UserService:
    if "user_service" not in g:
        g.user_service = UserService(
            base_url=current_app.config["AUTH_SERVICE_BASE_URL"],
            timeout=current_app.config.get("AUTH_TIMEOUT_SECONDS", 5),
        )
    return g.user_service


def teardown_user_service(exception=None):
    g.pop("user_service", None)


def get_status_service() -> StatusService:
    if "status_service" not in g:
        g.status_service = StatusService(
            gateway_base_url=current_app.config["GATEWAY_BASE_URL"],
            statuses_path=current_app.config.get("EMBEDDED_STATUSES_PATH", "/embedded-service/statuses"),
            timeout=current_app.config.get("STATUS_TIMEOUT_SECONDS", 5),
        )
    return g.status_service


def teardown_status_service(exception=None):
    g.pop("status_service", None)


def get_analytics_service() -> AnalyticsService:
    if "analytics_service" not in g:
        g.analytics_service = AnalyticsService(
            gateway_base_url=current_app.config["GATEWAY_BASE_URL"],
            timeout=current_app.config.get("ANALYTICS_TIMEOUT_SECONDS", 5),
        )
    return g.analytics_service


def teardown_analytics_service(exception=None):
    g.pop("analytics_service", None)


def get_machine_service() -> MachineService:
    if "machine_service" not in g:
        g.machine_service = MachineService(
            gateway_base_url=current_app.config["GATEWAY_BASE_URL"],
            timeout=current_app.config.get("MACHINE_TIMEOUT_SECONDS", 5),
        )
    return g.machine_service


def teardown_machine_service(exception=None):
    g.pop("machine_service", None)