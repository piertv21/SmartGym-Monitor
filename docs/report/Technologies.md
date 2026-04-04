# 5. Technologies

This chapter summarizes the technologies used to implement, test, and deploy SmartGym Monitor.

## 5.1 Backend Stack

### Java 21

All backend services are built with Java 21. The Gradle toolchain is configured to use this version consistently across modules.

### Spring Boot 3.4.3

Spring Boot is the foundation of the backend microservices.
It provides the HTTP layer, dependency injection, configuration management, actuator endpoints, and application bootstrap.

### Spring Cloud 2024.0.1

Spring Cloud is used for service discovery and API gateway integration.
It enables the microservices to register on Eureka and be routed through the gateway.

### Spring Web, WebFlux, and Actuator

- **Spring Web** is used by the synchronous REST services.
- **Spring WebFlux** supports reactive and non-blocking integrations where needed.
- **Actuator** exposes health and metrics endpoints used by the Docker health checks and observability tooling.

### Spring Data MongoDB

Each microservice persists its own data in MongoDB through Spring Data MongoDB.
This supports the database-per-service approach adopted in the architecture.

### Eureka

Eureka is the service registry used by `service-discovery` and consumed by the other backend services.

### JWT Security

JWT tokens are issued by `auth-service` and validated by the gateway.
The implementation uses the `jjwt` libraries and a shared secret configured through environment variables.

## 5.2 Data Stores and Messaging

### MongoDB

MongoDB is the main persistence technology.
Separate databases are used for auth, area, machine, tracking, analytics, and embedded data.

### MQTT / Mosquitto

The simulator publishes events to Mosquitto, and the embedded service consumes them.
MQTT is the main protocol for device-to-backend communication.

## 5.3 Frontend Stack

### Python 3.12

The dashboard frontend is implemented in Python 3.12.

### Flask 3.0.3

Flask is used to build the lightweight web interface for login and dashboard views.

### Requests

The frontend uses `requests` to communicate with `auth-service`.

### Gunicorn

Gunicorn is the production-ready WSGI server used to run the Flask application.

## 5.4 Simulator Stack

### Go 1.24

The device simulator is implemented in Go.
It is small, fast, and well suited to generating background traffic in a long-running process.

### Paho MQTT Client

The simulator relies on `github.com/eclipse/paho.mqtt.golang` to publish MQTT messages.

## 5.5 Testing and Quality Assurance

### JUnit and Cucumber

The backend repository includes JUnit tests and a Cucumber-based e2e suite.

### pytest

The Flask frontend is tested with `pytest`.

## 5.6 Tooling and DevOps

### Gradle

Gradle orchestrates the Java backend build.

### Poetry

Poetry manages Python dependencies and the Flask application environment.

### Docker and Docker Compose

Docker is used to containerize each component, while Docker Compose orchestrates the full stack locally.

### VitePress and Mermaid

The documentation site is built with VitePress, and Mermaid is used for diagrams.

### GitHub Actions

GitHub Actions implements the CI, documentation deployment, release, and commit validation workflows.

### Husky, commitlint, and Renovate

- **Husky** enforces local Git hooks.
- **commitlint** validates Conventional Commits.
- **Renovate** automates dependency update pull requests.

### semantic-release

Semantic Release handles automated releases from the main branch.
