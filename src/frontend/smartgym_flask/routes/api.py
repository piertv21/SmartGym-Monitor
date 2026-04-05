import requests
from flask import Blueprint, jsonify, session

from smartgym_flask.extensions import get_status_service

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


