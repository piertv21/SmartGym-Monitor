from flask import Blueprint, redirect, render_template, session, url_for
from flask import current_app

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
    access_token = session.get("access_token")
    if not username:
        return redirect(url_for("auth.login"))
    if not access_token:
        return redirect(url_for("auth.login"))

    user_exists = get_user_service().user_exists(username, access_token)
    auth_service_base_url = get_user_service().base_url

    return render_template(
        "dashboard.html",
        username=username,
        user_exists=user_exists,
        auth_service_base_url=auth_service_base_url,
        gateway_base_url=current_app.config.get("GATEWAY_BASE_URL", "http://localhost:8080"),
    )


@dashboard_bp.get("/live")
def live_monitor():
    username = session.get("user")
    access_token = session.get("access_token")
    if not username:
        return redirect(url_for("auth.login"))
    if not access_token:
        return redirect(url_for("auth.login"))

    return render_template("live_monitor.html")


@dashboard_bp.get("/history")
def history():
    username = session.get("user")
    access_token = session.get("access_token")
    if not username:
        return redirect(url_for("auth.login"))
    if not access_token:
        return redirect(url_for("auth.login"))

    return render_template("history.html")