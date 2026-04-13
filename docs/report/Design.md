# 3. Domain-Driven Design to Microservices

In the following chapter we describe how the domain model has been translated into a distributed architecture based on microservices, service discovery, and event-driven integration.

## 3.1 System Operations

This section shows the main operations identified in the SmartGym Monitor system. Operations are derived from the use cases and represent the commands exposed by the services.

| Actor      | Related Use Case   | Command                             | Description                                                                  |
| ---------- | ------------------ | ----------------------------------- | ---------------------------------------------------------------------------- |
| Gym Member | Access Gym         | `startGymSession(badgeId)`          | Creates a new gym session when the badge is validated at the entrance.       |
| Gym Member | Exit Gym           | `endGymSession(badgeId)`            | Closes the active gym session and updates the global gym count.              |
| Gym Member | Access Area        | `processAreaAccess(message)`        | Registers an access event for a specific area and increments area occupancy. |
| Gym Member | Exit Area          | `processAreaExit(message)`          | Registers an exit event for a specific area and decrements area occupancy.   |
| Gym Member | Use Machine        | `startMachineSession(message)`      | Starts a machine session and marks the machine as occupied.                  |
| Gym Member | Stop Using Machine | `endMachineSession(message)`        | Ends the active machine session and frees the machine.                       |
| Admin      | Create Machine     | `createMachine(message)`            | Creates a machine and associates it with an area.                            |
| Admin      | Update Machine     | `updateMachine(machineId, message)` | Updates machine metadata and area association.                               |
| Admin      | Set Maintenance    | `setMachineMaintenance(message)`    | Changes the machine state to maintenance.                                    |
| Admin      | Login              | `handleLogin(credentials)`          | Validates admin credentials and issues a JWT token.                          |

<p align="center"><em>Table 3.1: Main System Operations</em></p>

| Query                          | Description                                                   |
| ------------------------------ | ------------------------------------------------------------- |
| `getGymCount()`                | Returns the total number of members currently inside the gym. |
| `getAreaById(areaId)`          | Returns the occupancy status and capacity of a specific area. |
| `getAllAreas()`                | Returns all configured gym areas.                             |
| `getMachineStatus(machineId)`  | Returns the current state of a machine.                       |
| `getMachineHistory(machineId)` | Returns historical usage sessions for a machine.              |
| `getAttendanceStats(date)`     | Returns attendance statistics for a specific date.            |
| `getMachineUtilization()`      | Returns aggregated machine usage metrics.                     |

<p align="center"><em>Table 3.2: Main System Queries</em></p>

## 3.2 Subdomains to Microservices

Each bounded context has been mapped to a dedicated microservice in order to preserve separation of concerns, service autonomy, and database isolation.

### 3.2.1 Tracking Service (Core)

The `tracking-service` implements the core domain of the system.
It manages gym sessions and the global count of members currently inside the gym.

| Responsibility         | Description                                                                      |
| ---------------------- | -------------------------------------------------------------------------------- |
| Gym Session Management | Creates and terminates `GymSession` entities when members enter or exit the gym. |
| Global Gym Count       | Maintains the real-time number of people inside the gym.                         |
| Invariant Enforcement  | Ensures that a badge cannot have more than one active session at the same time.  |
| Persistence            | Stores session history in its own MongoDB database.                              |

<p align="center"><em>Table 3.3: Tracking service responsibilities</em></p>

### 3.2.2 Area Management Service (Supporting)

The `area-service` manages gym areas and area-level occupancy.

| Responsibility       | Description                                                        |
| -------------------- | ------------------------------------------------------------------ |
| Area Configuration   | Defines and updates area metadata such as name and capacity.       |
| Occupancy Tracking   | Maintains the current number of people inside each gym area.       |
| Capacity Enforcement | Guarantees that `0 ≤ currentCount ≤ capacity` always holds.        |
| Event Handling       | Processes access and exit messages coming from the embedded layer. |

<p align="center"><em>Table 3.4: Area service responsibilities</em></p>

### 3.2.3 Machine Management Service (Supporting)

The `machine-service` manages machines, machine sessions, and state transitions.

| Responsibility              | Description                                                                        |
| --------------------------- | ---------------------------------------------------------------------------------- |
| Machine Configuration       | Manages machine metadata and association with areas.                               |
| Machine Session Lifecycle   | Creates and closes `MachineSession` entities.                                      |
| State Transition Validation | Ensures valid transitions between `Free`, `Occupied`, and `Maintenance`.           |
| Consistency Enforcement     | Guarantees that a machine cannot have more than one active session simultaneously. |

<p align="center"><em>Table 3.5: Machine service responsibilities</em></p>

### 3.2.4 Embedded Service (Supporting)

The `embedded-service` acts as the bridge between simulated devices and backend services.
It is not a classic CRUD microservice: its main responsibility is to receive MQTT messages, translate them, and forward them to the operational services through HTTP adapters.

