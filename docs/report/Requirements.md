# 2. Requirements Analysis

> This section describes the domain and system requirements following a Domain-Driven Design approach.

## 2.1 Domain Definition

<!--
- Describe the problem domain
- Define the main goal of the system
- Identify the main actors involved
- Clarify what is inside and outside the domain boundary
-->

The core domain of this project is the management of a gym.

The aim of the system is therefore to manage the gym comprehensively, with particular attention to the management of machine occupancy.
The system must be able to monitor the use of machines by users, providing real-time information on availability and occupancy.

We decide to split the domain in more subdomains as shown in the following section, in order to break down the complexity of the system.

## 2.2 Subdomains Definitions

<!--
- Identify the main subdomains
- Classify subdomains (core, supporting, generic)
- Explain why the core domain represents the main business value
-->

- **Supporting — Embedded**: this subdomain includes the interaction with physical devices.
  In particular, it manages the machine sensors, the room RFID readers, and real-time communication with the backend.

- **Supporting — Room Management**: this subdomain manages all the aspects related to the room.
  It handles the entry and exit of people and all related data.

- **Supporting — Machine Management**: this subdomain manages all the aspects related to a gym machine.
  It handles the use of a machine by users.

- **Supporting — Gym Management**: this subdomain supervises the overall
  orchestration of the gym. It verifies availability.

- **Generic — Analytics**: this subdomain provides data collection and processing functionalities.
  It offers machine occupancy rate, average dwell time.
  This allows administrators to monitor system performance and optimize operations.

- **Generic — Authentication**: this subdomain manages user identity and access control.
  It provides mechanisms for microservice authentication and API token to ensure system security.

## 2.3 Ubiquitous Language / Glossary

> This section defines the shared domain language used throughout the project.
> All terms listed in the table below must be used consistently in documentation,
> diagrams, and source code.

| Term                   | Description                                                                                                            | Notes / Context                  |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------- | -------------------------------- |
| Gym                    | The physical facility composed of multiple areas.                                                                      | Gym Management                   |
| Gym Area               | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area).                         | Room Management / Gym Management |
| Cardio Area            | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area).                         | Room Management / Gym Management |
| Weight Area            | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area).                         | Room Management / Gym Management |
| Class Area             | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area).                         | Room Management / Gym Management |
| Entrace Area           | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area).                         | Room Management / Gym Management |
| Machine Area           | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area).                         | Room Management / Gym Management |
| Area count             | The number of people currently present in a gym area, including both people who are using machines and people waiting. | Room Management                  |
| Gym count              | The number of people currently present in the gym.                                                                     | Room Management / Gym Management |
| Turnstile              | An element that allows access to the gym/a gym area if a badge is read correctly positioned at the entrance of the gym | Embedded                         |
| RFID reader            | A reader detecting user entry and exit, by using RFID.                                                                 | Embedded                         |
| Door                   | An opening that allows passage from one area of the gym to another                                                     | Embedded                         |
| Access Area Direction  | The value that describe the direction of access between the areas. It can be _IN_ and _OUT_ area                       | Embedded / Room Management       |
| Machine                | A gym equipment unit that can be used by a user.                                                                       | Machine Management               |
| Proximity sensor       | A sensor that detects whether the machine is being used by a user                                                      | Room Management / Gym Management |
| Occupancy              | The current status of a machine, which can be _Free_ or _Occupied_ or _Maintainance_                                   | Room Management / Gym Management |
| Gym Attendance         | Historic attendance of people at the gym                                                                               | Room Management / Gym Management |
| User                   | A gym member accessing gym areas.                                                                                      | Room Management                  |
| Enter Area Event       | Event indicating that a user entered a gym area.                                                                       | Embedded / Room Management       |
| Exit Area Event        | Event indicating that a user left a gym area.                                                                          | Embedded / Room Management       |
| Enter Gym Event        | Event indicating that a user left a gym area.                                                                          | Embedded / Room Management       |
| Exit Gym Event         | Event indicating that a user left a gym area.                                                                          | Embedded / Room Management       |
| User Machine Session   | The time interval during which a user uses a machine.                                                                  | Room Management                  |
| User Gym Session       | The time interval during which a user stays in the gym                                                                 | Room Management                  |
| Admin or Administrator | Staff member responsible for monitoring gym usage and congestion.                                                      | Analytics / Authentication       |

<p align="center"><em>Table 1: Glossary of SmartGym Domain</em></p>

## 2.4 System Requirements

This section lists all system requirements, divided into functional and non-functional requirements.

### 2.4.1 Functional Requirements

<!--
- Define what the system must do
- Number each requirement
- Make requirements clear and testable
-->

1. The system must allow users to access the gym via a turnstile at the entrance of the gym, which reads their badge and grants or denies access accordingly
2. The system must allow users to access different areas of the gym via RFID badge readers
3. The door must allow users to enter a gym area
4. The system must allow users to use machines if they are free
5. The system must detect when a machine is occupied or free
6. The system must allow administrators to view the occupancy status of machines in real time and the machine usage history
7. The system must correctly manage user and machine access data, ensuring data consistency and integrity

### 2.4.2 Non-Functional Requirements

<!--
- Define quality attributes (performance, scalability, reliability, etc.)
- Include architectural and design constraints
- Relate requirements to project goals
-->

