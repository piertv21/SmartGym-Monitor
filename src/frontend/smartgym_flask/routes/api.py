import requests
from flask import Blueprint, jsonify, session
from datetime import datetime

from smartgym_flask.extensions import (
    get_analytics_service,
    get_area_service,
    get_machine_service,
    get_status_service,
)

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


def _normalize_list_payload(payload: object) -> list[dict]:
    if isinstance(payload, list):
        source = payload
    elif isinstance(payload, dict) and isinstance(payload.get("list"), list):
        source = payload["list"]
    elif isinstance(payload, dict) and isinstance(payload.get("machines"), list):
        source = payload["machines"]
    else:
        return []

    normalized = []
    for item in source:
        if isinstance(item, dict) and isinstance(item.get("map"), dict):
            normalized.append(item["map"])
        elif isinstance(item, dict):
            normalized.append(item)
    return normalized


def _as_non_negative_int(value: object) -> int:
    try:
        number = int(value)
    except (TypeError, ValueError):
        return 0
    return max(number, 0)


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

    try:
        areas_resp = get_area_service().fetch_areas(access_token)
        if areas_resp.status_code == 200:
            areas_payload = areas_resp.json()
            areas = _normalize_list_payload(areas_payload)
            total_current_count = sum(
                _as_non_negative_int(area.get("currentCount"))
                for area in areas
                if isinstance(area, dict)
            )
            if not isinstance(attendance_data, dict):
                attendance_data = {}
            attendance_data["gymCount"] = total_current_count
    except (requests.RequestException, ValueError):
        pass

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


@api_bp.get("/live-monitor")
def live_monitor_data():
    access_token = session.get("access_token")
    if not access_token:
        return jsonify({"error": "Unauthorized"}), 401

    try:
        machines_response = get_machine_service().fetch_machines(access_token)
        areas_response = get_area_service().fetch_areas(access_token)
    except requests.RequestException as ex:
        return jsonify({"error": f"Gateway unreachable: {ex}"}), 503

    if machines_response.status_code >= 400:
        return jsonify({"error": "Unable to fetch machines"}), machines_response.status_code
    if areas_response.status_code >= 400:
        return jsonify({"error": "Unable to fetch areas"}), areas_response.status_code

    try:
        machines_payload = machines_response.json()
        areas_payload = areas_response.json()
    except ValueError:
        return jsonify({"error": "Invalid response from upstream services"}), 502

    machines = _normalize_list_payload(machines_payload)
    areas = _normalize_list_payload(areas_payload)
    area_by_id = {
        str(item.get("id")): item
        for item in areas
        if isinstance(item, dict) and item.get("id")
    }

    machines_by_area: dict[str, list[dict]] = {}
    for machine in machines:
        if not isinstance(machine, dict):
            continue
        area_id = str(machine.get("areaId") or "")
        if not area_id:
            continue
        machines_by_area.setdefault(area_id, []).append(
            {
                "machineId": machine.get("machineId") or machine.get("id") or "-",
                "status": str(machine.get("status") or "FREE").upper(),
            }
        )

    all_area_ids = sorted(set(area_by_id.keys()) | set(machines_by_area.keys()))

    live_areas = []
    for area_id in all_area_ids:
        area = area_by_id.get(area_id, {})

        current_users = _as_non_negative_int(area.get("currentCount"))
        capacity = _as_non_negative_int(area.get("capacity"))
        occupancy_percent = 0
        if capacity > 0:
            occupancy_percent = round((current_users / capacity) * 100, 1)

        live_areas.append(
            {
                "areaId": area_id,
                "name": area.get("name") or area.get("areaType") or area_id,
                "areaType": area.get("areaType"),
                "currentUsers": current_users,
                "capacity": capacity,
                "occupancyPercent": occupancy_percent,
                "machines": sorted(machines_by_area.get(area_id, []), key=lambda machine: str(machine.get("machineId", ""))),
            }
        )

    return jsonify({
        "date": datetime.now().strftime("%Y-%m-%d"),
        "areas": live_areas,
        "lastUpdate": datetime.now().isoformat(),
    })


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
