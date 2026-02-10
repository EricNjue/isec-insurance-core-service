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
- `AFRICASTALKING_USERNAME`: Africa's Talking API username.
- `AFRICASTALKING_API_KEY`: Africa's Talking API key.
- `AFRICASTALKING_FROM`: Sender ID or Shortcode for SMS.
- `AFRICASTALKING_BASE_URL`: API URL.

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
- **SMS Integration:** Uses Africa's Talking API for SMS delivery.
- **Service Layer:** `SmsService` handles validation, client communication, and persistent storage of `SmsMessage` and `SmsRecipientResult`.
- **Delivery Reports:** Supports webhook callbacks at `POST /api/v1/sms/delivery-report` (public) to update delivery status.
- Uses idempotency to prevent spamming users on message replays.

### 5. SMS Delivery Report Callback
- **Endpoint:** `POST /api/v1/sms/delivery-report` (Public)
- **Supported Formats:** `application/x-www-form-urlencoded` and `application/json`.
- **Functionality:** Correlates Africa's Talking `messageId` with internal records and updates `deliveryStatus` and `deliveryFailureReason`.
- **Idempotency:** Enforced via unique constraint on `message_id` in the `sms_delivery_report` table.

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

## Vehicle Management API

### Vehicle Make APIs
- **POST** `/api/v1/vehicles/makes` - Create a new make (Admin only)
- **PUT** `/api/v1/vehicles/makes/{id}` - Update a make (Admin only)
- **GET** `/api/v1/vehicles/makes/{id}` - Get make details
- **GET** `/api/v1/vehicles/makes` - List makes with pagination and `active` filter
- **DELETE** `/api/v1/vehicles/makes/{id}` - Soft delete a make (Admin only)

### Vehicle Model APIs
- **POST** `/api/v1/vehicles/models` - Create a new model (Admin only)
- **PUT** `/api/v1/vehicles/models/{id}` - Update a model (Admin only)
- **GET** `/api/v1/vehicles/models/{id}` - Get model details
- **GET** `/api/v1/vehicles/models` - List models with pagination and filters (`makeId`, `makeCode`, `active`)
- **DELETE** `/api/v1/vehicles/models/{id}` - Soft delete a model (Admin only)

### Lookup APIs
- **GET** `/api/v1/vehicles/makes/{makeId}/models` - Get all models for a make ID
- **GET** `/api/v1/vehicles/makes/code/{makeCode}/models` - Get all models for a make code

### Example Request (Create Make)
```bash
curl -X POST http://localhost:8080/api/v1/vehicles/makes \
  -H "Content-Type: application/json" \
  -d '{
    "code": "TOYOTA",
    "name": "Toyota",
    "country": "Japan"
  }'
```

