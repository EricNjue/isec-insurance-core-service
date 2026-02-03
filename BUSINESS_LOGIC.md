# Business Logic & State Machines

## 1. Application Lifecycle
The Application lifecycle tracks the journey from initial data entry to policy issuance.

**States:**
*   `DRAFT`: Initial state. User is entering vehicle and owner details.
*   `SUBMITTED`: Data entry complete. Pending rating and document review.
*   `QUOTED`: Rating applied. User can see the premium breakdown.
*   `DOCUMENTS_UPLOADED`: Mandatory documents (Logbook, ID, KRA PIN) provided.
*   `PAYMENT_PENDING`: Quote accepted. Waiting for M-Pesa settlement.
*   `PARTIALLY_PAID`: At least one payment received, but balance remains.
*   `FULLY_PAID`: Full annual premium settled.
*   `POLICY_ISSUED`: Policy record created and active.
*   `CANCELLED`: Application terminated before completion.

**Guard Conditions:**
*   `DRAFT` → `SUBMITTED`: Requires all mandatory vehicle fields.
*   `QUOTED` → `DOCUMENTS_UPLOADED`: Requires successful metadata check for all 4 mandatory documents.
*   `PAYMENT_PENDING` → `PARTIALLY_PAID`: Requires M-Pesa callback with `ResultCode: 0`.

---

## 2. Payment Lifecycle
Tracks the M-Pesa STK Push flow.

**States:**
*   `INITIATED`: Request created in our DB.
*   `STK_SENT`: Safaricom acknowledged the request (CheckoutID received).
*   `COMPLETED`: Success callback received.
*   `FAILED`: Error callback received or request expired.

**Business Rules:**
*   **Max 4 Partial Payments**: System blocks any STK Push request if 4 successful/pending payments already exist for the application.
*   **Max Amount**: Individual payment capped at 1,000,000 KES.
*   **Idempotency**: `CheckoutRequestID` is used to ensure callbacks are processed exactly once.

---

## 3. Certificate Issuance (35% Rule)
Enforces regulatory and business constraints on certificate delivery.

**Types:**
*   `MONTH_1`: Valid for 30 days from policy start.
*   `MONTH_2`: Valid for 30 days following Month 1.
*   `ANNUAL_REMAINDER`: Valid for the remaining ~10 months.
*   `ANNUAL_FULL`: Single certificate valid for the full year.

**Issuance Logic:**
1.  **Month 1**: Issued if `TotalPaid >= 0.35 * AnnualPremium`.
2.  **Month 2**: Issued if `TotalPaid >= 0.70 * AnnualPremium`.
3.  **Month 3 / Annual**: Blocked until `Balance == 0`.

**Maturity Date Calculation:**
`MaturityDate = StartDate + 1 Year - 1 Day`.

---

## 4. Failure Handling & Retries
*   **DMVIC Integration**: If the DMVIC API fails during issuance, the task is pushed to a Dead-Letter Queue (DLQ) with a retry limit of 3 (exponential backoff).
*   **Notification Dispatch**: Failed emails/SMS are retried via RabbitMQ retry queues.
