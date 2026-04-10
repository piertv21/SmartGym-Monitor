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

    def fetch_machine_history_series(
        self,
        access_token: str,
        from_date: str,
        to_date: str,
        granularity: str,
        area_id: str | None,
        machine_id: str | None,
    ) -> requests.Response:
        params = {
            "from": from_date,
            "to": to_date,
            "granularity": granularity,
        }
        if area_id:
            params["areaId"] = area_id
        if machine_id:
            params["machineId"] = machine_id

        return requests.get(
            f"{self.gateway_base_url}/machine-service/machines/history/series",
            headers=self._bearer_headers(access_token),
            params=params,
            timeout=self.timeout,
        )