| Responsibility             | Description                                                                   |
| -------------------------- | ----------------------------------------------------------------------------- |
| Device Integration         | Manages RFID readers, turnstiles, doors, and proximity sensors.               |
| Message Translation        | Converts low-level device messages into structured application messages.      |
| Asynchronous Communication | Consumes and publishes MQTT messages on the broker.                           |
| Forwarding Layer           | Invokes the tracking, area, machine, and analytics services through adapters. |

<p align="center"><em>Table 3.6: Embedded service responsibilities</em></p>

### 3.2.5 Analytics Service (Generic)

The `analytics-service` provides historical and aggregated information for monitoring.

| Responsibility              | Description                                               |
| --------------------------- | --------------------------------------------------------- |
| Attendance Statistics       | Computes gym attendance trends and peak hours.            |
| Machine Usage Analysis      | Calculates machine utilization rates and dwell time.      |
| Historical Data Aggregation | Processes domain events to generate analytical snapshots. |
| Read Model Management       | Maintains optimized read models for the dashboard.        |

<p align="center"><em>Table 3.7: Analytics service responsibilities</em></p>

### 3.2.6 Authentication Service (Generic)

The `auth-service` manages administrator authentication and token-based security.

| Responsibility       | Description                                                             |
| -------------------- | ----------------------------------------------------------------------- |
| Admin Authentication | Validates administrator credentials.                                    |
| Token Management     | Issues JWT access tokens and supports token validation in the gateway.  |
| Access Control       | Protects dashboard access and service calls that require authorization. |
| User Registry        | Stores the seeded administrator account and login history.              |

<p align="center"><em>Table 3.8: Authentication service responsibilities</em></p>

### 3.2.7 Infrastructure Services

The architecture also includes two supporting infrastructure services:

- **`service-discovery`**: Eureka registry used to register and discover services;
- **`gateway`**: Spring Cloud Gateway entry point that routes requests to the registered services and enforces JWT checks on protected calls.

## 3.3 System Operation Identification

This section identifies the main operations exposed by each microservice and the components they collaborate with.

| Microservice        | Operations                                                                                                                                                   | Collaborators                                                              |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------- |
| `tracking-service`  | `startGymSession()`, `endGymSession()`, `getGymCount()`, `getActiveSessions()`                                                                               | `embedded-service`, `auth-service`                                         |
| `area-service`      | `processAreaAccess()`, `processAreaExit()`, `getAreaById()`, `getAllAreas()`, `updateAreaCapacity()`                                                         | `embedded-service`, `tracking-service`                                     |
| `machine-service`   | `createMachine()`, `updateMachine()`, `startMachineSession()`, `endMachineSession()`, `setMachineMaintenance()`, `getMachineStatus()`, `getMachineHistory()` | `embedded-service`, `tracking-service`, `area-service`                     |
| `analytics-service` | `ingestEvent()`, `getAttendanceStats()`, `getAllAttendanceStats()`, `getMachineUtilization()`, `getPeakHours()`, `getAreaAttendance()`, `getAreaPeakHours()` | `tracking-service`, `area-service`, `machine-service`                      |
| `auth-service`      | `handleLogin()`, `handleRegister()`, `handleVerifyUser()`, `handleLogout()`                                                                                  | `gateway`, `frontend`                                                      |
| `embedded-service`  | MQTT event handling and HTTP forwarding                                                                                                                      | `tracking-service`, `area-service`, `machine-service`, `analytics-service` |

<p align="center"><em>Table 3.9: Microservice operations and collaborators</em></p>

## 3.4 API Interface Definition and Identification

The system exposes two integration layers: asynchronous MQTT communication for simulated devices and synchronous REST APIs for the backend microservices and the frontend.

### 3.4.1 Gateway and Service Discovery

The gateway is the external HTTP entry point of the backend.
It uses Eureka to discover the registered services and applies JWT validation through a global filter.

Relevant behavior:

- public access is allowed for actuator paths and authentication endpoints such as `/login` and `/register`;
- protected requests must carry a Bearer token in the `Authorization` header;
- after validation, the gateway injects `X-User-Id` into the forwarded request;
- routes are resolved through service discovery (`lb://...`) rather than hard-coded hostnames.

The gateway routes the following service families:

- `auth-service`
- `analytics-service`
- `embedded-service`
- `machine-service`
- `area-service`
- `tracking-service`

### 3.4.2 Authentication Service

The authentication service exposes the following controller paths:

| Endpoint            | Type | Description                                              |
| ------------------- | ---- | -------------------------------------------------------- |
| `/login`            | POST | Authenticates an administrator and returns a JWT token.  |
| `/register`         | POST | Registers a new user.                                    |
| `/login/{username}` | GET  | Verifies whether a user exists.                          |
| `/logout`           | POST | Registers the logout event using the `X-User-Id` header. |

<p align="center"><em>Table 3.10: Authentication service endpoints</em></p>

### 3.4.3 Tracking Service

