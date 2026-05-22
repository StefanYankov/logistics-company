# Logistics Company - Postman API Testing Guide

This document defines the sequential HTTP request execution required to verify all functional requirements (1 through 7) specified in the assignment, as well as the advanced validation and security features implemented in the system.

---

## Phase 1: Workspace and Environment Setup

### Step 1.1: Create a Dedicated Workspace
1. Open Postman.
2. Navigate to **Workspaces** -> **Create Workspace**.
3. Assign Name: `Logistics Company App`.
4. Set Visibility to **Personal**.

### Step 1.2: Configure Environment Variables
1. Navigate to **Environments** -> **Create Environment**.
2. Assign Name: `Logistics-Local`.
3. Define the following variables (leave Initial/Current values blank unless specified):
   * `baseUrl` (Initial Value: `http://localhost:8080/api`)
   * `admin_token`
   * `client_token`
   * `clerk_token`
   * `courier_token`
   * `client_sender_id`
   * `client_receiver_id`
   * `company_id`
   * `office_id`
   * `clerk_id`
   * `courier_id`
   * `shipment_id`
   * `tracking_number`
4. Save the environment and set it as the Active Environment.

---

## 🗂️ Phase 2: Collection Structure

Create a new Collection named `Logistics API - Integration Flow`. Create the following folders within it to organize the testing sequence:
1. `01. Public IAM (Req 1, 2)`
2. `02. System Setup (Req 3a, 3b, 3г)`
3. `03. Operations - Employees (Req 4)`
4. `04. Reporting (Req 5, 6)`
5. `05. Client Portal & Security (Req 7)`
6. `06. Advanced Operations (Addons, Guest Receivers, Office Pickup)`
7. `07. Edge Cases & Defense in Depth (Bonus)`

---

## 🚀 Phase 3: The End-to-End Execution Flow

> **Note on Scripts:** All scripts provided below must be placed in the **Scripts -> Post-response** tab in Postman. These scripts execute *after* the HTTP response is received to automatically parse and store the state variables.

### Folder: `01. Public IAM (Req 1, 2)`

#### Request 1: Register Sender Client
* **Method:** `POST`
* **URL:** `{{baseUrl}}/clients/register`
* **Body (raw JSON):**
  ```json
  {
      "username": "sender_client",
      "email": "sender@logistics.com",
      "password": "SecurePassword123!",
      "firstName": "Alice",
      "lastName": "Smith",
      "phoneNumber": "0888111222"
  }
  ```
* **Post-response Script:**
  ```javascript
  if (pm.response.code === 201) {
      pm.environment.set("client_sender_id", pm.response.json().id);
  }
  ```
* **Action:** Click **Send**. Note that `isEmailVerified` is `false`. **Check your backend console log** to find the printed mock email and copy the raw UUID token for the next step.

#### Request 2: Verify Sender Client Email
* **Method:** `GET`
* **URL:** `{{baseUrl}}/clients/verify?token=PASTE_COPIED_TOKEN_HERE`
* **Expected:** `200 OK`. The client account is now fully verified and activated.

#### Request 3: Register Receiver Client
* **Method:** `POST`
* **URL:** `{{baseUrl}}/clients/register`
* **Body (raw JSON):**
  ```json
  {
      "username": "receiver_client",
      "email": "receiver@logistics.com",
      "password": "SecurePassword123!",
      "firstName": "Bob",
      "lastName": "Jones",
      "phoneNumber": "0888333444"
  }
  ```
* **Post-response Script:**
  ```javascript
  if (pm.response.code === 201) {
      pm.environment.set("client_receiver_id", pm.response.json().id);
  }
  ```
* **Action:** (Optional) Copy the token from the backend console and verify this user as well, using the same URL structure from Request 2.

#### Request 4: Login as Client (Sender)
* **Method:** `POST`
* **URL:** `{{baseUrl}}/auth/login`
* **Body (raw JSON):**
  ```json
  {
      "username": "sender_client",
      "password": "SecurePassword123!"
  }
  ```
