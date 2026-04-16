# 4. Implementation

This chapter describes how the SmartGym Monitor design was translated into code.
The implementation follows the bounded contexts and the microservice decomposition introduced in the previous chapters.

## 4.1 Overall Structure

The project is organized as a monorepo with three main layers:

- **Backend microservices** implemented in Java 21 with Spring Boot 3.4.3;
- **Frontend web application** implemented in Python 3.12 with Flask;
- **Simulator** implemented in Go, used to generate MQTT messages that emulate smart gym devices.

The backend is organized around independent services registered on Eureka and exposed through an API Gateway.
Operational data is persisted in MongoDB, while the embedded layer converts device-level events into structured requests handled by the backend services.

```mermaid
flowchart LR
    Sim[Go Simulator] -->|MQTT| Mosquitto[(Mosquitto Broker)]
    Mosquitto --> Embedded[embedded-service]
    Embedded --> Tracking[tracking-service]
    Embedded --> Area[area-service]
    Embedded --> Machine[machine-service]
    Embedded --> Analytics[analytics-service]
    Frontend[Flask frontend] --> Gateway[API Gateway]
    Gateway --> Auth[auth-service]
    Gateway --> Tracking
    Gateway --> Area
    Gateway --> Machine
    Gateway --> Analytics
    Gateway --> Embedded
```

<p align="center"><em>Listing 4.1: High-level architecture flow represented in Mermaid</em></p>

## 4.2 Backend Microservices

### 4.2.1 Service discovery

The discovery service hosts Eureka on port `8761`.
It acts as the central registry used by the other services to register themselves and discover one another.

```mermaid
flowchart LR
    SD["Eureka Service Discovery"]

    subgraph Clients["Registered Services"]
        GW["gateway"]
        AUTH["auth-service"]
        TRK["tracking-service"]
        AREA["area-service"]
        MACH["machine-service"]
        ANA["analytics-service"]
        EMB["embedded-service"]
    end

    GW -->|register + fetch registry| SD
    AUTH -->|register| SD
    TRK -->|register| SD
    AREA -->|register| SD
    MACH -->|register| SD
    ANA -->|register| SD
    EMB -->|register| SD

    GW -.->|resolve service addresses| SD
```

### 4.2.2 Gateway

The gateway runs on port `8080` and exposes the main entry point for backend requests.
It routes traffic to the microservices using service discovery and also performs JWT-related checks for protected paths.

The configured routes expose the services under these prefixes:

- `/auth-service/**`
- `/analytics-service/**`
- `/embedded-service/**`
- `/machine-service/**`
- `/area-service/**`
- `/tracking-service/**`

Following there is a simplified flow of how the gateway processes incoming requests, including authentication and routing logic.

```mermaid
flowchart TD
    Client["Flask Frontend"] -->|"HTTP request"| EAF["ExternalAuthFilter"]

    subgraph Gateway["API Gateway"]
        EAF --> PUB{"Public endpoint?"}
        PUB -->|"Yes: /login, /register, /actuator"| ROUTE["Spring Cloud Route Matching"]
        PUB -->|"No"| AUTHH{"Bearer token present?"}

        AUTHH -->|"No"| REJECT["401 Unauthorized"]
        AUTHH -->|"Yes"| JVS["JwtValidationService"]

        JVS -->|"JWT invalid / expired"| REJECT
        JVS -->|"JWT valid"| USER["Add X-User-Id header"]
        USER --> ROUTE
    end

    subgraph Services["Backend microservices"]
        direction LR

        subgraph Left[" "]
            direction TB
            AUTH["auth-service"]
            TRK["tracking-service"]
        end

        subgraph Right[" "]
            direction TB
            AREA["area-service"]
            MACH["machine-service"]
            ANA["analytics-service"]
            EMB["embedded-service"]
        end
    end

    ROUTE -->|"/auth-service/**"| AUTH
    ROUTE -->|"/tracking-service/**"| TRK
    ROUTE -->|"/area-service/**"| AREA
    ROUTE -->|"/machine-service/**"| MACH
    ROUTE -->|"/analytics-service/**"| ANA
    ROUTE -->|"/embedded-service/**"| EMB
```

### 4.2.3 Auth-service

The authentication service manages administrator login, registration, user lookup, and logout.
It stores data in MongoDB and issues JWT access tokens.

Main endpoints:

- `POST /login`
- `POST /register`
- `GET /login/{username}`
- `POST /logout`

