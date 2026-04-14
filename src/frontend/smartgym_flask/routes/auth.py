from flask import Blueprint, flash, redirect, render_template, request, session, url_for
import requests

from smartgym_flask.extensions import get_user_service
from smartgym_flask.services.user_service import Credentials

auth_bp = Blueprint("auth", __name__)


def _unwrap_payload(response: requests.Response) -> dict:
    data = response.json()
    if isinstance(data, dict) and isinstance(data.get("map"), dict):
        return data["map"]
    return data if isinstance(data, dict) else {}


@auth_bp.get("/login")
def login():
    if session.get("user") and session.get("access_token"):
        return redirect(url_for("dashboard.dashboard"))
    return render_template("login.html")


@auth_bp.post("/login")
def login_post():
    username = (request.form.get("username") or "").strip()
    password = request.form.get("password") or ""

    if not username or not password:
        flash("Username and password are required.", "error")
        return redirect(url_for("auth.login"))

    creds = Credentials(username=username, password=password)

    try:
        response = get_user_service().login(creds)
    except requests.RequestException as ex:
        flash(f"Auth service unreachable: {ex}", "error")
        return redirect(url_for("auth.login"))

    if response.status_code == 200:
        try:
            payload = _unwrap_payload(response)
        except ValueError:
            payload = {}

        access_token = payload.get("accessToken")
        if not access_token:
            flash("Auth response missing access token.", "error")
            return redirect(url_for("auth.login"))

        session["user"] = creds.username
        session["access_token"] = access_token
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
    access_token = session.get("access_token")
    session.clear()

    if access_token:
        try:
            get_user_service().logout(access_token)
        except requests.RequestException:
            pass

    flash("Logged out.", "success")
    return redirect(url_for("auth.login"))
