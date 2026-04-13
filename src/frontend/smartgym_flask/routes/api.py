import requests
from flask import Blueprint, jsonify, request, session
from datetime import datetime
from zoneinfo import ZoneInfo

from smartgym_flask.extensions import (
    get_analytics_service,
    get_area_service,
    get_machine_service,
    get_status_service,
    get_tracking_service,
)

api_bp = Blueprint("api", __name__, url_prefix="/api")
ANALYTICS_TIMEZONE = ZoneInfo("Europe/Rome")


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


def _normalize_attendance_snapshot(payload: object, today: str) -> dict:
    if isinstance(payload, dict):
        return payload

    if not isinstance(payload, list):
        return {}

    snapshots = [item for item in payload if isinstance(item, dict)]
    if not snapshots:
        return {}

    for snapshot in snapshots:
        if str(snapshot.get("date") or "").strip() == today:
            return snapshot

    snapshots.sort(key=lambda item: str(item.get("date") or ""), reverse=True)
    return snapshots[0]


def _parse_history_sessions(machine_series_payload: object) -> list[dict]:
    if not isinstance(machine_series_payload, dict):
        return []

    series = machine_series_payload.get("series")
    if not isinstance(series, list):
        return []

    sessions: list[dict] = []
    for point in series:
        if not isinstance(point, dict):
            continue
        period = point.get("period")
        point_sessions = point.get("sessions")
        if not isinstance(point_sessions, list):
            continue

        for item in point_sessions:
            if not isinstance(item, dict):
                continue
            session_item = dict(item)
            if period is not None:
                session_item["period"] = period
            sessions.append(session_item)

    sessions.sort(key=lambda row: str(row.get("startTime") or ""), reverse=True)
    return sessions


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

    today = datetime.now(ANALYTICS_TIMEZONE).strftime("%Y-%m-%d")
    analytics_service = get_analytics_service()

    attendance_data = {}
    try:
        att_resp = analytics_service.fetch_attendance(access_token)
        if att_resp.status_code == 200:
            attendance_data = _normalize_attendance_snapshot(att_resp.json(), today)
    except requests.RequestException:
        pass  # We'll just return what we have
    except ValueError:
        pass

    try:
        count_resp = get_tracking_service().fetch_gym_count(access_token)
        if count_resp.status_code == 200:
            count_payload = count_resp.json()
            if isinstance(count_payload, dict) and "gymCount" in count_payload:
                if not isinstance(attendance_data, dict):
                    attendance_data = {}
                attendance_data["gymCount"] = _as_non_negative_int(
                    count_payload["gymCount"]
                )
    except (requests.RequestException, ValueError):
        pass

    session_data = {}
    try:
        sess_resp = analytics_service.fetch_gym_session_duration(access_token, today)
        if sess_resp.status_code == 200:
            session_data = sess_resp.json()
    except requests.RequestException:
        pass

    return jsonify({"attendance": attendance_data, "session": session_data})


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
        return jsonify(
            {"error": "Unable to fetch machines"}
        ), machines_response.status_code
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
                "machines": sorted(
                    machines_by_area.get(area_id, []),
                    key=lambda machine: str(machine.get("machineId", "")),
                ),
            }
        )

    return jsonify(
        {
            "date": datetime.now().strftime("%Y-%m-%d"),
            "areas": live_areas,
            "lastUpdate": datetime.now().isoformat(),
        }
    )


@api_bp.post("/maintenance/toggle")
def toggle_maintenance():
    access_token = session.get("access_token")
    if not access_token:
        return jsonify({"error": "Unauthorized"}), 401

    data = request.get_json()
    machine_id = data.get("machineId")
    active = data.get("active")

    if not machine_id:
        return jsonify({"error": "machineId is required"}), 400

    try:
        response = get_machine_service().set_maintenance(
            access_token, machine_id, active
        )
    except requests.RequestException as ex:
        return jsonify({"error": f"Gateway unreachable: {ex}"}), 503

    if response.status_code >= 400:
        error_message = "Unable to toggle maintenance"
        try:
            payload = response.json()
            if isinstance(payload, dict):
                error_message = (
                    payload.get("error") or payload.get("message") or error_message
                )
        except ValueError:
            pass
        return jsonify({"error": error_message}), response.status_code

    return jsonify(response.json())


