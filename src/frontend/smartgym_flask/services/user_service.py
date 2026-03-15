from __future__ import annotations

from dataclasses import dataclass

import requests


@dataclass(frozen=True)
class Credentials:
    username: str
    password: str


class UserService:
    def __init__(self, base_url: str, timeout: float = 5):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def login(self, credentials: Credentials) -> requests.Response:
        return requests.post(
            f"{self.base_url}/login",
            json={"username": credentials.username, "password": credentials.password},
            timeout=self.timeout,
        )

    def logout(self, username: str) -> requests.Response:
        return requests.post(
            f"{self.base_url}/logout",
            json={"username": username},
            timeout=self.timeout,
        )

    def user_exists(self, username: str) -> bool | None:
        try:
            response = requests.get(f"{self.base_url}/login/{username}", timeout=self.timeout)
            return response.status_code == 200
        except requests.RequestException:
            return None

