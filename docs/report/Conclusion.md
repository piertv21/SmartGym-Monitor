# 8. Conclusions

SmartGym Monitor demonstrates how Domain-Driven Design can be translated into a practical microservice-based system for gym monitoring.
The project combines simulated devices, operational services, historical analytics, and a lightweight web frontend in a coherent architecture.

From an implementation perspective, the system achieves the main goals defined in the requirements:

- access and session tracking for gym members,
- area occupancy management,
- machine state and session tracking,
- authentication for the administrator dashboard,
- historical analytics for monitoring and decision support.

The modular architecture makes the system easy to understand and evolve.
Each bounded context is represented by a dedicated service, which helps maintain separation of concerns and reduces coupling between functional areas.

The project also integrates several quality-oriented practices, including containerization, CI pipelines, commit validation, automated dependency updates, and documentation deployment.
These elements improve maintainability and make the repository more suitable for collaborative development.

At the same time, the system still leaves room for future improvements.
Possible next steps include:

- richer dashboard visualizations;
- stronger authorization policies for different user roles;
- more detailed analytics and predictive metrics;
- improved observability and monitoring dashboards;
- further abstraction of the embedded simulation layer.

Overall, SmartGym Monitor provides a complete reference implementation of a domain-oriented gym monitoring platform and shows how the various parts of the stack can work together in an end-to-end scenario.
