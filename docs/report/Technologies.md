# 5. Technologies

This chapter summarizes the technologies used to implement, test, and deploy SmartGym Monitor.

## 5.1 Backend Stack

### 5.1.1 Java 21

All backend services are built with Java 21. The Gradle toolchain is configured to use this version consistently across modules.

### 5.1.2 Spring Boot 3.4.3

Spring Boot is the foundation of the backend microservices.
It provides the HTTP layer, dependency injection, configuration management, actuator endpoints, and application bootstrap.

### 5.1.3 Spring Cloud 2024.0.1

Spring Cloud is used for service discovery and API gateway integration.
It enables the microservices to register on Eureka and be routed through the gateway.

### 5.1.4 Spring Web, WebFlux, and Actuator

- **Spring Web** is used by the synchronous REST services.
- **Spring WebFlux** supports reactive and non-blocking integrations where needed.
- **Actuator** exposes health and metrics endpoints used by the Docker health checks and observability tooling.

### 5.1.5 Spring Data MongoDB

Each microservice persists its own data in MongoDB through Spring Data MongoDB.
This supports the database-per-service approach adopted in the architecture.

### 5.1.6 Eureka

Eureka is the service registry used by `service-discovery` and consumed by the other backend services.

### 5.1.7 JWT Security

JWT tokens are issued by `auth-service` and validated by the gateway.
The implementation uses the `jjwt` libraries and a shared secret configured through environment variables.

## 5.2 Data Stores and Messaging

### 5.2.1 MongoDB

MongoDB is the main persistence technology.
Separate databases are used for auth, area, machine, tracking, analytics, and embedded data.

### 5.2.2 MQTT / Mosquitto

The simulator publishes events to Mosquitto, and the embedded service consumes them.
MQTT is the main protocol for device-to-backend communication.

## 5.3 Frontend Stack

### 5.3.1 Python 3.12

The dashboard frontend is implemented in Python 3.12.

### 5.3.2 Flask 3.0.3

Flask is used to build the lightweight web interface for login and dashboard views.

### 5.3.3 Requests

The frontend uses `requests` to communicate with `auth-service`.

### 5.3.4 Gunicorn

Gunicorn is the production-ready WSGI server used to run the Flask application.

## 5.4 Simulator Stack

### 5.4.1 Go 1.24

The device simulator is implemented in Go.
It is small, fast, and well suited to generating background traffic in a long-running process.

### 5.4.2 Paho MQTT Client

The simulator relies on `github.com/eclipse/paho.mqtt.golang` to publish MQTT messages.

## 5.5 Testing and Quality Assurance

### 5.5.1 JUnit and Cucumber

The backend repository includes JUnit tests and a Cucumber-based e2e suite.

### 5.5.2 pytest

The Flask frontend is tested with `pytest`.

## 5.6 Tooling and DevOps

### 5.6.1 Gradle

Gradle orchestrates the Java backend build.

### 5.6.2 Poetry

Poetry manages Python dependencies and the Flask application environment.

### 5.6.3 Docker and Docker Compose

Docker is used to containerize each component, while Docker Compose orchestrates the full stack locally.

### 5.6.4 VitePress and Mermaid

The documentation site is built with VitePress, and Mermaid is used for diagrams.

### 5.6.5 GitHub Actions

GitHub Actions implements the CI, documentation deployment, release, and commit validation workflows.

### 5.6.6 Husky, commitlint, and Renovate

- **Husky** enforces local Git hooks.
- **commitlint** validates Conventional Commits.
- **Renovate** automates dependency update pull requests.

### 5.6.7 semantic-release

Semantic Release handles automated releases from the main branch.
