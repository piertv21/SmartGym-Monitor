from flask import Blueprint, redirect, render_template, session, url_for

from smartgym_flask.extensions import get_user_service

dashboard_bp = Blueprint("dashboard", __name__)


@dashboard_bp.get("/")
def index():
    if session.get("user"):
        return redirect(url_for("dashboard.dashboard"))
    return redirect(url_for("auth.login"))


@dashboard_bp.get("/dashboard")
def dashboard():
    username = session.get("user")
    gateway_token = session.get("gateway_token")
    if not username:
        return redirect(url_for("auth.login"))
    if not gateway_token:
        return redirect(url_for("auth.login"))

    user_exists = get_user_service().user_exists(username, gateway_token)
    auth_service_base_url = get_user_service().base_url

    return render_template(
        "dashboard.html",
        username=username,
        user_exists=user_exists,
        auth_service_base_url=auth_service_base_url,
    )