### Example Request (Create Model)
```bash
curl -X POST http://localhost:8080/api/v1/vehicles/models \
  -H "Content-Type: application/json" \
  -d '{
    "makeId": "uuid-of-toyota",
    "code": "COROLLA",
    "name": "Corolla 1.8 Hybrid",
    "yearFrom": 2018,
    "yearTo": 2024,
    "bodyType": "Sedan",
    "fuelType": "Hybrid"
  }'
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


---

## 16. Multi‑Tenancy, Rate Books, and New Motor Flow (2026 Update)

### Multi‑Tenant Concept
- Tenant equals insurer. Every request carries a `tenant_id` derived from JWT claim `tenant_id` or `X-Tenant-Id` header.
- DB isolation: all new pricing/rating tables include `tenant_id` with tenant‑first indexes.
- Cache isolation: rate book snapshots cached per tenant.

### Rate Books
- Active rate book is loaded per tenant (versioned, effective dates). Rules evaluated in order:
  1) Eligibility  2) Referral  3) Base premium  4) Minimum premium  5) Add‑ons
- Pricing output includes: base premium, levies (PCF/ITL), certificate charge, add‑ons, total, referral decision, applied rule IDs.

### New APIs
- Quote: `POST /api/v1/{tenantId}/motor/quotes`
  - Request: `{ category, vehicleMake, vehicleModel, yearOfManufacture, vehicleValue, registrationNumber, chassisNumber, engineNumber, quoteId?, addonRuleIds[] }`
  - Response: `{ quoteId, tenantId, category, vehicleMake, vehicleModel, yearOfManufacture, vehicleValue, rateBookId, rateBookVersion, cacheKey, pricing{...}, expiryDate }`
  - **Selecting Add-ons**: Provide a list of `rate_rule` IDs in the `addonRuleIds` field to include optional covers in the quote.
  - **Editing a Quote**: To modify an existing quote, provide the original `quoteId` in the request body. The system will recalculate the pricing and overwrite the cached entry using the same ID.
- Application (existing): `POST /api/v1/applications`
  - Extended request supports `quoteId` to convert a quote → application, persisting the pricing snapshot and setting status:
    - If referral → `UNDERWRITING_REVIEW`
    - Else → `APPROVED_PENDING_PAYMENT` and auto‑creates Policy using total premium from snapshot
- Underwriting decisions:
  - `POST /api/v1/applications/{applicationId}/underwriting/approve?underwriterId=...&comments=...`
  - `POST /api/v1/applications/{applicationId}/underwriting/decline?underwriterId=...&comments=...&reason=...`

### End‑to‑End Testing (Quote → Application → Underwriting → Payment → Issuance)
1. Obtain token and set `X-Tenant-Id` header (e.g., `SANLAM`, `APA`, `ICEA`).
2. Quote: `POST /api/v1/{tenantId}/motor/quotes` with `category=PRIVATE_CAR`, `vehicleMake=Toyota`, `vehicleModel=Corolla`, `yearOfManufacture=2018`, `vehicleValue=1200000`.
3. Create application with returned `quoteId`: `POST /api/v1/applications` with body including `quoteId` and customer/vehicle fields.
4. If application status is `UNDERWRITING_REVIEW`, approve:
   - `POST /api/v1/applications/{id}/underwriting/approve?underwriterId=uw1&comments=ok`
5. Initiate payment via existing flow: `POST /api/v1/payments/stk-push` with `applicationId`, `amount` (>= 35% on first payment), and `phoneNumber`.
6. On callback confirmation, existing issuance/documents/notifications flows are reused automatically.

### Seed Data
- Tenants: SANLAM, APA, ICEA Lion.
- Rate books: `v1.0` active for each tenant; sample rules (Private Car/Commercial) plus min‑premium/referral/add‑on.
- Make bands (sample): Toyota (Corolla, RAV4, Hilux), Mazda (Demio, CX‑5, Axela) — used via simple rule conditions (can extend to group‑based lookups later).

### Adding a New Insurer (Tenant)
1. Insert into `tenants` table and add any `tenant_configs` as needed.
2. Create a new `rate_books` row (inactive), author rules in `rate_rules`.
3. Activate the new rate book (this invalidates the rate book cache automatically when wired to activation events).
4. Test with `X-Tenant-Id` set to the new tenant ID.

### Rate Rule Management
Rate rules are defined in the `rate_rules` table and are scoped by `rate_book_id` and `tenant_id`. They use **Spring Expression Language (SpEL)** for conditions and value calculations.

#### Rule Types and Order of Evaluation
The `PricingEngine` evaluates rules in the following strict order:
1. **ELIGIBILITY**: Boolean check. If any eligible rule matches but returns `false` (or if explicit blocking rules exist), the quote is rejected.
2. **REFERRAL**: If matched, the application is flagged for manual Underwriting Review.
3. **BASE_PREMIUM**: Calculates the core rate (usually a percentage of `vehicleValue`).
4. **MIN_PREMIUM**: Ensures the base premium doesn't fall below a statutory or commercial floor.
5. **ADDON**: Optional covers added to the base premium.

#### Creating a Rate Rule
- **`condition_expression`**: A SpEL expression that must evaluate to `true` for the rule to apply. 
  - Context variables: `vehicleMake`, `vehicleModel`, `vehicleAge`, `vehicleValue`, `category`.
  - Example: `vehicleMake == 'Toyota' and vehicleAge > 5`
- **`value_expression`**: A SpEL expression that calculates the result.
  - For `BASE_PREMIUM`, `MIN_PREMIUM`, `ADDON`: Calculates the rate or fixed amount.
  - For `ELIGIBILITY`: Must return `true` or `false`.
- **`priority`**: Lower numbers are evaluated first.

### Eligibility Rule Management
Eligibility rules (`rule_type = 'ELIGIBILITY'`) act as gatekeepers. If any eligibility rule matches the request but evaluates to `false`, the quote is rejected immediately.

#### How to Define Eligibility Rules
1. **Define the Scope**: Set the `category` (e.g., `PRIVATE_CAR`) the rule applies to.
2. **Set the Logic**: Use the `condition_expression` to define when the rule should be checked, and `value_expression` to define the requirement.
   - *Note*: It's often simpler to put the requirement directly in the `condition_expression` and set `value_expression` to `'true'`.

#### Example Eligibility Rule (Value Range)
```sql
-- Rejects vehicles valued below 500k or above 15M
INSERT INTO rate_rules (rate_book_id, tenant_id, rule_type, category, description, priority, condition_expression, value_expression)
VALUES (1, 'SANLAM', 'ELIGIBILITY', 'PRIVATE_CAR', 'Value must be 500k - 15M', 1, 'vehicleValue >= 500000 and vehicleValue <= 15000000', 'true');
```

#### Example Eligibility Rule (Blacklisted Makes)
```sql
-- Rejects specific high-risk makes
INSERT INTO rate_rules (rate_book_id, tenant_id, rule_type, category, description, priority, condition_expression, value_expression)
VALUES (1, 'SANLAM', 'ELIGIBILITY', 'PRIVATE_CAR', 'High-risk luxury makes not eligible', 2, 'not (vehicleMake.equalsIgnoreCase("Ferrari") or vehicleMake.equalsIgnoreCase("Lamborghini"))', 'true');
```

### Add-on Management
Add-ons are technically a specific type of `rate_rule` (`rule_type = 'ADDON'`). 

#### How to Define Add-ons
1. **Define the Metadata**: Add an entry to `addon_definitions` if you want to track standard add-on codes across tenants.
2. **Link to Rate Book**: Insert into `rate_rules` with `rule_type = 'ADDON'`.
3. **Set the Price**: 
   - Use `value_expression` for the cost.
   - Use `condition_expression` if the add-on only applies to certain vehicles (e.g. `category == 'PRIVATE_CAR'`).

#### Example Add-on (Fixed Price)
```sql
INSERT INTO rate_rules (rate_book_id, tenant_id, rule_type, category, description, priority, value_expression)
VALUES (1, 'SANLAM', 'ADDON', 'PRIVATE_CAR', 'Excess Protector', 30, '2500');
```

#### Example Add-on (Dynamic Price with Minimum)
For covers like Excess Protector or PVT that have a percentage rate but a fixed minimum floor, use the `T(java.lang.Math).max()` function in the `value_expression`:

```sql
-- 0.5% of vehicleValue, but at least 5,000
INSERT INTO rate_rules (rate_book_id, tenant_id, rule_type, category, description, priority, value_expression)
VALUES (1, 'SANLAM', 'ADDON', 'PRIVATE_CAR', 'Excess Protector', 40, 'T(java.lang.Math).max(5000.0, vehicleValue.doubleValue() * 0.005)');
```

```sql
-- 0.45% of vehicleValue, but at least 3,000
INSERT INTO rate_rules (rate_book_id, tenant_id, rule_type, category, description, priority, value_expression)
VALUES (1, 'SANLAM', 'ADDON', 'PRIVATE_CAR', 'PVT Cover', 41, 'T(java.lang.Math).max(3000.0, vehicleValue.doubleValue() * 0.0045)');
```

### Production Notes
- Idempotent APIs and defensive validations in place (e.g., payment thresholds, callback deduplication).
- Clean logs (no sensitive data) and explicit exceptions for domain failures.
- Designed for horizontal scaling; caching layer used for rate book snapshots.