```mermaid
flowchart TD
    GW[API Gateway]

    subgraph auth-service\n\n\n
        CTRL["AuthRestControllerImpl"]
        CTRL --> API["AuthServiceApiImpl"]
        API -->|"Verify Credentials\n"| REPO["AuthRepositoryImpl"]

        API -->|"Generate / validate Access Token\n" | JWT["JwtTokenService"]
        REPO --> DB[(MongoDB\nauth-db)]
    end

    GW -->|"POST /login\nPOST /register\nGET /login/{username}\nPOST /logout     \n\n "| CTRL
```

### 4.2.4 Tracking-service

This service manages gym sessions.
It creates and closes sessions when a member enters or exits the gym and exposes the current gym count.

Main endpoints:

- `POST /start-session`
- `POST /end-session`
- `GET /count`
- `GET /active-sessions`

```mermaid
flowchart TD
    EMB["embedded-service\n(TrackingServiceHttpAdapter)"]
    DASH["Dashboard\n(via Gateway)"]

    subgraph tracking-service
        CTRL["TrackingRestControllerImpl"]
        CTRL --> API["TrackingServiceApiImpl"]
        API -->|"create / close sessions"| REPO["TrackingRepositoryImpl"]
        REPO --> DB[(MongoDB\ntracking-db)]
    end

    EMB -->|"POST /start-session\n(ENTRY)\n\n"| CTRL
    EMB -->|"POST /end-session\n(EXIT)\n\n"| CTRL
    DASH -->|"GET /count\n GET      /active-sessions \n\n"| CTRL
```

### 4.2.5 Area-service

The area service manages gym areas and area-level occupancy.
It processes access events, exit events, area queries, and capacity updates.

Main endpoints:

- `POST /access`
- `POST /exit`
- `GET /{areaId}`

```mermaid
flowchart TD
    EMB["embedded-service\n(AreaServiceHttpAdapter)"]
    DASH["Dashboard\n(via Gateway)"]

    subgraph area-service
        CTRL["AreaRestControllerImpl"]
        CTRL --> API["AreaServiceApiImpl"]
        API -->|"increment/ decrement\ncurrentCount"| REPO["AreaRepositoryImpl"]
        REPO --> DB[(MongoDB\narea-db)]
    end

    EMB -->|"POST /access (IN)\nPOST /exit (OUT)\n\n"    | CTRL
    DASH -->|"GET /{areaId}\nPUT /capacity"\n\n| CTRL
```

### 4.2.6 Machine-service

The machine service manages machines, their status, and machine sessions.
It supports machine creation, update, maintenance, session start/end, and historical queries.

Main endpoints:

- `POST /machines`
- `PUT /machines/{machineId}`
- `POST /start-session`
- `POST /end-session`
- `POST /set-maintenance`
- `GET /machines`
- `GET /{machineId}`
- `GET /history/{machineId}`

```mermaid
flowchart TD
    EMB["embedded-service\n(MachineServiceHttpAdapter)"]
    DASH["Dashboard\n(via Gateway)"]

    subgraph machine-service
        CTRL["MachineRestControllerImpl"]
        CTRL --> API["MachineServiceApiImpl"]
        API -->|"update status\nmachine + session"| REPO["MachineRepositoryImpl"]
        REPO --> DB[(MongoDB\nmachine-db)]
    end

    EMB -->|"POST /start-session\nPOST /end-session\n\n"| CTRL
    DASH -->|"POST /set-maintenance\nGET /machines\nGET /{machineId}\nGET /machines/history/series"| CTRL
```

### 4.2.7 Analytics-service

The analytics service stores and exposes historical data used by the dashboard.
It ingests events and computes attendance statistics and gym session duration metrics.

Main endpoints:

- `POST /events/ingest`
- `GET /attendance`
- `GET /attendance/series`
- `GET /gym-session-duration/{date}`

```mermaid
flowchart TD
    EMB["embedded-service\n(AnalyticsServiceHttpAdapter)"]
    DASH["Dashboard\n(via Gateway)"]

    subgraph analytics-service
        CTRL["AnalyticsRestControllerImpl"]
        CTRL --> API["AnalyticsServiceApiImpl"]
        API -->|"save event +\ncalcolate aggregates"| REPO["AnalyticsRepositoryImpl"]
        REPO --> DB[(MongoDB\nanalytics-db)]
    end

    EMB -->|"POST /events/ingest"| CTRL
    DASH -->|"\nGET /gym-session-duration/{date}"| CTRL
```

