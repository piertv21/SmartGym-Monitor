# SmartGym-Monitor

<p align="center">
  <a href="https://github.com/piertv21/SmartGym-Monitor">
    <img src="./docs/public/resources/Logo.png" alt="SmartGym Monitor Logo" width="220" height="220" />
  </a>
</p>

<p align="center">
  <b>SmartGym-Monitor</b>
  <br/>
  Monitor gym occupancy, machine usage, and historical activity through an event-driven microservices platform.
  <br/><br/>
  <img src="https://img.shields.io/badge/architecture-microservices-6f42c1" alt="Architecture" />
  <img src="https://img.shields.io/badge/backend-Spring%20Boot%20%2B%20Eureka-6DB33F" alt="Backend" />
  <img src="https://img.shields.io/badge/frontend-Flask-000000" alt="Frontend" />
  <img src="https://img.shields.io/badge/simulator-Go%20%2B%20MQTT-00ADD8" alt="Simulator" />
  <img src="https://img.shields.io/badge/license-MIT-yellow" alt="License" />
</p>

---

## Overview

SmartGym-Monitor is a distributed system for smart gym monitoring.
It combines simulated sensor events, backend domain services, and a Flask dashboard to support:

- real-time area occupancy monitoring;
- machine usage tracking;
- historical data collection for analytics;
- authenticated access for administrative users.

## Main Features

- **Event-driven flow:** simulated devices publish MQTT events consumed by backend services.
- **Domain-based microservices:** dedicated services for auth, areas, machines, tracking, analytics, and embedded orchestration.
- **Central gateway and discovery:** API routing via `gateway` and registry via `service-discovery` (Eureka).
- **Dashboard frontend:** Flask web app for login and monitoring views.
- **Containerized stack:** full local orchestration with Docker Compose.

## Architecture

### Core Services

- `service-discovery`
- `gateway`
- `auth-service`
- `area-service`
- `machine-service`
- `tracking-service`
- `analytics-service`
- `embedded-service`
- `frontend`
- `simulator`

### Supporting Infrastructure

- MongoDB instances (database-per-service approach)
- Eclipse Mosquitto MQTT broker

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.4.3, Spring Cloud, Gradle
- **Frontend:** Python 3.12, Flask, Requests, Gunicorn, Poetry
- **Simulator:** Go 1.24, Eclipse Paho MQTT client
- **Data & Messaging:** MongoDB, MQTT (Mosquitto)
- **Testing:** JUnit, Cucumber, pytest
- **DevOps:** Docker, Docker Compose, GitHub Actions, semantic-release

## Branching Model

This repository follows a Git Flow-style workflow:

- `main`: stable/release-ready code
- `develop`: integration branch
- `feature/*`: feature development branches

## Getting Started

1. Create your local environment file from the example. Configuration values (ports, credentials, service URLs) are managed here.

```bash
cp .env.example .env
```

2. Build and start the full stack:

```bash
docker compose up --build
```

3. Open the dashboard (default):

```bash
http://localhost:5001
```

## Monitoring (Prometheus + Grafana)

The Docker Compose stack also starts a monitoring pipeline based on Spring Boot Actuator, Prometheus, and Grafana.

- **Prometheus** scrapes backend metrics from `/actuator/prometheus` using `prometheus.yml`.
- **Grafana** uses a provisioned Prometheus datasource and auto-loads the dashboard in `grafana/dashboards/smartgym-overview.json`.

Useful local endpoints (default values from `.env.example`):

- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3000`
- Example metrics endpoint: `http://localhost:8080/actuator/prometheus` (gateway)

Grafana default credentials are configured through environment variables:

- username: `admin`
- password: `admin`

After login, open the dashboard with UID `smartgym-overview` (title: `SmartGym Monitor`) to inspect:

- request rate and p95 latency per service;
- JVM memory and thread trends;
- service availability (`up`);
- HTTP 5xx rate and CPU usage.

## Project Structure

```text
SmartGym-Monitor/
|- src/
|  |- backend/        # Java microservices + e2e tests
|  |- frontend/       # Flask dashboard
|  |- simulator/      # Go MQTT event simulator
|- docs/              # VitePress docs
|- docker-compose.yml # Local orchestration
```

## Documentation

The complete project documentation is available in the `docs/` folder or at the repository's website.

## License

This project is released under the MIT License. See `LICENSE` for details.
