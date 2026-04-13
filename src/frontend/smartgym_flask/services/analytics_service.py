from __future__ import annotations

import requests


class AnalyticsService:
    def __init__(
        self,
        gateway_base_url: str,
        timeout: float = 5,
    ):
        self.gateway_base_url = gateway_base_url.rstrip("/")
        self.timeout = timeout

    @staticmethod
    def _bearer_headers(access_token: str) -> dict[str, str]:
        return {"Authorization": f"Bearer {access_token}"}

    def fetch_attendance(self, access_token: str) -> requests.Response:
        return requests.get(
            f"{self.gateway_base_url}/analytics-service/attendance",
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )

    def fetch_gym_session_duration(
        self, access_token: str, date: str
    ) -> requests.Response:
        return requests.get(
            f"{self.gateway_base_url}/analytics-service/gym-session-duration/{date}",
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )

    def fetch_attendance_series(
        self,
        access_token: str,
        from_date: str,
        to_date: str,
        granularity: str,
        area_id: str | None,
    ) -> requests.Response:
        params = {
            "from": from_date,
            "to": to_date,
            "granularity": granularity,
        }
        if area_id:
            params["areaId"] = area_id

        return requests.get(
            f"{self.gateway_base_url}/analytics-service/attendance/series",
            headers=self._bearer_headers(access_token),
            params=params,
            timeout=self.timeout,
        )
