import requests
from flask import Blueprint, jsonify, session
from datetime import datetime

from smartgym_flask.extensions import get_status_service, get_analytics_service, get_machine_service

api_bp = Blueprint("api", __name__, url_prefix="/api")


@api_bp.get("/health")
def health():
    return jsonify({"status": "ok"})


def _normalize_statuses(payload: object) -> list[dict]:
    if isinstance(payload, list):
        statuses = payload
    elif isinstance(payload, dict) and isinstance(payload.get("list"), list):
        statuses = payload["list"]
    else:
        return []

    normalized = []
    for item in statuses:
        if isinstance(item, dict) and isinstance(item.get("map"), dict):
            normalized.append(item["map"])
        elif isinstance(item, dict):
            normalized.append(item)

    return sorted(normalized, key=lambda status: status.get("deviceId", ""))


@api_bp.get("/statuses")
def statuses():
    access_token = session.get("access_token")
    if not access_token:
        return jsonify({"error": "Unauthorized"}), 401

    try:
        response = get_status_service().fetch_statuses(access_token)
    except requests.RequestException as ex:
        return jsonify({"error": f"Gateway unreachable: {ex}"}), 503

    if response.status_code >= 400:
        return jsonify({"error": "Unable to fetch statuses"}), response.status_code

    try:
        payload = response.json()
    except ValueError:
        return jsonify({"error": "Invalid response from embedded-service"}), 502

    statuses_data = _normalize_statuses(payload)
    return jsonify({"statuses": statuses_data, "count": len(statuses_data)})


@api_bp.get("/analytics/dashboard")
def dashboard_stats():
    access_token = session.get("access_token")
    if not access_token:
        return jsonify({"error": "Unauthorized"}), 401

    today = datetime.now().strftime("%Y-%m-%d")
    analytics_service = get_analytics_service()

    attendance_data = {}
    try:
        att_resp = analytics_service.fetch_attendance(access_token, today)
        if att_resp.status_code == 200:
            attendance_data = att_resp.json()
    except requests.RequestException:
        pass  # We'll just return what we have

    session_data = {}
    try:
        sess_resp = analytics_service.fetch_gym_session_duration(access_token, today)
        if sess_resp.status_code == 200:
            session_data = sess_resp.json()
    except requests.RequestException:
        pass

    return jsonify({
        "attendance": attendance_data,
        "session": session_data
    })


@api_bp.get("/machines")
def machines():
    access_token = session.get("access_token")
    if not access_token:
        return jsonify({"error": "Unauthorized"}), 401

    try:
        response = get_machine_service().fetch_machines(access_token)
    except requests.RequestException as ex:
        return jsonify({"error": f"Gateway unreachable: {ex}"}), 503

    if response.status_code >= 400:
        return jsonify({"error": "Unable to fetch machines"}), response.status_code

    return jsonify(response.json())


@api_bp.post("/maintenance/toggle")
def toggle_maintenance():
    access_token = session.get("access_token")
    if not access_token:
        return jsonify({"error": "Unauthorized"}), 401

    from flask import request
    data = request.get_json()
    machine_id = data.get("machineId")
    active = data.get("active")

    if not machine_id:
        return jsonify({"error": "machineId is required"}), 400

    try:
        response = get_machine_service().set_maintenance(access_token, machine_id, active)
    except requests.RequestException as ex:
        return jsonify({"error": f"Gateway unreachable: {ex}"}), 503

    if response.status_code >= 400:
        error_message = "Unable to toggle maintenance"
        try:
            payload = response.json()
            if isinstance(payload, dict):
                error_message = payload.get("error") or payload.get("message") or error_message
        except ValueError:
            pass
        return jsonify({"error": error_message}), response.status_code

    return jsonify(response.json())