@api_bp.get("/history/filters")
def history_filters():
    access_token = session.get("access_token")
    if not access_token:
        return jsonify({"error": "Unauthorized"}), 401

    try:
        machines_response = get_machine_service().fetch_machines(access_token)
        areas_response = get_area_service().fetch_areas(access_token)
    except requests.RequestException as ex:
        return jsonify({"error": f"Gateway unreachable: {ex}"}), 503

    if machines_response.status_code >= 400:
        return jsonify(
            {"error": "Unable to fetch machines"}
        ), machines_response.status_code
    if areas_response.status_code >= 400:
        return jsonify({"error": "Unable to fetch areas"}), areas_response.status_code

    try:
        machines = _normalize_list_payload(machines_response.json())
        areas = _normalize_list_payload(areas_response.json())
    except ValueError:
        return jsonify({"error": "Invalid response from upstream services"}), 502

    normalized_areas = sorted(
        [
            {
                "id": str(area.get("id") or "").strip(),
                "name": str(
                    area.get("name") or area.get("areaType") or area.get("id") or ""
                ).strip(),
            }
            for area in areas
            if isinstance(area, dict) and str(area.get("id") or "").strip()
        ],
        key=lambda area: area["name"].lower(),
    )

    normalized_machines = sorted(
        [
            {
                "machineId": str(
                    machine.get("machineId") or machine.get("id") or ""
                ).strip(),
                "areaId": str(machine.get("areaId") or "").strip(),
            }
            for machine in machines
            if isinstance(machine, dict)
            and str(machine.get("machineId") or machine.get("id") or "").strip()
        ],
        key=lambda machine: machine["machineId"].lower(),
    )

    return jsonify({"areas": normalized_areas, "machines": normalized_machines})


@api_bp.get("/history")
def history_data():
    access_token = session.get("access_token")
    if not access_token:
        return jsonify({"error": "Unauthorized"}), 401

    from_date = (request.args.get("from") or "").strip()
    to_date = (request.args.get("to") or "").strip()
    if not from_date or not to_date:
        return jsonify({"error": "from and to are required (yyyy-MM-dd)"}), 400

    granularity = (request.args.get("granularity") or "daily").strip() or "daily"
    area_id = (request.args.get("areaId") or "").strip() or None
    machine_id = (request.args.get("machineId") or "").strip() or None

    analytics_service = get_analytics_service()
    machine_service = get_machine_service()

    try:
        attendance_response = analytics_service.fetch_attendance_series(
            access_token,
            from_date,
            to_date,
            granularity,
            area_id,
        )
        machine_history_response = machine_service.fetch_machine_history_series(
            access_token,
            from_date,
            to_date,
            granularity,
            area_id,
            machine_id,
        )
    except requests.RequestException as ex:
        return jsonify({"error": f"Gateway unreachable: {ex}"}), 503

    if attendance_response.status_code >= 400:
        return jsonify(
            {"error": "Unable to fetch attendance series"}
        ), attendance_response.status_code
    if machine_history_response.status_code >= 400:
        return jsonify(
            {"error": "Unable to fetch machine history series"}
        ), machine_history_response.status_code

    try:
        attendance_payload = attendance_response.json()
        machine_history_payload = machine_history_response.json()
    except ValueError:
        return jsonify({"error": "Invalid response from upstream services"}), 502

    sessions = _parse_history_sessions(machine_history_payload)
    return jsonify(
        {
            "attendanceSeries": attendance_payload,
            "machineHistorySeries": machine_history_payload,
            "sessions": sessions,
            "totalSessions": len(sessions),
        }
    )