* **Post-response Script:**
  ```javascript
  if (pm.response.code === 200) {
      pm.environment.set("client_token", pm.response.json().token);
  }
  ```

#### Request 5: Request Password Reset
* **Method:** `POST`
* **URL:** `{{baseUrl}}/auth/forgot-password`
* **Body (raw JSON):**
  ```json
  {
      "email": "sender@logistics.com"
  }
  ```
* **Action:** Click **Send**. Check your backend console log for the `PASSWORD RESET` mock email. Copy the raw UUID token.

#### Request 6: Execute Password Reset
* **Method:** `POST`
* **URL:** `{{baseUrl}}/auth/reset-password`
* **Body (raw JSON):**
  ```json
  {
      "token": "PASTE_RESET_TOKEN_HERE",
      "newPassword": "NewStrongPassword123!"
  }
  ```
* **Expected:** `200 OK`. The client's password is now changed.

---

### Folder: `02. System Setup (Req 3a, 3b, 3г)`

*(Assumes an Admin user `admin` with password `password123` was seeded via DataSeeder).*

#### Request 7: Login as Admin
* **Method:** `POST`
* **URL:** `{{baseUrl}}/auth/login`
* **Body (raw JSON):** `{"username": "admin", "password": "password123"}`
* **Post-response Script:**
  ```javascript
  if (pm.response.code === 200) { pm.environment.set("admin_token", pm.response.json().token); }
  ```

*For Requests 8-11, configure the **Authorization** tab to use **Bearer Token** with `{{admin_token}}`.*

#### Request 8: Create Company
* **Method:** `POST`
* **URL:** `{{baseUrl}}/companies`
* **Body (raw JSON):**
  ```json
  {
      "name": "Fast Delivery OOD",
      "registrationNumber": "BG123456789",
      "addressDetails": {
          "cityId": 1,
          "street": "Tsarigradsko Shose 115"
      }
  }
  ```
* **Post-response Script:** `if (pm.response.code === 201) pm.environment.set("company_id", pm.response.json().id);`

#### Request 9: Create Office
* **Method:** `POST`
* **URL:** `{{baseUrl}}/offices`
* **Body (raw JSON):**
  ```json
  {
      "companyId": {{company_id}},
      "address": { "cityId": 1, "street": "Main Blvd 10" },
      "operatingHours": [
          {
              "dayOfWeek": "MONDAY",
              "openTime": "09:00:00",
              "closeTime": "18:00:00",
              "isClosed": false
          }
      ]
  }
  ```
* **Post-response Script:** `if (pm.response.code === 201) pm.environment.set("office_id", pm.response.json().id);`

#### Request 10: Register Office Clerk
* **Method:** `POST`
* **URL:** `{{baseUrl}}/employees`
* **Body (raw JSON):**
  ```json
  {
      "username": "clerk1",
      "email": "clerk1@logistics.com",
      "password": "SecurePassword123!",
      "firstName": "Ivan",
      "lastName": "Ivanov",
      "hireDate": "2026-01-01",
      "salary": 2000.00,
      "applicationRole": "CLERK",
      "officeId": {{office_id}}
  }
  ```
* **Post-response Script:** `if (pm.response.code === 201) pm.environment.set("clerk_id", pm.response.json().id);`

#### Request 11: Register Courier
* **Method:** `POST`
* **URL:** `{{baseUrl}}/employees`
* **Body (raw JSON):** Similar to Request 10, omit `officeId`, change username to `courier1`, set role to `COURIER`. Save to `courier_id`.

---

### Folder: `03. Operations - Employees (Req 4)`

#### Request 12 & 13: Authenticate Staff
1. Repeat the Login request for `clerk` (seeded user) and save the token to `clerk_token`.
2. Repeat the Login request for `courier` (seeded user) and save the token to `courier_token`.

#### Request 14: Register Shipment (Clerk Action)
* **Auth:** Bearer Token -> `{{clerk_token}}`
* **Method:** `POST`
* **URL:** `{{baseUrl}}/shipments`
* **Body (raw JSON):**
  ```json
  {
      "senderId": "{{client_sender_id}}",
      "receiverId": "{{client_receiver_id}}",
      "originOfficeId": 1,
      "type": "PARCEL",
      "weight": 2.5,
      "deliveryOfficeId": {{office_id}},
      "paidBy": "SENDER"
  }
  ```
* **Post-response Script:**
  ```javascript
  if (pm.response.code === 201) {
      pm.environment.set("shipment_id", pm.response.json().id);
      pm.environment.set("tracking_number", pm.response.json().trackingNumber);
  }
  ```
* **Showcase:** Review the `totalPrice` in the response, proving the dynamic Pricing Engine algorithm executed successfully.

#### Request 15: Update Status to Transit (Clerk Action)
* **Auth:** Bearer Token -> `{{clerk_token}}`
* **Method:** `PATCH`
* **URL:** `{{baseUrl}}/shipments/{{shipment_id}}/status`
* **Body (raw JSON):** `{"newStatus": "IN_TRANSIT"}`
* **Expected:** `200 OK`. State Machine progresses perfectly.

#### Request 16: Update Status to Delivered (Courier Action)
* **Auth:** Bearer Token -> `{{courier_token}}`
* **Method:** `PATCH`
* **URL:** `{{baseUrl}}/shipments/{{shipment_id}}/status`
* **Body (raw JSON):** `{"newStatus": "DELIVERED"}`
* **Expected:** `200 OK`. State Machine terminates successfully.

---

### Folder: `04. Reporting (Req 5, 6)`

*Execute these requests with `{{admin_token}}` or `{{clerk_token}}` to verify staff visibility.*

*   **Req 5a (All Employees):** `GET {{baseUrl}}/employees` (Admin only)
*   **Req 5b (All Clients):** `GET {{baseUrl}}/clients` (Admin only)
*   **Req 5c (All Shipments):** `GET {{baseUrl}}/shipments` (Clerk/Courier access - Req 6)
*   **Req 5d (By Employee):** `GET {{baseUrl}}/shipments/registered-by/{{clerk_id}}`
*   **Req 5e (Pending):** `GET {{baseUrl}}/shipments/pending`
*   **Req 5h (Revenue):** `GET {{baseUrl}}/shipments/revenue?startDate=2026-01-01&endDate=2026-12-31` (Admin only)

---

### Folder: `05. Client Portal & Security (Req 7)`

*Execute these requests with `{{client_token}}`.*
*(Make sure to log in using the `NewStrongPassword123!` if you executed the reset flow).*

#### Request 17: Register Shipment (Client Self-Service)
* **Auth:** Bearer Token -> `{{client_token}}`
* **Method:** `POST`
* **URL:** `{{baseUrl}}/shipments`
* **Body (raw JSON):**
  ```json
  {
      "senderId": "{{client_sender_id}}",
      "receiverId": "{{client_receiver_id}}",
      "originOfficeId": 1,
      "type": "PARCEL",
      "weight": 1.5,
      "deliveryOfficeId": {{office_id}},
      "paidBy": "SENDER"
  }
  ```
* **Expected:** `201 Created`. Verifies a client can register their own shipment.

#### Request 18: Track Own Shipment (Success)
* **Method:** `GET`
* **URL:** `{{baseUrl}}/shipments/track/{{tracking_number}}`
* **Expected:** `200 OK`. Fulfills Req 7 (Client views own shipment).

#### Request 19: View Sent Shipments (Req 5f)
* **Method:** `GET`
* **URL:** `{{baseUrl}}/shipments/sender/{{client_sender_id}}`
* **Expected:** `200 OK`.

