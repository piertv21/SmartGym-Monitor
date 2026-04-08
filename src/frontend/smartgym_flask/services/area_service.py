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
        paths = ("/area-service", "/area-service/area-service")

        first_response = requests.get(
            f"{self.gateway_base_url}{paths[0]}",
            headers=headers,
            timeout=self.timeout,
        )
        if first_response.status_code < 400:
            return first_response

        fallback_response = requests.get(
            f"{self.gateway_base_url}{paths[1]}",
            headers=headers,
            timeout=self.timeout,
        )
        return fallback_response