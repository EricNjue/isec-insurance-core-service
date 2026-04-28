# Integrations Module

This module handles all external integrations with insurance partners and payment providers. It is designed to be extensible, allowing new partners to be added with minimal changes to the core application logic.

## Architecture Patterns

The module employs two main integration patterns:

1.  **Insurance Partner Integration**: For core insurance services like quoting, double insurance checks, and reference data.
2.  **M-Pesa Payment Integration**: For handling mobile money payments through different insurance partner gateways or directly.

---

## 1. Insurance Partner Integration

Insurance partners (e.g., Sanlam, APA, Jubilee) implement the `InsuranceIntegrationAdapter` interface.

### Key Components
- `InsuranceIntegrationAdapter`: The core interface defining mandatory operations for any insurance partner.
- `SanlamIntegrationAdapter`: Current implementation for Sanlam.

### Adding a New Insurance Partner
To add a new insurance partner:
1.  **Create a new package**: `com.isec.platform.modules.integrations.<partner_name>`.
2.  **Implement `InsuranceIntegrationAdapter`**: Create a class (e.g., `ApaIntegrationAdapter`) that implements the required methods.
3.  **Define DTOs**: Create partner-specific Request/Response DTOs within the partner's package.
4.  **Mapper**: Create a mapper to convert between the core module's common models and the partner's DTOs.
5.  **Client**: Implement a reactive HTTP client to communicate with the partner's API.
6.  **Configuration**: Add partner-specific configurations in `application.yml` under `integrations.<partner_name>`.

---

## 2. M-Pesa Payment Integration

M-Pesa integrations are structured using a **Provider/Adapter** pattern to support multiple gateways (some partners provide their own M-Pesa proxy APIs).

---

## 3. Motor Premium Calculation Integration

Motor premium calculation uses a **Provider/Factory** pattern to support multiple insurance partners with different rating engines and API specifications.

### Directory Structure
```text
integrations
  └── premium
      ├── provider
      │   ├── PremiumCalculationProvider (Interface)
      │   ├── PremiumProviderType (Enum)
      │   └── PremiumCalculationProviderFactory (Resolver)
      ├── model (Partner-Agnostic Models)
      │   ├── PremiumCalculationRequest
      │   ├── PremiumCalculationResponse
      │   ├── PremiumBenefitBreakdown
      │   ├── PremiumGrossBreakdown
      │   ├── PremiumCalculationMetadata
      │   └── PremiumCalculationStatus
      └── <partner_name> (Partner-Specific Implementation)
          ├── client (HTTP Client)
          ├── provider (Provider Implementation)
          ├── dto (Partner DTOs)
          ├── mapper (Model Conversion)
          └── config (Configuration Properties)
```

### How it Works
1.  **PremiumCalculationProviderFactory**: Automatically collects all Spring-managed beans implementing `PremiumCalculationProvider`.
2.  **Provider Selection**: The factory resolves the correct provider based on the `PremiumProviderType` (e.g., `SANLAM`).
3.  **Abstraction**: Core services use the factory and the `PremiumCalculationProvider` interface, remaining oblivious to partner-specific API details.

### Adding a New Premium Partner
To add a new partner (e.g., `APA`):

1.  **Update `PremiumProviderType`**:
    Add `APA` to the enum.

2.  **Create Implementation Package**:
    `com.isec.platform.modules.integrations.premium.apa`

3.  **Implement `PremiumCalculationProvider`**:
    ```java
    @Service
    public class ApaPremiumCalculationProvider implements PremiumCalculationProvider {
        @Override
        public PremiumProviderType providerType() {
            return PremiumProviderType.APA;
        }
        // Implement calculatePremium
    }
    ```

4.  **Create DTOs, Client, and Mapper**:
    - Define partner-specific request/response DTOs.
    - Implement a reactive client using `ReactiveHttpClient`.
    - Implement a mapper to convert between common models and partner DTOs.

5.  **Configure in `application.yml`**:
    ```yaml
    integrations:
      premium:
        apa:
          base-url: https://api.apa.co.ke
          calculate-premium-path: /v1/motor/rates
          timeout: 5s
    ```

### Validation Rules
Providers are responsible for validating the common `PremiumCalculationRequest` before making external calls. Standard validations include:
- `vehicleValue` > 0
- `vehicleYear` is reasonable (e.g., > 1900)
- Mandatory fields like `vehicleMake`, `vehicleModel`, `motorClass` are present.

### Resilience
- Use `ReactiveHttpClient` for standard timeout and retry support.
- Retry only transport/network errors (5xx, timeouts).
- Do not retry business/validation errors (4xx).

### Directory Structure
```text
integrations
  └── mpesa
      ├── provider
      │   ├── MpesaPaymentProvider (Interface)
      │   ├── MpesaProviderType (Enum)
      │   └── MpesaProviderFactory (Resolver)
      ├── model (Partner-Agnostic Models)
      │   ├── MpesaInitiatePaymentRequest
      │   ├── MpesaInitiatePaymentResponse
      │   ├── MpesaCheckStatusRequest
      │   └── MpesaPaymentStatusResponse
      └── <partner_name> (Partner-Specific Implementation)
          ├── client (HTTP Client)
          ├── service (Provider Implementation)
          ├── dto (Partner DTOs)
          ├── mapper (Model Conversion)
          └── config (Optional Config classes)
```

### How it Works
1.  **MpesaProviderFactory**: Automatically collects all Spring-managed beans implementing `MpesaPaymentProvider`.
2.  **Provider Selection**: The factory resolves the correct provider based on the `MpesaProviderType` (e.g., `SANLAM`).
3.  **Abstraction**: Core services use the factory and the `MpesaPaymentProvider` interface, remaining oblivious to partner-specific API details.

### Adding a New M-Pesa Partner
To add a new partner (e.g., `ICEA`):

1.  **Update `MpesaProviderType`**:
    Add `ICEA` to the enum.

2.  **Create Implementation Package**:
    `com.isec.platform.modules.integrations.mpesa.icea`

3.  **Implement `MpesaPaymentProvider`**:
    ```java
    @Service
    public class IceaMpesaProvider implements MpesaPaymentProvider {
        @Override
        public MpesaProviderType providerType() {
            return MpesaProviderType.ICEA;
        }
        // Implement initiatePayment and checkStatus
    }
    ```

4.  **Create DTOs and Mapper**:
    Define `IceaStkPushRequest`, etc., and a mapper to convert from/to `MpesaInitiatePaymentRequest`.

5.  **Configure in `application.yml`**:
    ```yaml
    integrations:
      mpesa:
        icea:
          base-url: https://api.icea.co.ke
          stk-push-path: /payments/stk
          timeout: 10s
    ```

### Mapping Rules (Standardization)
Every provider must map their partner-specific status codes to the common `MpesaPaymentStatus` enum:
- `ACCEPTED`: Request received by the gateway.
- `SUCCESS`: Payment successfully completed.
- `PENDING`: Payment still in progress (e.g., waiting for user PIN).
- `FAILED`: Payment failed.
- `CANCELLED`: User cancelled the STK push.
- `TIMEOUT`: User did not enter PIN in time.
- `UNKNOWN`: Status could not be determined.

### Observability
- All providers should log using the provider name as a prefix: `[SANLAM] Initiating payment...`.
- Ensure phone numbers are masked in logs (e.g., `254****678`).
- Log `quoteRef`, `checkoutId`, `status`, and `latency`.

### Resilience
- Use `ReactiveHttpClient` which supports standard timeout and retry configurations.
- Only retry transport errors (5xx, timeouts). Do not retry business failures (Cancelled, Pending).
