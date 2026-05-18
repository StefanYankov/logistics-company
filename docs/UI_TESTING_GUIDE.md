# Logistics Company - End-to-End UI Testing Guide

This guide provides a comprehensive manual testing flow using the Angular frontend to verify the core features of the Logistics Company system, specifically focusing on staff workflows, guest receivers, service addons, and office pickups.

## 🧪 The "Walk-in & Pickup" Scenario

This flow uses the seeded `clerk` account to test the system's ability to handle walk-in customers, apply dynamic pricing via addons, and deliver packages directly from an office.

### Prerequisites
*   The Spring Boot backend must be running.
*   The database must be migrated (including the `DataSeeder` execution which creates the default staff users).
*   The Angular frontend must be running (`npm start` or `ng serve`).

---

### Step 1: Log in as the Office Clerk
1. Open your browser and navigate to `http://localhost:4200`.
2. Click **Login** in the top navigation bar.
3. Enter the seeded credentials:
   * **Username:** `clerk`
   * **Password:** `password123`
4. Click **Submit**.
   * *Expected Outcome:* You should be instantly redirected to the **Shipments Dashboard** (`/app/shipments`).

### Step 2: Quick-Register a Walk-in Customer
1. On the left sidebar, click **Office Registration** (or Register New Shipment).
2. Under the **Sender** section, type `0888999111` into the search box.
3. Wait half a second for the debounce timer. A **"Quick Register"** button will appear below the search bar. Click it.
4. A panel will open. Notice the phone number is already pre-filled. Fill in the rest:
   * **First Name:** `Petar`
   * **Last Name:** `Petrov`
   * **Email:** *(Leave this completely blank to verify the system allows offline-only clients).*
5. Click **Quick Register**.
   * *Expected Outcome:* The panel closes, the API registers the client, and "Petar Petrov (0888999111)" is automatically locked in as the Sender for this shipment.

### Step 3: Register a Shipment with Addons
1. Scroll down to the **Receiver** section. Fill in details to test the "Guest Receiver" feature (a recipient without a system account):
   * **Full Name:** `Maria Ivanova`
   * **Phone:** `0888777666`
2. Scroll to the **Destination** section:
   * **Type:** Select `Office`
   * **City Filter:** Select `Plovdiv` (or another available city).
   * **Office:** Select the available office from the dropdown.
3. Scroll to the **Package & Payment** section:
   * **Weight:** `5.0`
4. Scroll to the **Additional Services** section:
   * Check the box for **Fragile (+ 5.00 BGN)**.
   * Check the box for **SMS Notification (+ 0.20 BGN)**.
5. Click the large **Register Shipment** button at the bottom.
   * *Expected Outcome:* The shipment is saved to the database, and you are automatically redirected back to the **All Shipments** dashboard.

### Step 4: Verify the Dashboard Badges
1. Look at the top row of your Shipments table (the shipment you just created).
2. Look at the **Financials** column.
   * *Expected Outcome:* You should see a base price plus the accurately calculated Addon costs.
   * Below the price, you should visually see the grey badge bubbles: `[Fragile]` and `[SMS Notification]`.

### Step 5: Perform an "Office Pickup" Delivery
1. On that same row, look at the **Actions (Staff)** column.
2. The package is currently `REGISTERED`. Click **Mark In Transit**.
   * *The status updates.*
3. Now click **Arrived at Office** (simulating the package reached the destination office).
   * *The status updates to AT_DELIVERY_OFFICE.*
4. Look at the buttons available now. You should see a distinctive blue button: **Deliver (Pickup)**.
5. Click **Deliver (Pickup)** (simulating Maria Ivanova walked into the office to claim her package directly from the clerk).
   * *Expected Outcome:* The status updates to a green `DELIVERED` badge. The lifecycle is successfully completed without requiring courier assignment!

---

## 🧭 The "Smart Routing & Tracking" Scenario

This flow verifies that authenticated users are directed to the correct portals and that the public tracking system functions securely.

### Step 1: Verify Role-Based Smart Home Page
1. While logged in as `clerk` from the previous scenario, click the **📦 LogisticsCo** logo in the top-left corner of the sidebar, or manually navigate to `http://localhost:4200/`.
   * *Expected Outcome:* You are immediately redirected back to the Staff Dashboard (`/app/shipments`). You cannot access the public landing page while authenticated as staff.
2. Click **Logout**.
3. Log in using a regular **Client** account (e.g., `client@example.com` / `SecurePassword123!`).
4. Click the logo to navigate to `http://localhost:4200/`.
   * *Expected Outcome:* You are instantly redirected to the Client Dashboard (`/app`).

### Step 2: Test Public Anonymous Tracking
1. Click **Logout**. You are now an anonymous user on the public home page.
2. Enter the Tracking Number generated from Scenario 1 (e.g., `TRK-A1B2C3D4`) into the search bar and hit Enter.
3. *Expected Outcome:* You are directed to `/track/TRK-A1B2C3D4`. The tracking page displays the full shipment details (Names, Status, Origin/Destination routing, Addon badges) without requiring a login.

### Step 3: Test Quick Tracking from Staff Dashboard
1. Log in as `clerk` again.
2. At the top of the **All Shipments** dashboard, locate the new "Quick Track" search input.
3. Enter a valid tracking number and click **Track**.
4. *Expected Outcome:* You are directed to the same rich `/track/:trackingNumber` view, proving the tracking portal is seamlessly accessible from the staff workspace.
