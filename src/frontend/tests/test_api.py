import requests
from datetime import datetime as real_datetime

from smartgym_flask import create_app


def test_api_health_returns_ok():
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    response = client.get("/api/health")

    assert response.status_code == 200
    assert response.get_json() == {"status": "ok"}


def test_api_statuses_requires_login_session():
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    response = client.get("/api/statuses")

    assert response.status_code == 401
    assert response.get_json() == {"error": "Unauthorized"}


def test_api_statuses_returns_normalized_data(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class DummyResponse:
        status_code = 200

        @staticmethod
        def json():
            return {
                "list": [
                    {
                        "map": {
                            "deviceId": "sensor-2",
                            "deviceType": "SENSOR",
                            "online": False,
                            "statusDetail": "DOWN",
                            "timeStamp": "2026-04-05T10:20:37Z",
                        }
                    },
                    {
                        "map": {
                            "deviceId": "sensor-1",
                            "deviceType": "SENSOR",
                            "online": True,
                            "statusDetail": "UP",
                            "timeStamp": "2026-04-05T10:20:37Z",
                        }
                    },
                ]
            }

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_status_service",
        lambda: type(
            "S", (), {"fetch_statuses": lambda self, access_token: DummyResponse()}
        )(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.get("/api/statuses")
    payload = response.get_json()

    assert response.status_code == 200
    assert payload["count"] == 2
    assert payload["statuses"][0]["deviceId"] == "sensor-1"
    assert payload["statuses"][1]["deviceId"] == "sensor-2"


def test_toggle_maintenance_returns_backend_conflict_message(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class DummyResponse:
        status_code = 409

        @staticmethod
        def json():
            return {"message": "cannot set maintenance while occupied: machine-1"}

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_machine_service",
        lambda: type(
            "S",
            (),
            {
                "set_maintenance": lambda self,
                access_token,
                machine_id,
                active: DummyResponse()
            },
        )(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.post(
        "/api/maintenance/toggle", json={"machineId": "machine-1", "active": True}
    )

    assert response.status_code == 409
    assert response.get_json() == {
        "error": "cannot set maintenance while occupied: machine-1"
    }


def test_live_monitor_requires_login_session():
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    response = client.get("/api/live-monitor")

    assert response.status_code == 401
    assert response.get_json() == {"error": "Unauthorized"}


def test_live_monitor_returns_aggregated_data(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class MachinesResponse:
        status_code = 200

        @staticmethod
        def json():
            return [
                {"machineId": "bench-1", "areaId": "area-weight", "status": "FREE"},
                {
                    "machineId": "treadmill-1",
                    "areaId": "area-cardio",
                    "status": "OCCUPIED",
                },
            ]

    class AreasResponse:
        status_code = 200

        @staticmethod
        def json():
            return [
                {
                    "id": "area-weight",
                    "name": "Weight",
                    "capacity": 20,
                    "currentCount": 10,
                    "areaType": "WEIGHT",
                },
                {
                    "id": "area-cardio",
                    "name": "Cardio",
                    "capacity": 8,
                    "currentCount": 4,
                    "areaType": "CARDIO",
                },
            ]

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_machine_service",
        lambda: type(
            "S", (), {"fetch_machines": lambda self, access_token: MachinesResponse()}
        )(),
    )
    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_area_service",
        lambda: type(
            "S", (), {"fetch_areas": lambda self, access_token: AreasResponse()}
        )(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.get("/api/live-monitor")
    payload = response.get_json()

    assert response.status_code == 200
    assert len(payload["areas"]) == 2
    assert payload["areas"][0]["name"] == "Cardio"
    assert payload["areas"][0]["currentUsers"] == 4
    assert payload["areas"][0]["occupancyPercent"] == 50.0
    assert payload["areas"][1]["name"] == "Weight"
    assert payload["areas"][1]["currentUsers"] == 10
    assert payload["areas"][1]["occupancyPercent"] == 50.0


def test_dashboard_stats_uses_areas_current_count_for_gym_count(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class AttendanceResponse:
        status_code = 200

        @staticmethod
        def json():
            return {"gymCount": 3, "totalEntries": 18, "totalExits": 9}

    class SessionResponse:
        status_code = 200

        @staticmethod
        def json():
            return {"averageDurationMinutes": 44.5}

    class AreasResponse:
        status_code = 200

        @staticmethod
        def json():
            return [
                {"id": "a1", "currentCount": 10},
                {"id": "a2", "currentCount": "15"},
                {"id": "a3", "currentCount": -3},
                {"id": "a4", "currentCount": "invalid"},
            ]

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_analytics_service",
        lambda: type(
            "S",
            (),
            {
                "fetch_attendance": lambda self, access_token: AttendanceResponse(),
                "fetch_gym_session_duration": lambda self,
                access_token,
                date: SessionResponse(),
            },
        )(),
    )
    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_area_service",
        lambda: type(
            "S", (), {"fetch_areas": lambda self, access_token: AreasResponse()}
        )(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.get("/api/analytics/dashboard")
    payload = response.get_json()

    assert response.status_code == 200
    assert payload["attendance"]["gymCount"] == 25
    assert payload["attendance"]["totalEntries"] == 18
    assert payload["attendance"]["totalExits"] == 9
    assert payload["session"]["averageDurationMinutes"] == 44.5


def test_dashboard_stats_keeps_analytics_gym_count_when_areas_unreachable(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class AttendanceResponse:
        status_code = 200

        @staticmethod
        def json():
            return {"gymCount": 7, "totalEntries": 20, "totalExits": 13}

    class SessionResponse:
        status_code = 200

        @staticmethod
        def json():
            return {"averageDurationMinutes": 38.0}

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_analytics_service",
        lambda: type(
            "S",
            (),
            {
                "fetch_attendance": lambda self, access_token: AttendanceResponse(),
                "fetch_gym_session_duration": lambda self,
                access_token,
                date: SessionResponse(),
            },
        )(),
    )

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_area_service",
        lambda: type(
            "S",
            (),
            {
                "fetch_areas": lambda self, access_token: (_ for _ in ()).throw(
                    requests.RequestException("boom")
                )
            },
        )(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.get("/api/analytics/dashboard")
    payload = response.get_json()

    assert response.status_code == 200
    assert payload["attendance"]["gymCount"] == 7
    assert payload["attendance"]["totalEntries"] == 20


def test_dashboard_stats_uses_today_attendance_snapshot_when_attendance_is_a_list(
    monkeypatch,
):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class AttendanceResponse:
        status_code = 200

        @staticmethod
        def json():
            return [
                {
                    "date": "2026-04-09",
                    "gymCount": 4,
                    "totalEntries": 11,
                    "totalExits": 6,
                },
                {
                    "date": "2026-04-10",
                    "gymCount": 8,
                    "totalEntries": 21,
                    "totalExits": 13,
                },
            ]

    class SessionResponse:
        status_code = 200

        @staticmethod
        def json():
            return {"averageDurationMinutes": 31.0}

    class AreasResponse:
        status_code = 503

        @staticmethod
        def json():
            return {"error": "upstream unavailable"}

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_analytics_service",
        lambda: type(
            "S",
            (),
            {
                "fetch_attendance": lambda self, access_token: AttendanceResponse(),
                "fetch_gym_session_duration": lambda self,
                access_token,
                date: SessionResponse(),
            },
        )(),
    )
    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_area_service",
        lambda: type(
            "S", (), {"fetch_areas": lambda self, access_token: AreasResponse()}
        )(),
    )
    monkeypatch.setattr(
        "smartgym_flask.routes.api.datetime",
        type(
            "FakeDatetime",
            (),
            {
                "now": staticmethod(
                    lambda *args, **kwargs: real_datetime(2026, 4, 10, 8, 0, 0)
                )
            },
        ),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.get("/api/analytics/dashboard")
    payload = response.get_json()

    assert response.status_code == 200
    assert payload["attendance"]["date"] == "2026-04-10"
    assert payload["attendance"]["gymCount"] == 8
    assert payload["attendance"]["totalEntries"] == 21
    assert payload["attendance"]["totalExits"] == 13


def test_history_filters_requires_login_session():
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    response = client.get("/api/history/filters")

    assert response.status_code == 401
    assert response.get_json() == {"error": "Unauthorized"}


def test_history_filters_returns_areas_and_machines(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class MachinesResponse:
        status_code = 200

        @staticmethod
        def json():
            return [
                {"machineId": "m-02", "areaId": "a2"},
                {"machineId": "m-01", "areaId": "a1"},
            ]

    class AreasResponse:
        status_code = 200

        @staticmethod
        def json():
            return [
                {"id": "a2", "name": "Strength"},
                {"id": "a1", "name": "Cardio"},
            ]

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_machine_service",
        lambda: type(
            "S", (), {"fetch_machines": lambda self, access_token: MachinesResponse()}
        )(),
    )
    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_area_service",
        lambda: type(
            "S", (), {"fetch_areas": lambda self, access_token: AreasResponse()}
        )(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.get("/api/history/filters")
    payload = response.get_json()

    assert response.status_code == 200
    assert payload["areas"][0] == {"id": "a1", "name": "Cardio"}
    assert payload["machines"][0] == {"machineId": "m-01", "areaId": "a1"}


def test_history_returns_attendance_and_flattened_sessions(monkeypatch):
    app = create_app({"TESTING": True, "SECRET_KEY": "test"})
    client = app.test_client()

    class AttendanceSeriesResponse:
        status_code = 200

        @staticmethod
        def json():
            return {
                "series": [
                    {
                        "period": "2026-04-10",
                        "currentCount": 12,
                        "totalEntries": 30,
                        "totalExits": 18,
                    }
                ]
            }

    class MachineHistoryResponse:
        status_code = 200

        @staticmethod
        def json():
            return {
                "series": [
                    {
                        "period": "2026-04-10",
                        "sessions": [
                            {
                                "machineId": "m-01",
                                "areaId": "a1",
                                "startTime": "2026-04-10T10:00:00",
                                "endTime": "2026-04-10T10:30:00",
                                "durationSeconds": 1800,
                                "badgeId": "usr-1",
                            }
                        ],
                    }
                ]
            }

    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_analytics_service",
        lambda: type(
            "S",
            (),
            {
                "fetch_attendance_series": lambda self,
                access_token,
                from_date,
                to_date,
                granularity,
                area_id: AttendanceSeriesResponse(),
            },
        )(),
    )
    monkeypatch.setattr(
        "smartgym_flask.routes.api.get_machine_service",
        lambda: type(
            "S",
            (),
            {
                "fetch_machine_history_series": lambda self,
                access_token,
                from_date,
                to_date,
                granularity,
                area_id,
                machine_id: MachineHistoryResponse(),
            },
        )(),
    )

    with client.session_transaction() as session:
        session["access_token"] = "jwt-token-123"

    response = client.get(
        "/api/history?from=2026-04-10&to=2026-04-10&granularity=daily&areaId=a1&machineId=m-01"
    )
    payload = response.get_json()

    assert response.status_code == 200
    assert payload["attendanceSeries"]["series"][0]["currentCount"] == 12
    assert payload["totalSessions"] == 1
    assert payload["sessions"][0]["machineId"] == "m-01"
    assert payload["sessions"][0]["period"] == "2026-04-10"
