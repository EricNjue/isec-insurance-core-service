# Motor Insurance Platform - Modular Monolith

## 1. Project Overview
The Motor Insurance Platform is a production-grade, end-to-end MVP implementation for a Web-Based Motor Insurance Application & Certificate Issuance Platform. Built as a **Modular Monolith**, it is designed for fast delivery, regulatory compliance (DMVIC), and future scalability towards microservices.

The platform enables users to obtain motor insurance quotes, upload necessary documentation, make payments via M-Pesa STK Push, and receive digital insurance certificates with specific business rules for monthly and annual covers.

---

## 2. Architecture Summary
The system follows a **Modular Monolith** architecture style, ensuring strict domain boundaries while maintaining a single deployable unit.

- **Architecture Style:** Modular Monolith
- **Hosting:** AWS (Fargate/ECS)
- **Identity & Access Management:** Keycloak (OIDC/JWT)
- **Database:** PostgreSQL
- **Object Storage:** Amazon S3 (Documents)
- **Async Processing:** RabbitMQ (Events, DLQs, Retries)
- **Deployment:** Stateless, Dockerized, horizontally scalable

---

## 3. Tech Stack
- **Core:** Java 17, Spring Boot 3.3
- **Security:** Spring Security 6, Keycloak, JWT-based RBAC
- **Persistence:** Spring Data JPA, Hibernate, PostgreSQL, Liquibase
- **Messaging:** Spring AMQP (RabbitMQ)
- **Build Tool:** Maven (Multi-module project)
- **Integrations:** M-Pesa STK Push, DMVIC Certificate API

---

## 4. Module Breakdown
The project is structured as a multi-module Maven repository to enforce boundaries and facilitate future microservices extraction.

- `app-bootstrap`: Main entry point & global configuration.
- `common`: Shared security, exceptions, and utilities.
- `messaging`: RabbitMQ configuration and event definitions.
- `modules/identity`: IAM integration (Keycloak) and User Profile.
- `modules/applications`: Vehicle & owner details management and lifecycle.
- `modules/rating`: Premium computation (Base, Levies, Charges).
- `modules/documents`: S3 document management & metadata.
- `modules/payments`: M-Pesa STK Push & Partial Payment logic.
- `modules/policies`: Policy lifecycle management.
- `modules/certificates`: DMVIC issuance rules (Monthly vs Annual).
- `modules/notifications`: Async email/SMS notifications and reminders.
- `modules/reporting`: CSV exports & date-range reports.
- `modules/audit`: Aspect-oriented audit logging.
- `modules/integrations`: External API adapters (M-Pesa, DMVIC).

---

## 5. Authentication & Roles (Keycloak)
Authentication and Authorization are handled via **Keycloak**.
- **Roles:**
    - `ROLE_RETAIL_USER`: Create applications, view own policies.
    - `ROLE_AGENT`: Create applications on behalf of clients.
    - `ROLE_ADMIN`: Configuration, reporting, and full audit view.
- **RBAC:** Enforced at both Controller and Service layers using `@PreAuthorize`.

---

## 6. Environment Setup
### Prerequisites
- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- RabbitMQ
- PostgreSQL
- Keycloak

---

## 7. Configuration (env vars)
- `DB_URL`: JDBC URL for PostgreSQL.
- `DB_USERNAME` / `DB_PASSWORD`: Database credentials.
- `RABBITMQ_HOST` / `RABBITMQ_USER` / `RABBITMQ_PASS`: RabbitMQ connection details.
- `KEYCLOAK_ISSUER_URI`: OIDC Discovery endpoint.
- `S3_BUCKET` / `AWS_REGION`: AWS S3 configuration for document storage.

---

## 8. API Usage Flow (Happy Path)
1. **Anonymous Quote**: `POST /api/v1/rating/anonymous-quote` (unauthenticated) to see potential premium. Store the returned `id`.
2. **Get Profile**: `GET /api/v1/profile` after logging in.
3. **Create Application**: `POST /api/v1/applications` to start a draft. You can provide `anonymousQuoteId` to pre-fill and link your quote.
4. **Get Authenticated Quote**: `POST /api/v1/applications/quote` to see premium breakdown for an existing application.
5. **Upload Documents**: Use `GET /api/v1/documents/presigned-url` to get a PUT URL, upload your file, then use `GET /api/v1/documents/application/{id}` to see all your associated documents and their download URLs.
6. **Pay**: `POST /api/v1/payments/stk-push` to initiate M-Pesa.
7. **Issue Certificate**: `POST /api/v1/certificates/request` after payment callback.

---

## 9. Payment & Certificate Flow
- User initiates STK Push.
- System records payment as `PENDING`.
- Safaricom sends callback; System updates balance and records receipt.
- If payment reaches 35%, Month 1 certificate is unlocked.

---

## 10. Monthly Cover Rules
- **Month 1 & 2:** Issued if at least 35% (Month 1) or 70% (Month 2) of the annual premium is paid.
- **Month 3:** Locked until 100% of the annual premium is settled.
- **Maturity Date:** Calculated as `Policy Start Date + 1 Year - 1 Day`.

---

## 11. Notifications & Reminders
- Expiry reminders are sent at 30, 14, and 3 days before expiry.
- Valuation reminder letters are generated as PDF and sent via email.

---

## 12. Reporting
- Admin users can export YoY premiums and issuance history via `GET /api/v1/reports/export`.

---

## 13. Running Locally
```bash
mvn clean install
mvn spring-boot:run -pl app-bootstrap
```

### Local Startup (H2)
For local development without a full PostgreSQL instance:
```bash
DB_URL=jdbc:h2:mem:testdb DB_USERNAME=sa DB_PASSWORD=sa ./mvnw spring-boot:run -pl app-bootstrap
```

---

## 14. Running in AWS
The application is Dockerized and ready for deployment on ECS Fargate.
```bash
docker build -t motor-insurance-platform .
```

---

## 15. Postman Collection Usage
Import `isec-insurance-platform.postman_collection.json` into Postman. 

**Authentication Flow:**
1. Open the **Authentication** folder.
2. Run the **Get Access Token** request. This will use the Resource Owner Password Credentials grant to obtain a JWT from Keycloak.
3. The access token is automatically saved to the `accessToken` environment variable via a test script.
4. Subsequent requests in the collection are configured to use this `accessToken` via Bearer token authentication.

**Environment Variables:**
Update the following variables in the collection to match your setup:
- `baseUrl`: The URL of the Spring Boot application (default: `http://localhost:8080`).
- `keycloakUrl`: The base URL of your Keycloak instance (default: `http://localhost:9050`).
- `realm`: The Keycloak realm (default: `isec-insurance`).
- `clientId`: The OIDC client ID.
- `username` / `password`: Test user credentials.
- `applicationId` / `policyId`: IDs used for testing specific records.
- `presignedUploadUrl`: (Managed) Set automatically when running 'Get Upload Presigned URL'.
