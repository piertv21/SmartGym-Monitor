from flask import current_app, g

from smartgym_flask.services.status_service import StatusService
from smartgym_flask.services.user_service import UserService
from smartgym_flask.services.analytics_service import AnalyticsService
from smartgym_flask.services.machine_service import MachineService
from smartgym_flask.services.area_service import AreaService
from smartgym_flask.services.tracking_service import TrackingService


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
            statuses_path=current_app.config.get(
                "EMBEDDED_STATUSES_PATH", "/embedded-service/statuses"
            ),
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


def get_area_service() -> AreaService:
    if "area_service" not in g:
        g.area_service = AreaService(
            gateway_base_url=current_app.config["GATEWAY_BASE_URL"],
            timeout=current_app.config.get("AREA_TIMEOUT_SECONDS", 5),
        )
    return g.area_service


def teardown_area_service(exception=None):
    g.pop("area_service", None)


def get_tracking_service() -> TrackingService:
    if "tracking_service" not in g:
        g.tracking_service = TrackingService(
            gateway_base_url=current_app.config["GATEWAY_BASE_URL"],
            timeout=current_app.config.get("TRACKING_TIMEOUT_SECONDS", 5),
        )
    return g.tracking_service


def teardown_tracking_service(exception=None):
    g.pop("tracking_service", None)

