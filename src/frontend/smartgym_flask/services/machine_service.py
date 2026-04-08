from __future__ import annotations

import requests


class MachineService:
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

    def fetch_machines(self, access_token: str) -> requests.Response:
        return requests.get(
            f"{self.gateway_base_url}/machine-service/machines",
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )

    def set_maintenance(self, access_token: str, machine_id: str, active: bool) -> requests.Response:
        return requests.post(
            f"{self.gateway_base_url}/machine-service/set-maintenance",
            headers=self._bearer_headers(access_token),
            json={
                "machineId": machine_id,
                "active": active
            },
            timeout=self.timeout,
        )