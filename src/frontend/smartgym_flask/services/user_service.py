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
        gateway_base_url: str,
        gateway_client_id: str,
        gateway_client_secret: str,
        timeout: float = 5,
    ):
        self.base_url = base_url.rstrip("/")
        self.gateway_base_url = gateway_base_url.rstrip("/")
        self.gateway_client_id = gateway_client_id
        self.gateway_client_secret = gateway_client_secret
        self.timeout = timeout

    def generate_gateway_token(self) -> str:
        response = requests.post(
            f"{self.gateway_base_url}/auth/generate",
            json={
                "clientId": self.gateway_client_id,
                "clientSecret": self.gateway_client_secret,
            },
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()["token"]

    def login(self, credentials: Credentials, gateway_token: str) -> requests.Response:
        return requests.post(
            f"{self.base_url}/login",
            json={"username": credentials.username, "password": credentials.password},
            headers={"X-Auth-Token": gateway_token},
            timeout=self.timeout,
        )

    def logout(self, username: str, gateway_token: str) -> requests.Response:
        return requests.post(
            f"{self.base_url}/logout",
            json={"username": username},
            headers={"X-Auth-Token": gateway_token},
            timeout=self.timeout,
        )

    def user_exists(self, username: str, gateway_token: str) -> bool | None:
        try:
            response = requests.get(
                f"{self.base_url}/login/{username}",
                headers={"X-Auth-Token": gateway_token},
                timeout=self.timeout,
            )
            return response.status_code == 200
        except requests.RequestException:
            return None
