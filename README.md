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

## 11. Async Processing & Idempotency
The platform utilizes an event-driven architecture with **RabbitMQ** for critical asynchronous workflows:
- **Certificate Issuance:** Requests to DMVIC are processed asynchronously to handle external API latency and failures.
- **Valuation Letters:** Letters are generated and stored in S3 asynchronously.
- **Notifications:** SMS and Email notifications are triggered by lifecycle events.

### Idempotency Strategy
To ensure reliability and prevent duplicate side effects (like double-issuing certificates), we use:
- **Idempotency Keys:** Every async request is assigned a unique key.
- **Redis Tracking:** A Redis-backed `IdempotencyService` tracks processed keys with a configurable TTL (default 7 days).
- **Status Tracking:** The `Certificate` entity maintains state (`PENDING`, `PROCESSING`, `ISSUED`, `FAILED`) to ensure we don't re-process completed requests.

---

## 12. End-to-End Journey (Implementation Details)

### 1. Request Initiation
- When a payment is received and meets business rule thresholds, the `CertificateService` creates a `PENDING` certificate record and publishes a `CertificateRequestedEvent`.
- Users can manually trigger valuation letters via `POST /api/v1/notifications/valuation-letter/{policyId}`.

### 2. Async Certificate Issuance
- `CertificateRequestConsumer` listens for requests.
- It checks idempotency in Redis.
- Updates status to `PROCESSING`.
- Calls `DmvicClient` (mocked).
- On success, updates status to `ISSUED`, saves the reference, and triggers a `NotificationSendEvent`.
- On failure, updates status to `FAILED` and triggers a failure notification.

### 3. Valuation Letter Flow
- `ValuationLetterConsumer` listens for requests.
- Generates a production-grade PDF using **OpenPDF**, uploads it to **Amazon S3** (encrypted at rest), and persists metadata in `valuation_letters`.
- Triggers a `NotificationSendEvent` with a secure, time-limited **presigned download URL**.
- Supports idempotent generation to prevent duplicate letters for the same policy on the same day.

### 4. Notification Flow
- `NotificationConsumer` handles all SMS/Email dispatch.
- Uses idempotency to prevent spamming users on message replays.

---

## 14. Valuation Letter Management & Authenticity
The platform provides a complete capability for generating and managing valuation letters with built-in document authenticity features.

### 1. Purpose
Valuation letters are required by insurers to determine the current market value of a vehicle. Every generated letter includes cryptographic protection and verification tools.

### 2. Document Authenticity & Verification Security
To ensure document integrity and prevent fraud, the system implements:
- **Cryptographic Hashing:** A SHA-256 hash is generated from the final PDF content and stored in the database.
- **Embedded Metadata:** Security metadata (Document ID, Hash, Issuer, IssuedAt) is embedded directly into the PDF info dictionary, programmatically readable but invisible to users.
- **QR Code Verification:** A high-resolution QR code is embedded in the document footer, acting as a pointer to the online verification service.
- **Server-Side Validation:** The system maintains the "Source of Truth" for every document's hash and status.

### 3. End-to-End Verification Flow
1. **QR Scan:** Scanning the QR code on a physical or digital document directs the user to `https://{domain}/verify/doc/{documentId}`.
2. **Instant Status Check:** The Verification API checks the database for the document's existence and its current status (**ACTIVE**, **REVOKED**, or **EXPIRED**).
3. **Cryptographic Proof:** For absolute certainty, the original PDF can be uploaded to `/verify/upload`. The system recomputes the SHA-256 hash of the uploaded file and compares it against the registered hash.
4. **Tamper Detection:** If even a single byte of the PDF content is modified after generation, the hashes will not match, and the system will return a **MODIFIED** status.

### 4. S3 Storage Strategy
- **Encryption:** All PDFs are encrypted at rest using `AES256` server-side encryption.
- **Privacy:** S3 objects are private. Access is granted only via time-limited **Presigned URLs** (valid for 1 hour).
- **Key Structure:** `valuation-letters/{policyId}/{valuationLetterId}.pdf`.

### 5. API Usage
#### A. Manage Authorized Valuers
- `POST /api/v1/valuers`: Add a new valuer (Admin only).
- `GET /api/v1/valuers`: List all active valuers.
- `PUT /api/v1/valuers/{id}`: Update valuer details.
- `DELETE /api/v1/valuers/{id}`: Deactivate a valuer.

#### B. Valuation Letters & Verification
- `POST /api/v1/policies/{policyId}/valuation-letter`: Trigger generation. Includes UUID generation, metadata embedding, and hashing.
- `GET /verify/doc/{documentUuid}`: Public endpoint to verify document status via ID (used by QR codes).
- `POST /verify/upload`: Secure endpoint to verify a PDF file against its registered cryptographic hash.
- `GET /api/v1/valuation-letters/{id}/download`: Obtain a secure download link.

#### C. Revocation
Documents can be revoked server-side (e.g., if issued in error). Once revoked, any verification attempt via QR or ID will return a **REVOKED** warning, even if the cryptographic hash remains valid.

---

## 15. How to Run Locally

### Required Environment Variables
- `RABBITMQ_HOST`: localhost
- `RABBITMQ_USER`: admin
- `RABBITMQ_PASS`: admin
- `REDIS_HOST`: localhost
- `REDIS_PORT`: 6379
- `DB_URL`: jdbc:postgresql://localhost:5432/isec_insurance_db
- `KEYCLOAK_ISSUER_URI`: http://localhost:9050/realms/isec-insurance

### Starting Dependencies
We recommend using Docker Compose for dependencies:
```bash
docker-compose up -d rabbitmq redis postgres keycloak
```

### Starting the Application
```bash
./mvnw clean install
./mvnw spring-boot:run -pl app-bootstrap
```

---

## 14. How to Test the Full E2E Flow

### 1. Trigger a Certificate Request
Perform a payment that satisfies the business rules (at least 35% of annual premium).
```bash
# Example via Postman or Curl
POST /api/v1/payments/stk-push
```

### 2. Observe Async Processing
Monitor the logs for:
- `Creating pending MONTH_1 certificate for policy...`
- `Received certificate request event...`
- `DMVIC issued certificate with reference...`

### 3. Verify Certificate Issuance
Check the database or the API:
```bash
GET /api/v1/certificates/policy/{policyId}
# Verify status is ISSUED and dmvicReference is present.
```

### 4. Verify Notifications
Check logs for `[MOCK EMAIL]` or `[MOCK SMS]` entries confirming delivery.

### 5. Verify Valuation Letter
Trigger a valuation letter:
```bash
POST /api/v1/notifications/valuation-letter/{policyId}
```
Observe logs for `Generating valuation letter...` and subsequent notification logs.

---

## 15. Reporting
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