The tracking service exposes the following controller paths:

| Endpoint           | Type | Description                    |
| ------------------ | ---- | ------------------------------ |
| `/start-session`   | POST | Creates a new gym session.     |
| `/end-session`     | POST | Ends the active gym session.   |
| `/count`           | GET  | Returns the current gym count. |
| `/active-sessions` | GET  | Returns active gym sessions.   |

<p align="center"><em>Table 3.11: Tracking service endpoints</em></p>

### 3.4.4 Area Management Service

The area service exposes the following controller paths:

| Endpoint    | Type | Description                              |
| ----------- | ---- | ---------------------------------------- |
| `/access`   | POST | Registers entry to a specific area.      |
| `/exit`     | POST | Registers exit from a specific area.     |
| `/{areaId}` | GET  | Returns a specific area by id.           |
| `/`         | GET  | Returns all areas.                       |
| `/capacity` | PUT  | Updates the maximum capacity of an area. |

<p align="center"><em>Table 3.12: Area service endpoints</em></p>

### 3.4.5 Machine Management Service

The machine service exposes the following controller paths:

| Endpoint                | Type | Description                                                 |
| ----------------------- | ---- | ----------------------------------------------------------- |
| `/machines`             | POST | Creates a new machine.                                      |
| `/machines/{machineId}` | PUT  | Updates machine metadata.                                   |
| `/start-session`        | POST | Starts a machine session and marks the machine as occupied. |
| `/end-session`          | POST | Ends the active machine session and frees the machine.      |
| `/set-maintenance`      | POST | Sets the machine status to maintenance.                     |
| `/{machineId}`          | GET  | Returns the current state of a machine.                     |
| `/history/{machineId}`  | GET  | Returns the historical usage sessions of a machine.         |

<p align="center"><em>Table 3.13: Machine service endpoints</em></p>

### 3.4.6 Analytics Service

The analytics service exposes the following controller paths:

| Endpoint                           | Type | Description                                                   |
| ---------------------------------- | ---- | ------------------------------------------------------------- |
| `/events/ingest`                   | POST | Ingests a domain event into the analytics pipeline.           |
| `/attendance/{date}`               | GET  | Returns attendance statistics for a specific date.            |
| `/attendance`                      | GET  | Returns all attendance statistics.                            |
| `/machine-utilization`             | GET  | Returns aggregated machine utilization statistics.            |
| `/machine-utilization/{date}`      | GET  | Returns machine utilization for a specific date.              |
| `/peak-hours`                      | GET  | Returns peak attendance periods.                              |
| `/peak-hours/{date}`               | GET  | Returns peak attendance periods for a specific date.          |
| `/area-attendance`                 | GET  | Returns aggregated area attendance statistics.                |
| `/area-attendance/{date}`          | GET  | Returns area attendance for a specific date.                  |
| `/area-attendance/{date}/{areaId}` | GET  | Returns area attendance for a specific date and area.         |
| `/area-peak-hours`                 | GET  | Returns peak attendance periods per area.                     |
| `/area-peak-hours/{date}`          | GET  | Returns peak attendance periods per area for a specific date. |
| `/area-peak-hours/{date}/{areaId}` | GET  | Returns peak attendance periods for a specific date and area. |

<p align="center"><em>Table 3.14: Analytics service endpoints</em></p>

### 3.4.7 Embedded Service and MQTT Topics

The embedded layer is driven by MQTT rather than by business REST endpoints.
The Go simulator publishes events to the broker using the following topics:

- `smartgym/gym-access`
- `smartgym/area-access`
- `smartgym/machine-usage`
- `smartgym/device-status`

The embedded service consumes these messages, normalizes their payloads, and forwards the resulting commands to the backend services through HTTP adapters.

### 3.4.8 Frontend Flask Application

The frontend is a lightweight Flask application used as the administrative entry point.
Its main routes are:

| Endpoint      | Type | Description                                                        |
| ------------- | ---- | ------------------------------------------------------------------ |
| `/`           | GET  | Redirects to the dashboard or login page depending on the session. |
| `/login`      | GET  | Renders the login form.                                            |
| `/login`      | POST | Authenticates the administrator via `auth-service`.                |
| `/logout`     | GET  | Clears the session and notifies the auth service.                  |
| `/dashboard`  | GET  | Displays the user dashboard and service connectivity summary.      |
| `/api/health` | GET  | Returns a simple health response for deployment checks.            |

<p align="center"><em>Table 3.15: Frontend Flask routes</em></p>

## 3.5 Architectural Notes

The design follows a database-per-service approach with independent MongoDB instances for the operational and analytical services.
This reduces coupling and allows each bounded context to evolve separately.

The overall flow is:

1. the simulator generates device events via MQTT;
2. the embedded service translates them into application-level messages;
3. the operational services update their local state;
4. analytics stores and aggregates historical data;
5. the frontend interacts with the auth service and displays the dashboard to administrators.
