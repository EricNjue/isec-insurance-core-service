# Motor Insurance Platform - Modular Monolith

## 1. Executive Summary
The Motor Insurance Platform is a production-grade, end-to-end MVP implementation for a Web-Based Motor Insurance Application & Certificate Issuance Platform. Built as a **Modular Monolith**, it is designed for fast delivery, regulatory compliance (DMVIC), and future scalability towards microservices.

The platform enables users to obtain motor insurance quotes, upload necessary documentation, make payments via M-Pesa STK Push, and receive digital insurance certificates with specific business rules for monthly and annual covers.

---

## 2. Architecture Overview
The system follows a **Modular Monolith** architecture style, ensuring strict domain boundaries while maintaining a single deployable unit.

- **Architecture Style:** Modular Monolith
- **Hosting:** AWS (Fargate/ECS)
- **Identity & Access Management:** Keycloak (OIDC/JWT)
- **Database:** PostgreSQL
- **Object Storage:** Amazon S3 (Documents)
- **Async Processing:** RabbitMQ (Events, DLQs, Retries)
- **Deployment:** Stateless, Dockerized, horizontally scalable

---

## 3. Technical Stack
- **Core:** Java 17, Spring Boot 3.3
- **Security:** Spring Security 6, Keycloak, JWT-based RBAC
- **Persistence:** Spring Data JPA, Hibernate, PostgreSQL, Liquibase
- **Messaging:** Spring AMQP (RabbitMQ)
- **Build Tool:** Maven (Multi-module project)
- **Integrations:** M-Pesa STK Push, DMVIC Certificate API

---

## 4. Project Structure
The project is structured as a multi-module Maven repository to enforce boundaries and facilitate future microservices extraction.

```text
motor-insurance-platform
│
├── app-bootstrap         # Main entry point & global configuration
├── common                # Shared security, exceptions, and utilities
├── messaging             # RabbitMQ configuration and event definitions
│
├── modules/
│   ├── identity          # IAM integration (Keycloak)
│   ├── applications      # Vehicle & owner details management
│   ├── rating            # Premium computation (Base, Levies, Charges)
│   ├── documents         # S3 document management & metadata
│   ├── payments          # M-Pesa STK Push & Partial Payment logic
│   ├── policies          # Policy lifecycle management
│   ├── certificates      # DMVIC issuance rules (Monthly vs Annual)
│   ├── notifications     # Async email/SMS notifications
│   ├── reporting         # CSV exports & date-range reports
│   ├── audit             # Aspect-oriented audit logging
│   └── integrations      # External API adapters (M-Pesa, DMVIC)
│
└── docker                # Dockerfile and environment configs
```

---

## 5. Key Business Logic & Rules

### Premium Computation (Rating)
- **Base Premium:** Based on vehicle value/type.
- **Levies:** PCF (0.25%) and ITL (0.20%).
- **Certificate Charge:** Fixed fee per certificate issued.

### Partial Payments (M-Pesa)
- Supports **partial payments** (max 4 entries per application).
- Total amount must be settled before full annual certificate issuance.

### Monthly Certificate Rules (35% Rule)
- **Month 1 & 2:** Issued if at least 35% (Month 1) or 70% (Month 2) of the annual premium is paid.
- **Month 3:** Locked until 100% of the annual premium is settled.
- **Maturity Date:** Calculated as `Policy Start Date + 1 Year - 1 Day`.

---

## 6. Security & IAM
Authentication and Authorization are handled via **Keycloak**.
- **Roles:**
    - `ROLE_RETAIL_USER`: Create applications, view own policies.
    - `ROLE_AGENT`: Create applications on behalf of clients.
    - `ROLE_ADMIN`: Configuration, reporting, and full audit view.
- **RBAC:** Enforced at both Controller and Service layers using `@PreAuthorize`.

---

## 7. Messaging & Events (RabbitMQ)
Asynchronous communication is handled via RabbitMQ to ensure high availability and decoupling.
- **Events:** `ApplicationSubmittedEvent`, `PaymentReceivedEvent`, `CertificateIssuedEvent`, etc.
- **Reliability:** Topic exchanges, durable queues, and Dead-Letter Queues (DLQs) for failed message handling.

---

## 8. Operational Features
- **Audit Trail:** Every state change and critical action is captured in an append-only `audit_logs` table (Actor, Action, Entity, Timestamp, Before/After snapshots).
- **Idempotency:** Enforced for payment callbacks and external integrations to prevent duplicate processing.
- **Observability:** Structured logging with correlation IDs; readiness and health probes included.

---

## 9. Setup & Development
### Prerequisites
- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- RabbitMQ
- PostgreSQL
- Keycloak

### Running the Application
The `app-bootstrap` module is the main entry point for the platform.

1. **Clone the repository.**
2. **Configure environment variables** (see `app-bootstrap/src/main/resources/application.yml`).
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
   - `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASS`
   - `KEYCLOAK_ISSUER_URI`
3. **Run with Maven:**
   The application main class is `com.isec.platform.InsurancePlatformApplication`.
   ```bash
   mvn clean install
   mvn spring-boot:run -pl app-bootstrap
   ```
4. **Run with Docker:**
   ```bash
   docker build -t motor-insurance-platform .
   docker run -p 8080:8080 motor-insurance-platform
   ```

---

## 10. Future Evolution
1. **Infrastructure:** Transition from RabbitMQ to Amazon MQ or EventBridge for cloud-native events.
2. **Microservices:** Extract `Payments` or `Certificates` as independent services by moving their respective Maven modules into separate repositories.
3. **Analytics:** Implement a dedicated Read-Model or Data Warehouse for advanced reporting.