### 4.2.8 Embedded-service

The embedded service is the integration layer between physical devices and backend services.
It subscribes to MQTT topics, forwards events to the operational services, and translates low-level messages into higher-level domain commands.

The service uses adapters for the area, tracking, machine, and analytics services, plus an MQTT manager to publish and receive messages.

The embedded service also exposes a REST endpoint:

- `GET /statuses`

This endpoint is used by the frontend dashboard to retrieve the current status of all known devices.

Here is a simplified flow of how the embedded service processes incoming MQTT messages and interacts with the backend services.

```mermaid
flowchart TD
    SIM["Go Simulator"] -->|"MQTT"| BROKER["Mosquitto Broker"]
    BROKER -->|"MQTT"| EMB
    DASH["Dashboard\n(via Gateway)"] -->|"GET /statuses"| EMB

    EMB["embedded-service"] -->|"HTTP"| TRK["tracking-service"]
    EMB -->|"HTTP"| AREA["area-service"]
    EMB -->|"HTTP"| MACH["machine-service"]
    EMB -->|"HTTP"| ANA["analytics-service"]
    EMB --> DB[(MongoDB\nembedded-db)]
```

## 4.3 Frontend Web Application

The frontend is a Flask application located in `src/frontend/smartgym_flask`.
It provides a lightweight administrative dashboard for authentication, real-time monitoring, and historical data exploration.

The application includes:

- a login page that authenticates against `auth-service`;
- a dashboard page that shows attendance and gym session duration stats;
- a live monitor page that displays real-time area occupancy and machine statuses;
- a history page that allows exploring attendance series and machine session history;
- a health endpoint at `/api/health` used by automated checks;
- several API endpoints (`/api/statuses`, `/api/analytics/dashboard`, `/api/machines`, `/api/live-monitor`, `/api/maintenance/toggle`, `/api/history/filters`, `/api/history`) that aggregate data from backend services for the frontend views.

The frontend stores the access token in the user session and forwards it as a Bearer token when interacting with the backend services through the gateway.
It does not implement the business logic of the gym: that remains in the backend microservices.

```mermaid
flowchart TD
    BROWSER["Browser"] -->|"HTTP"| FLASK["Flask"]

    subgraph Frontend Flask
        FLASK --> AUTH_BP["auth_bp"]
        FLASK --> DASH_BP["dashboard_bp"]
        FLASK --> API_BP["api_bp"]

        AUTH_BP --> US["UserService"]
        DASH_BP --> US
        API_BP --> SS["StatusService"]
        API_BP --> AS["AnalyticsService"]
        API_BP --> MS["MachineService"]
        API_BP --> ARS["AreaService"]
        API_BP --> TS["TrackingService"]
    end

    US -->|"/auth-service/*"| GW["Gateway"]
    SS -->|"/embedded-service/*"| GW
    AS -->|"/analytics-service/*"| GW
    MS -->|"/machine-service/*"| GW
    ARS -->|"/area-service/*"| GW
    TS -->|"/tracking-service/*"| GW
```

## 4.4 Simulator and Event Generation

The simulator is a Go application that publishes MQTT messages to emulate gym activity.
It generates access events for turnstiles, area readers, and machine sensors.

The simulator uses a fixed set of hardcoded areas, machines, and devices, then periodically decides whether a gym member enters the gym, moves between areas, starts a machine session, or leaves.
This approach allows the platform to be exercised end-to-end without requiring real hardware.

The simulator publishes messages such as:

- gym access events,
- area access events,
- machine usage events,
- device status messages.

## 4.5 Persistence and Data Flow

Each operational service owns its own MongoDB database.
This keeps the services loosely coupled and makes it possible to scale or evolve each bounded context independently.

The typical flow is:

1. the simulator emits MQTT events;
2. the embedded service receives and normalizes them;
3. the tracking, area, and machine services update their domain state;
4. analytics stores historical snapshots and aggregated metrics;
5. the frontend dashboard queries the authentication service and exposes the system entry point for administrators.

## 4.6 Validation Strategy

The implementation is validated through multiple test layers:

- **unit tests** for service and domain logic;
- **integration tests** for controller and persistence behavior;
- **e2e tests** for the full system flow.

This structure matches the repository test organization and supports continuous verification during development.
