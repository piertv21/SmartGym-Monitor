from __future__ import annotations

import requests


class TrackingService:
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

    def fetch_gym_count(self, access_token: str) -> requests.Response:
        return requests.get(
            f"{self.gateway_base_url}/tracking-service/count",
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )

