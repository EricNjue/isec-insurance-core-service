# M-PESA Daraja STK Push Integration Module

This module provides a pure Spring Boot integration with Safaricom M-PESA Daraja APIs.

## Supported APIs

1. **OAuth Token Generation**: Generates and caches the access token in Redis.
2. **STK Push (Process Request)**: Initiates an STK Push to a customer's phone.
3. **STK Push Query**: Queries the status of an STK Push request.
4. **Callback Handling**: DTOs for parsing Safaricom callbacks.

## Configuration

The following environment variables are required:

- `MPESA_CONSUMER_KEY`: Your Daraja App Consumer Key
- `MPESA_CONSUMER_SECRET`: Your Daraja App Consumer Secret
- `MPESA_SHORTCODE`: Business Short Code (e.g., 174379)
- `MPESA_PASSKEY`: Online Passkey
- `MPESA_CALLBACK_URL`: URL where Safaricom will send callbacks
- `MPESA_OAUTH_URL`: OAuth endpoint (e.g., `https://sandbox.safaricom.co.ke/oauth/v1/generate`)
- `MPESA_STK_PUSH_URL`: STK Push endpoint (e.g., `https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest`)
- `MPESA_STK_QUERY_URL`: STK Query endpoint (e.g., `https://sandbox.safaricom.co.ke/mpesa/stkpushquery/v1/query`)

## Flow Diagrams

### Token Flow
1. Client requests an action (STK Push).
2. `MpesaOAuthService` checks Redis for a valid `mpesa_access_token`.
3. If not found or expired, it calls the Daraja OAuth API.
4. Token is cached in Redis with TTL (expires_in - 60s).

### STK Push Flow
1. `MpesaStkService.initiateStkPush` is called.
2. Generates `Timestamp` (yyyyMMddHHmmss) and `Password` (Base64 of shortcode+passkey+timestamp).
3. Fetches Access Token.
4. Sends POST request to Safaricom.
5. Returns `CheckoutRequestID` and `ResponseCode`.

### Callback Handling
1. Safaricom sends a POST request to `MPESA_CALLBACK_URL`.
2. Use `MpesaDtos.CallbackRequest` to parse the payload.
3. `ResultCode` 0 indicates success.
4. `CallbackMetadata` contains details like `MpesaReceiptNumber` and `Amount`.

## Sample Payloads

### STK Push Request
```json
{
  "BusinessShortCode": "174379",
  "Password": "...",
  "Timestamp": "20260207153023",
  "TransactionType": "CustomerPayBillOnline",
  "Amount": 1500,
  "PartyA": "254712345678",
  "PartyB": "174379",
  "PhoneNumber": "254712345678",
  "CallBackURL": "https://your-domain.com/mpesa/callback",
  "AccountReference": "INV-001",
  "TransactionDesc": "Lipa Na MPESA"
}
```

### Callback Payload
```json
{
  "Body": {
    "stkCallback": {
      "MerchantRequestID": "29115-34620561-1",
      "CheckoutRequestID": "ws_CO_07022026153023456",
      "ResultCode": 0,
      "ResultDesc": "The service request is processed successfully.",
      "CallbackMetadata": {
        "Item": [
          { "Name": "Amount", "Value": 1500 },
          { "Name": "MpesaReceiptNumber", "Value": "QFT7Z9A2BC" },
          { "Name": "TransactionDate", "Value": 20260207153045 },
          { "Name": "PhoneNumber", "Value": 254712345678 }
        ]
      }
    }
  }
}
```