1. The system must guarantee the security of user and machine data
2. The system must have low latency to provide real-time information
3. The system must be scalable to handle a growing number of users and machines
4. The system must be able to read a configuration file at startup to initialize the system with the correct settings
5. The system must be reliable and available to guarantee continuous service to users and administrators

## 2.5 Use Case

In this section is shown the use case diagram of the system from the point of
view of the two main actors: Administrator and Gym Member.

![UseCaseDiagram](../public/resources/use_case_diagram.png)

In the following tables the description of each use case related to Administrator and Gym Member.

| Use Case                   | Description                                                                                                        |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| Login to Dashboard         | Allows the administrator to access the monitoring dashboard. Required before performing any administrative action. |
| View Real-time Occupancy   | Displays the current occupancy status of gym areas and machines in real time.                                      |
| View Machine Usage History | Allows the administrator to consult historical data about machine usage sessions.                                  |
| Monitor Gym Attendance     | Shows aggregated data about gym attendance, including peak hours and occupancy trends.                             |

<p align="center"><em>Table 2: Administrator Use Case Description</em></p>

| Use Case        | Description                                                                                                                         |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Access Gym      | Allows the gym member to enter the gym through the turnstile using their badge. This action automatically starts a new Gym Session. |
| Exit Gym        | Allows the gym member to leave the gym. This action automatically ends the current Gym Session.                                     |
| Access Gym Area | Allows the gym member to enter a specific gym area using badge authentication.                                                      |
| Use Machine     | Allows the gym member to use a machine if it is available. The system detects and records the machine session automatically.        |

<p align="center"><em>Table 3: Gym Member Use Case Description</em></p>

## 2.6 User Stories

> In order to better understand the domain following Domain Driven Design we
> isolate user stories in order to better achieve acceptance criteria.

| ID    | User Story                                       | Related FR |
| ----- | ------------------------------------------------ | ---------- |
| US-01 | As a **Gym Member**, I want ... , so that I .... | FR-1, FR-3 |

## 2.7 Quality Attributes Scenarios

| Quality Attribute  | Stimulus                                               | Environment                                              | Artifact                                         | Response                                                                            | Response Measure                                                        |
| ------------------ | ------------------------------------------------------ | -------------------------------------------------------- | ------------------------------------------------ | ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| **Performance**    | A gym member enters an area or starts using a machine. | Normal operating conditions with multiple users present. | Room Management and Machine Management services. | The system updates occupancy status and propagates the change to the dashboard.     | Updated data visible within **2 seconds**.                              |
| **Scalability**    | Increase in number of users and machines.              | Peak hours with high concurrent usage.                   | Backend microservices and database.              | The system handles increased load without degradation.                              | Response time ≤ **3 seconds** under peak load; no data loss.            |
| **Availability**   | A non-critical service failure occurs.                 | Production environment during operational hours.         | Embedded or Analytics service.                   | The system continues operating and restores the failed component or notifies admin. | At least **99% uptime** during operational hours.                       |
| **Reliability**    | A badge is scanned at the turnstile.                   | Normal network conditions.                               | Authentication and Room Management services.     | The system validates the badge and records the access event correctly.              | No duplicated or missing events; guaranteed event persistence.          |
| **Security**       | An administrator attempts to access the dashboard.     | Internal or external network access.                     | Authentication service and API Gateway.          | The system requires valid credentials and enforces authorization.                   | Unauthorized access denied and logged; communication encrypted (HTTPS). |
| **Modifiability**  | A new gym area or machine is added.                    | Maintenance phase.                                       | Configuration files and Gym Management service.  | The system allows configuration without changing core logic.                        | New elements configurable without code modification.                    |
| **Data Integrity** | Concurrent machine occupancy events occur.             | High concurrent usage.                                   | Machine Management aggregate and database.       | The system maintains consistent machine state transitions.                          | A machine cannot be both _Free_ and _Occupied_ simultaneously.          |

## 2.8 Story Telling

> In this section, realistic usage scenarios are described in order to better understand how the SmartGym Monitor system behaves
> in real-life situations. Storytelling is used as a complementary technique to formal models and diagrams, allowing us to
> observe how domain concepts interact dynamically over time. <br> The objective of this section is to highlight how the system reacts to physical events (such as badge scans or machine usage detection),
> how sessions are created and terminated, and how data becomes available for monitoring and analytics.

### 2.8.1 Gym member Storytelling

- Describe system behavior from a gym member perspective
- Focus on interactions with sensors and physical space

### 2.8.2 Administrator Storytelling

- Describe system behavior from an administrator perspective
- Focus on monitoring and decision-making activities

## 2.9 Domain Model

- Introduce the domain model at a high level
- Explain modeling choices

### 2.9.1 Entities and Value Objects

- List main entities
- List value objects
- Explain why each concept is modeled as entity or value object

### 2.9.2 Aggregates

- Identify aggregate roots
- Define aggregate boundaries
- Specify invariants and consistency rules

### 2.9.3 Domain Events

- List domain events
- Describe when and why they occur
- Explain their role in the system

## 2.10 Bounded Context

- Identify bounded contexts
- Define responsibilities for each context
- Explain terminology differences across contexts

## 2.11 Context Map

- Describe relationships between bounded contexts
- Identify integration patterns (e.g., Customer–Supplier, ACL, OHS)
- Explain how data and events flow between contexts
