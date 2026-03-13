from __future__ import annotations

import os
from dataclasses import dataclass

import requests
from flask import Flask, redirect, render_template, request, session, url_for, flash, jsonify

from config import Config


@dataclass(frozen=True)
class Credentials:
    username: str
    password: str


def create_app() -> Flask:
    app = Flask(__name__)
    app.config.from_object(Config)

    auth_base = app.config["AUTH_SERVICE_BASE_URL"].rstrip("/")

    def _auth_post(path: str, payload: dict):
        url = f"{auth_base}{path}"
        return requests.post(url, json=payload, timeout=5)

    def _auth_get(path: str):
        url = f"{auth_base}{path}"
        return requests.get(url, timeout=5)

    @app.get("/health")
    def health():
        return jsonify({"status": "ok"})

    @app.get("/")
    def index():
        if session.get("user"):
            return redirect(url_for("dashboard"))
        return redirect(url_for("login"))

    @app.get("/login")
    def login():
        return render_template("login.html")

    @app.post("/login")
    def login_post():
        username = (request.form.get("username") or "").strip()
        password = request.form.get("password") or ""

        if not username or not password:
            flash("Username and password are required.", "error")
            return redirect(url_for("login"))

        creds = Credentials(username=username, password=password)

        try:
            r = _auth_post("/login", {"username": creds.username, "password": creds.password})
        except requests.RequestException as ex:
            flash(f"Auth service unreachable: {ex}", "error")
            return redirect(url_for("login"))

        if r.status_code == 200:
            session["user"] = creds.username
            flash("Login successful.", "success")
            return redirect(url_for("dashboard"))

        try:
            err = r.json().get("error", "Unauthorized")
        except Exception:
            err = "Unauthorized"

        flash(err, "error")
        return redirect(url_for("login"))

    @app.get("/logout")
    def logout():
        username = session.get("user")
        session.clear()

        if username:
            try:
                _auth_post("/logout", {"username": username})
            except requests.RequestException:
                pass

        flash("Logged out.", "success")
        return redirect(url_for("login"))

    @app.get("/dashboard")
    def dashboard():
        username = session.get("user")
        if not username:
            return redirect(url_for("login"))

        # Optional verification to show a small “system is alive” signal (SPE demo-friendly)
        user_exists = None
        try:
            r = _auth_get(f"/login/{username}")
            user_exists = (r.status_code == 200)
        except requests.RequestException:
            user_exists = None

        return render_template(
            "dashboard.html",
            username=username,
            user_exists=user_exists,
            auth_service_base_url=auth_base,
        )

    return app


app = create_app()

if __name__ == "__main__":
    port = int(os.getenv("PORT", "5001"))
    app.run(host="0.0.0.0", port=port, debug=False)