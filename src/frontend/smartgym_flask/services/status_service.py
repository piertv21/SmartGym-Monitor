from __future__ import annotations

import requests


class StatusService:
    def __init__(
        self,
        gateway_base_url: str,
        statuses_path: str,
        timeout: float = 5,
    ):
        self.gateway_base_url = gateway_base_url.rstrip("/")
        self.statuses_path = statuses_path if statuses_path.startswith("/") else f"/{statuses_path}"
        self.timeout = timeout

    @staticmethod
    def _bearer_headers(access_token: str) -> dict[str, str]:
        return {"Authorization": f"Bearer {access_token}"}

    def fetch_statuses(self, access_token: str) -> requests.Response:
        return requests.get(
            f"{self.gateway_base_url}{self.statuses_path}",
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )

