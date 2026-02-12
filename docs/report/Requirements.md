# 2. Requirements Analysis

> This section describes the domain and system requirements following a Domain-Driven Design approach.

## 2.1 Domain Definition
- Describe the problem domain
- Define the main goal of the system
- Identify the main actors involved
- Clarify what is inside and outside the domain boundary

## 2.2 Subdomains Definitions
<!--
- Identify the main subdomains
- Classify subdomains (core, supporting, generic)
- Explain why the core domain represents the main business value
-->

The core domain of this project is the management of a gym. Including all the aspects needed to manage the machine occupancy.
We decide to split the domain in more subdomains as shown in the following section, in order to break down the complexity of the system.

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
| Machine                | A gym equipment unit that can be used by a user.                                               | Machine Management               |
| Machine sensor         | A sensor that detects whether the machine is being used by a user                              | Room Management / Gym Management |
| Occupancy              | The current status of a machine, which can be _Free_ or _Occupied_.                            | Room Management / Gym Management |
| User                   | A gym member accessing gym areas.                                                              | Room Management                  |
| Badge reader           | A reader detecting user entry and exit, by using cards.                                        | Embedded                         |
| Enter Event            | Event indicating that a user entered a gym area.                                               | Embedded / Room Management       |
| Exit Event             | Event indicating that a user left a gym area.                                                  | Embedded / Room Management       |
| User Session           | The time interval during which a user uses a machine.                                          | Room Management                  |
| Admin or Administrator | Staff member responsible for monitoring gym usage and congestion.                              | Analytics / Authentication       |

## 2.4 System Requirements

### 2.4.1 Functional Requirements
- Define what the system must do
- Number each requirement
- Make requirements clear and testable

### 2.4.2 Non-Functional Requirements
- Define quality attributes (performance, scalability, reliability, etc.)
- Include architectural and design constraints
- Relate requirements to project goals

## 2.5 Use Case
- Identify main system use cases
- Describe typical interactions between actors and the system
- Focus on behavior, not implementation details

## 2.6 User Story
- Define user stories from the administrator perspective
- Use simple and clear language
- Link stories to functional requirements

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

### 2.9.1 Customer Storytelling
- Describe system behavior from a gym member perspective
- Focus on interactions with sensors and physical space

### 2.9.2 Admin Storytelling
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