#### Request 20: Security Validation (Forbidden Report Access)
* **Method:** `GET`
* **URL:** `{{baseUrl}}/shipments/revenue?startDate=2026-01-01&endDate=2026-12-31`
* **Expected:** `403 Forbidden`. Verifies authorization boundaries isolate administrative endpoints from standard users.

---

### Folder: `06. Advanced Operations (Addons, Guest Receivers, Office Pickup)`

#### Request 21: Quick Register Walk-in Client
* **Auth:** Bearer Token -> `{{clerk_token}}`
* **Method:** `POST` `{{baseUrl}}/clients/quick-register`
* **Body:**
  ```json
  {
      "firstName": "Walkin",
      "lastName": "Customer",
      "phoneNumber": "0888999000"
  }
  ```
* **Expected:** `200 OK`. Creates a client profile without an email.

#### Request 22: Register Shipment with Addons & Guest Receiver
* **Auth:** Bearer Token -> `{{clerk_token}}`
* **Method:** `POST` `{{baseUrl}}/shipments`
* **Body:**
  ```json
  {
      "senderId": "{{client_sender_id}}",
      "receiverName": "Guest Receiver",
      "receiverPhone": "0888777666",
      "originOfficeId": 1,
      "type": "PARCEL",
      "weight": 5.0,
      "deliveryOfficeId": 1,
      "selectedServiceIds": [1, 2]
  }
  ```
* **Post-response Script:** `pm.environment.set("pickup_shipment_id", pm.response.json().id);`
* **Expected:** `201 Created`. Showcases XOR validation (using guest details instead of receiver ID) and dynamic pricing applied for services 1 & 2 (e.g., Fragile, SMS).

#### Request 23: Deliver via Office Pickup (Clerk Action)
* **Auth:** Bearer Token -> `{{clerk_token}}`
* **Method:** `PATCH` `{{baseUrl}}/shipments/{{pickup_shipment_id}}/status`
* **Pre-requisite:** First update status to `IN_TRANSIT`, then `AT_DELIVERY_OFFICE`.
* **Body:** `{"newStatus": "DELIVERED"}`
* **Expected:** `200 OK`. Showcases the business logic allowing Clerks to hand over packages directly from the office.

---

### Folder: `07. Edge Cases & Defense in Depth (Bonus)`

*This folder showcases the robust error handling, RFC 9457 ProblemDetail mapping, and business rules implemented beyond the basic requirements.*

#### Request 24: Registration Duplicate Conflict (409)
* **Method:** `POST` `{{baseUrl}}/clients/register`
* **Body:** Send the exact same payload used in Request 1.
* **Expected:** `409 Conflict`. Showcases proactive database integrity checks preventing duplicate usernames/emails, returning a custom `urn:logistics:business-error`.

#### Request 25: Negative Weight Validation (400)
* **Method:** `POST` `{{baseUrl}}/shipments` (Use `{{clerk_token}}`)
* **Body:** Send a shipment with `"weight": -5.0`.
* **Expected:** `400 Bad Request`. Showcases JSR-380 input validation intercepting mathematically impossible pricing values.

#### Request 26: State Machine Violation (400)
* **Method:** `PATCH` `{{baseUrl}}/shipments/{{shipment_id}}/status` (Use `{{clerk_token}}`)
* **Body:** Send `{"newStatus": "IN_TRANSIT"}`.
* **Expected:** `400 Bad Request`. The shipment is already `DELIVERED` (from Request 16). Showcases the strict State Machine preventing packages from going backward in status.

#### Request 27: ID Enumeration Protection (404)
* **Method:** `GET` `{{baseUrl}}/shipments/track/TRK-UNKNOWN-123` (Use `{{client_token}}`)
* **Expected:** `404 Not Found`. Showcases that the API safely hides the existence of invalid data (or data belonging to other users) rather than throwing internal server errors or 403s.
