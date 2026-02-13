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

| Term                   | Description                                                                                    | Notes / Context                  |
|------------------------|------------------------------------------------------------------------------------------------|----------------------------------|
| Gym                    | The physical facility composed of multiple areas.                                              | Gym Management                   |
| Gym Area               | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area). | Room Management / Gym Management |
| Cardio Area            | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area). | Room Management / Gym Management |
| Weight Area            | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area). | Room Management / Gym Management |
| Class Area             | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area). | Room Management / Gym Management |
| Entrace Area           | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area). | Room Management / Gym Management |
| Machine Area           | A specific physical zone inside the gym (e.g., cardio zone, free weights area, machines area). | Room Management / Gym Management |
| Turnstile              | An element that allows access to the gym/a gym area if a badge is read correctly positioned at the entrance of the gym | Embedded  |
| RFID reader            | A reader detecting user entry and exit, by using RFID.                                        | Embedded                         |
| Machine                | A gym equipment unit that can be used by a user.                                               | Machine Management               |
| Machine sensor         | A sensor that detects whether the machine is being used by a user                              | Room Management / Gym Management |
| Occupancy              | The current status of a machine, which can be _Free_ or _Occupied_ or _Maintainance_     | Room Management / Gym Management |
| User                   | A gym member accessing gym areas.                                                              | Room Management                  |
| Access Area Direction  | The value that describe the direction of access between the areas. It can be _IN_ and _OUT_ area   | Embedded / Room Management       |
| Enter Area Event            | Event indicating that a user entered a gym area.                                               | Embedded / Room Management       |
| Exit Area Event             | Event indicating that a user left a gym area.                                                  | Embedded / Room Management       |
| Enter Gym Event             | Event indicating that a user left a gym area.                                                  | Embedded / Room Management       |
| Exit Gym Event             | Event indicating that a user left a gym area.                                                  | Embedded / Room Management       |
| User Machine Session   | The time interval during which a user uses a machine.                                          | Room Management                  |
| User Gym Session       | The time interval during which a user stays in the gym                                        | Room Management                  |
| Admin or Administrator | Staff member responsible for monitoring gym usage and congestion.                              | Analytics / Authentication       |

## 2.4 System Requirements
This section lists all system requirements, divided into functional and non-functional requirements.

### 2.4.1 Functional Requirements
<!--
- Define what the system must do
- Number each requirement
- Make requirements clear and testable
-->

1. The system must allow users to access the gym via badge readers
2. The system must allow users to access different areas of the gym via badge readers
3. The turnstile must allow users to enter the gym or a gym area if they have a valid badge and deny access otherwise
4. The system must allow users to use machines if they are free
5. The system must detect when a machine is occupied or free
6. The system must allow administrators to view the occupancy status of machines in real time and the usage history
7. The system must correctly manage user and machine access data, ensuring data consistency and integrity

### 2.4.2 Non-Functional Requirements
<!--
- Define quality attributes (performance, scalability, reliability, etc.)
- Include architectural and design constraints
- Relate requirements to project goals
-->

1. The system must guarantee the security of user and machine data
2. The system must be scalable to handle a growing number of users and machines
3. The system must be reliable and available to guarantee continuous service to users and administrators

## 2.5 Use Case

```mermaid
TO DO DIAGRAMMI USE CASE
```



## 2.6 User Stories

| ID    | User Story | Related FR |
|-------|------------|------------|
| US-01 | As a **Gym Member**, I want ... , so that I .... | FR-1, FR-3 |


## 2.7 Event Storming
- Identify domain events
- Identify commands that trigger events
- Identify aggregates involved
- Define the main event flow

## 2.8 Quality Attributes Scenarios 
- Define scenarios for key quality attributes
- Specify stimulus, environment, and expected response
- Focus on system behavior under specific conditions

## 2.9 Story Telling
- Describe realistic system usage scenarios

### 2.9.1 Gym member Storytelling
- Describe system behavior from a gym member perspective
- Focus on interactions with sensors and physical space

### 2.9.2 Administrator Storytelling
- Describe system behavior from an administrator perspective
- Focus on monitoring and decision-making activities

## 2.10 Domain Model
- Introduce the domain model at a high level
- Explain modeling choices

### 2.10.1 Entities and Value Objects
- List main entities
- List value objects
- Explain why each concept is modeled as entity or value object

### 2.10.2 Aggregates
- Identify aggregate roots
- Define aggregate boundaries
- Specify invariants and consistency rules

### 2.10.3 Domain Events
- List domain events
- Describe when and why they occur
- Explain their role in the system

## 2.11 Bounded Context
- Identify bounded contexts
- Define responsibilities for each context
- Explain terminology differences across contexts

## 2.12 Context Map
- Describe relationships between bounded contexts
- Identify integration patterns (e.g., Customer–Supplier, ACL, OHS)
- Explain how data and events flow between contexts