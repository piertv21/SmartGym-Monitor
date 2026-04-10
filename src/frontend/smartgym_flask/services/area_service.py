from __future__ import annotations

import requests


class AreaService:
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

    def fetch_areas(self, access_token: str) -> requests.Response:
        headers = self._bearer_headers(access_token)
        response = requests.get(
            f"{self.gateway_base_url}/area-service",
            headers=headers,
            timeout=self.timeout,
        )
        return response