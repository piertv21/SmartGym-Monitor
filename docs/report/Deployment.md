# 7. Deployment

This chapter describes how SmartGym Monitor is deployed locally and how the runtime stack is orchestrated.
The deployment strategy is designed for reproducible end-to-end execution using Docker Compose.

## 7.1 Deployment Scope

The deployment described in this chapter targets a local or lab environment.
It includes:

- Backend microservices and gateway;
- Dedicated MongoDB instances (database-per-service);
- MQTT broker (Mosquitto) and Go simulator;
- Flask frontend;
- Observability stack with Prometheus and Grafana.

The orchestrator is `docker-compose.yml`, while runtime parameters are configured through `.env`.

## 7.2 Prerequisites

Before starting the stack, the host machine must have:

- Docker Engine;
- Docker Compose (v2 plugin);
- Enough available ports for the default configuration (for example `5001`, `8080`, `8761`, `9090`, `3000`).

The standard setup sequence is:

```bash
cp .env.example .env
docker compose up --build
```

<p align="center"><em>Listing 7.1: Standard Docker Compose bootstrap sequence</em></p>

With the default values, the dashboard is available at `http://localhost:5001`.

## 7.3 Runtime Topology

The deployment uses one Docker bridge network, `smartgym-net`, shared by all services.
Each container resolves the others through service hostnames.

The runtime stack includes:

- **Core backend:** `service-discovery`, `gateway`, `auth-service`, `area-service`, `machine-service`, `tracking-service`, `analytics-service`, `embedded-service`.
- **Data layer:** `mongo-auth`, `mongo-analytics`, `mongo-area`, `mongo-machine`, `mongo-tracking`, `mongo-embedded`.
- **Messaging and simulation:** `mosquitto`, `simulator`.
- **Presentation:** `frontend`.
- **Observability:** `prometheus`, `grafana`.

Persistent data is stored through named volumes for MongoDB, Mosquitto, Prometheus, and Grafana.

## 7.4 Startup Order and Initialization

The compose file uses health checks and dependency conditions to enforce a deterministic startup sequence.

1. Infrastructure containers start first (`mongo-*`, `service-discovery`, `mosquitto`).
2. `mongo-reset` waits for all MongoDB containers and clears databases at startup.
3. `mongo-seed` loads initial area and machine data after reset.
4. Domain services start only after required dependencies are healthy or completed.
5. `gateway`, `frontend`, and `simulator` start after backend readiness constraints.

This flow reduces race conditions and allows repeatable test/demo runs with seeded data.

## 7.5 Configuration Model

Configuration is centralized in `.env`, generated from `.env.example`.
Key groups of variables include:

- Service hostnames and ports (Eureka, gateway, each microservice);
- MongoDB connection URIs per service;
- JWT settings (`JWT_SECRET`, issuer, audience, token TTL);
- Frontend session and gateway client settings;
- MQTT broker credentials and topic;
- Prometheus and Grafana endpoint and admin credentials.

This model keeps deployment configuration externalized and consistent across local environments.

## 7.6 Exposed Endpoints

With default environment values, the most relevant host endpoints are:

- Frontend: `http://localhost:5001`;
- Gateway: `http://localhost:8080`;
- Eureka: `http://localhost:8761`;
- Prometheus: `http://localhost:9090`;
- Grafana: `http://localhost:3000`.

Grafana credentials are injected from environment variables (default `admin` / `admin`).

## 7.7 Observability Deployment

The observability layer is deployed together with the application stack:

- Backend services expose metrics at `/actuator/prometheus`;
- Prometheus scrapes all backend services every `5s` (defined in `prometheus.yml`);
- Grafana is automatically provisioned with:
  - datasource `Prometheus` (`http://prometheus:9090`),
  - dashboards loaded from `/var/lib/grafana/dashboards`.

The default dashboard (`smartgym-overview`) provides visibility on request rate, latency, JVM health, availability (`up`), and error trends.

## 7.8 Operational Commands

Typical operations during local deployment are:

```bash
docker compose up --build
docker compose ps
docker compose logs -f gateway
docker compose down
```

<p align="center"><em>Listing 7.2: Core operational commands for local deployment</em></p>

To stop and remove volumes as well:

```bash
docker compose down -v
```

<p align="center"><em>Listing 7.3: Stack teardown with volume cleanup</em></p>

## 7.9 Deployment Notes

The current deployment is optimized for development and demonstration.
In a production scenario, recommended improvements include:

- Secure secret management instead of plain `.env` values;
- TLS termination and hardened network exposure;
- Replicated services and resilient storage;
- Environment-specific compose overlays or orchestration on a cluster platform.
