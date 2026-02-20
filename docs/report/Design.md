# 3 Domain Driven Design to Microservices

> In following subsections we describe how we created the microservices architecture starting
> from the definition of the domain and subdomains described in previous sections.

## 3.1 System Operations

> In this section are shown the mais systems operations identified in the smart gym
> system. Operations are derived use cases and represent the main commands that guides
> the behaviour of the system.

| Actor      | Related Use Case   | Command                                   | Description                                                            |
| ---------- | ------------------ | ----------------------------------------- | ---------------------------------------------------------------------- |
| Gym Member | Access Gym         | `startGymSession(badgeId)`                | Creates a new GymSession when badge is validated at entrance turnstile |
| Gym Member | Exit Gym           | `endGymSession(badgeId)`                  | Closes active GymSession and updates gym count                         |
| Gym Member | Access Area        | `enterArea(badgeId, areaId)`              | Registers AreaEntered event and increments area count                  |
| Gym Member | Exit Area          | `exitArea(badgeId, areaId)`               | Registers AreaExited event and decrements area count                   |
| Gym Member | Use Machine        | `startMachineSession(machineId, badgeId)` | Sets machine status to Occupied and creates MachineSession             |
| Gym Member | Stop Using Machine | `endMachineSession(machineId)`            | Closes MachineSession and sets machine status to Free                  |
| Admin      | Set Maintenance    | `setMachineMaintenance(machineId)`        | Changes machine state to Maintenance                                   |
| Admin      | Login              | `authenticateAdmin(credentials)`          | Validates admin credentials                                            |

<p align="center"><em>Table X: Main System Operation Description</em></p>

| Query                          | Description                                       |
| ------------------------------ | ------------------------------------------------- |
| `getGymCount()`                | Returns total people currently inside the gym     |
| `getAreaStatus(areaId)`        | Returns area occupancy and capacity               |
| `getMachineStatus(machineId)`  | Returns machine state (Free/Occupied/Maintenance) |
| `getActiveSessions()`          | Returns active gym sessions                       |
| `getMachineHistory(machineId)` | Returns machine usage history                     |
| `getAttendanceReport(date)`    | Returns aggregated attendance statistics          |

<p align="center"><em>Table X: Main System Queries Description</em></p>

## 3.2 Subdomains to Microservices

> To correctly apply the Decomposition by Subdomain pattern suggested by Domain-Driven Design, each identified bounded context has been mapped to a dedicated microservice.
> This choice guarantees clear separation of concerns, independent evolution, service autonomy, and database isolation.
> Each microservice owns its domain model, enforces its invariants, and exposes its behavior through well-defined REST APIs or asynchronous events.
> The mapping between subdomains and microservices is described below.

## 3.2.1 Occupancy Tracking Service (Core)

This microservice implements the Core Domain of the SmartGym Monitor system.
It is responsible for managing the lifecycle of gym sessions and maintaining the global consistency of gym occupancy.

| Responsibility           | Description                                                                        |
| ------------------------ | ---------------------------------------------------------------------------------- |
| Gym Session Management   | Creates and terminates GymSession entities when members enter or exit the gym.     |
| Global Gym Count         | Maintains the real-time number of people inside the gym.                           |
| Invariant Enforcement    | Ensures that a badge cannot have more than one active GymSession at the same time. |
| Domain Event Publication | Emits domain events such as GymSessionStarted and GymSessionEnded.                 |

## 3.2.2 Area Management Service (Supporting)

| Responsibility       | Description                                                              |
| -------------------- | ------------------------------------------------------------------------ |
| Area Configuration   | Defines and updates area metadata (name, capacity).                      |
| Occupancy Tracking   | Maintains the current number of people inside each GymArea.              |
| Capacity Enforcement | Guarantees that the constraint 0 ≤ currentCount ≤ capacity always holds. |
| Event Handling       | Processes AreaEntered and AreaExited events.                             |

## 3.2.3 Machine Management Service (Supporting)

| Responsibility              | Description                                                                        |
| --------------------------- | ---------------------------------------------------------------------------------- |
| Machine Configuration       | Manages machine metadata and association with areas.                               |
| Machine Session Lifecycle   | Creates and closes MachineSession entities.                                        |
| State Transition Validation | Ensures valid transitions between Free, Occupied, and Maintenance states.          |
| Consistency Enforcement     | Guarantees that a machine cannot have more than one active session simultaneously. |

## 3.2.4 Embedded Service (Supporting)

| Responsibility             | Description                                                     |
| -------------------------- | --------------------------------------------------------------- |
| Device Integration         | Manages RFID readers, turnstiles, doors, and proximity sensors. |
| Event Translation          | Converts hardware signals into structured backend events.       |
| Asynchronous Communication | Publishes device events through an MQTT/Event Bus mechanism.    |
| Device Monitoring          | Exposes device health and operational status for observability. |

## 3.2.5 Analytics Service (Generic)

| Responsibility              | Description                                                  |
| --------------------------- | ------------------------------------------------------------ |
| Attendance Statistics       | Computes gym attendance trends and peak hours.               |
| Machine Usage Analysis      | Calculates machine utilization rates and dwell time.         |
| Historical Data Aggregation | Processes domain events to generate analytical reports.      |
| Read Model Management       | Maintains optimized read models for dashboard visualization. |

## 3.2.6 Authentication Service (Generic)

| Responsibility            | Description                               |
| ------------------------- | ----------------------------------------- |
| Admin Authentication      | Validates administrator credentials.      |
| Token Management          | Issues and validates JWT tokens.          |
| Role-Based Access Control | Enforces authorization rules.             |
| Service Authentication    | Secures service-to-service communication. |

## 3.3 System Operation Identification

> In this section we identify the main operations exposed by each microservice of the SmartGym Monitor system.
> Operations are derived from the use cases and represent the core commands that drive the behavior of the distributed system.

| Micro-service                  | Operations                                                                                                   | Collaborators                                                                                 |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------- |
| **Occupancy Tracking Service** | startGymSession(), endGymSession(), getGymCount(), getActiveGymSessions()                                    | Embedded Service, Area Management Service, Authentication Service                             |
| **Area Management Service**    | enterArea(), exitArea(), getAreaStatus(), updateAreaCapacity()                                               | Embedded Service, Occupancy Tracking Service, Authentication Service                          |
| **Machine Management Service** | startMachineSession(), endMachineSession(), setMachineMaintenance(), getMachineStatus(), getMachineHistory() | Embedded Service, Area Management Service, Occupancy Tracking Service, Authentication Service |
| **Embedded Service**           | publishBadgeScanned(), publishAreaAccess(), publishMachineUsage(), getDeviceStatus()                         | none (event producer)                                                                         |
| **Analytics Service**          | generateAttendanceReport(), getAttendanceStats(), getMachineUtilization(), getPeakHours()                    | Occupancy Tracking Service, Area Management Service, Machine Management Service               |
| **Authentication Service**     | login(), logout(), validateToken()                                                                           | none                                                                                          |
