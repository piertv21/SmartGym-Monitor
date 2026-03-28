from flask import Blueprint, flash, redirect, render_template, request, session, url_for
import requests

from smartgym_flask.extensions import get_user_service
from smartgym_flask.services.user_service import Credentials

auth_bp = Blueprint("auth", __name__)


def _ensure_gateway_token() -> str | None:
    token = session.get("gateway_token")
    if token:
        return token

    try:
        token = get_user_service().generate_gateway_token()
    except requests.RequestException as ex:
        flash(f"Gateway token unavailable: {ex}", "error")
        return None

    session["gateway_token"] = token
    return token


@auth_bp.get("/login")
def login():
    _ensure_gateway_token()
    if session.get("user"):
        return redirect(url_for("dashboard.dashboard"))
    return render_template("login.html")

@auth_bp.post("/login")
def login_post():
    username = (request.form.get("username") or "").strip()
    password = request.form.get("password") or ""

    if not username or not password:
        flash("Username and password are required.", "error")
        return redirect(url_for("auth.login"))

    gateway_token = _ensure_gateway_token()
    if not gateway_token:
        return redirect(url_for("auth.login"))

    creds = Credentials(username=username, password=password)

    try:
        response = get_user_service().login(creds, gateway_token)
    except requests.RequestException as ex:
        flash(f"Auth service unreachable: {ex}", "error")
        return redirect(url_for("auth.login"))

    if response.status_code == 200:
        session["user"] = creds.username
        flash("Login successful.", "success")
        return redirect(url_for("dashboard.dashboard"))

    try:
        error = response.json().get("error", "Unauthorized")
    except Exception:
        error = "Unauthorized"

    flash(error, "error")
    return redirect(url_for("auth.login"))


@auth_bp.get("/logout")
def logout():
    username = session.get("user")
    gateway_token = session.get("gateway_token")
    session.clear()

    if username and gateway_token:
        try:
            get_user_service().logout(username, gateway_token)
        except requests.RequestException:
            pass

    flash("Logged out.", "success")
    return redirect(url_for("auth.login"))

