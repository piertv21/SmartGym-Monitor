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

    def fetch_attendance(self, access_token: str, date: str) -> requests.Response:
        return requests.get(
            f"{self.gateway_base_url}/analytics-service/attendance/{date}",
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )

    def fetch_gym_session_duration(self, access_token: str, date: str) -> requests.Response:
        return requests.get(
            f"{self.gateway_base_url}/analytics-service/gym-session-duration/{date}",
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )

    def fetch_area_attendance(self, access_token: str, date: str) -> requests.Response:
        return requests.get(
            f"{self.gateway_base_url}/analytics-service/area-attendance/{date}",
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )