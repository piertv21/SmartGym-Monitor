from __future__ import annotations

from dataclasses import dataclass

import requests


@dataclass(frozen=True)
class Credentials:
    username: str
    password: str


class UserService:
    def __init__(
        self,
        base_url: str,
        timeout: float = 5,
    ):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    @staticmethod
    def _bearer_headers(access_token: str) -> dict[str, str]:
        return {"Authorization": f"Bearer {access_token}"}

    def login(self, credentials: Credentials) -> requests.Response:
        return requests.post(
            f"{self.base_url}/login",
            json={"username": credentials.username, "password": credentials.password},
            timeout=self.timeout,
        )

    def logout(self, access_token: str) -> requests.Response:
        return requests.post(
            f"{self.base_url}/logout",
            json={},
            headers=self._bearer_headers(access_token),
            timeout=self.timeout,
        )

    def user_exists(self, username: str, access_token: str) -> bool | None:
        try:
            response = requests.get(
                f"{self.base_url}/login/{username}",
                headers=self._bearer_headers(access_token),
                timeout=self.timeout,
            )
            return response.status_code == 200
        except requests.RequestException:
            return None
