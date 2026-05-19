# Logistics Company - End-to-End UI Testing Guide

This guide provides a comprehensive manual testing flow using the Angular frontend to verify the core features of the Logistics Company system, specifically focusing on role-based dashboards, staff workflows, guest receivers, service addons, and office pickups.

## 🧪 The "Walk-in & Pickup" Scenario (Clerk Workflow)

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
   * *Expected Outcome:* You should be instantly redirected to the **All Shipments Dashboard** (`/app/shipments`).
   * *Observe:* The header now displays staff-specific navigation links and a "Logout" button, and the "Login" / "Register" buttons are gone.

### Step 2: Quick-Register a Walk-in Customer
1. In the left sidebar, click **Register Shipment** (this is the Clerk Registration form).
2. Under the **Sender** section, type `0888999111` into the search box.
3. Wait half a second for the debounce timer. A **"Quick Register"** button will appear below the search bar. Click it.
4. A panel will open. Notice the phone number is already pre-filled. Fill in the rest:
   * **First Name:** `Petar`
   * **Last Name:** `Petrov`
   * **Email:** *(Leave this completely blank to verify the system allows offline-only clients).*
5. Click **Save & Select**.
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
5. Click the large **Confirm Shipment** button at the bottom.
   * *Expected Outcome:* The shipment is saved to the database, and you are automatically redirected back to the **All Shipments** dashboard.

### Step 4: Perform an "Office Pickup" Delivery
1. On the **All Shipments** dashboard, locate the row for the shipment you just created.
2. The package is currently `REGISTERED`. Click **Mark In Transit**.
   * *The status updates.*
3. Now click **Arrived at Office** (simulating the package reached the destination office).
   * *The status updates to AT_DELIVERY_OFFICE.*
4. Look at the buttons available now. You should see a distinctive blue button: **Deliver (Pickup)**.
5. Click **Deliver (Pickup)** (simulating Maria Ivanova walked into the office to claim her package directly from the clerk).
   * *Expected Outcome:* The status updates to a green `DELIVERED` badge. The lifecycle is successfully completed without requiring courier assignment!

---

## 🧭 The "Role-Based Dashboards & Tracking" Scenario

This flow verifies that authenticated users are directed to the correct portals and that the public tracking system functions securely, and couriers have their dedicated task views.

### Step 1: Verify Role-Based Smart Home Page
1. While logged in as `clerk` from the previous scenario, click the **📦 LogisticsCo** logo in the header, or manually navigate to `http://localhost:4200/`.
   * *Expected Outcome:* You are immediately redirected back to the **All Shipments Dashboard** (`/app/shipments`). You cannot access the public landing page while authenticated as staff.
2. Click **Logout** (from the header).
3. Log in using a regular **Client** account (e.g., `sender@logistics.com` / `SecurePassword123!`).
   * *Expected Outcome:* You are instantly redirected to the **Client Dashboard** (`/app`).
   * *Observe:* The header now displays client-specific navigation links and a "Logout" button.
4. Click **Logout** (from the header).

### Step 2: Test Public Anonymous Tracking
1. You are now an anonymous user on the public home page.
2. Enter the Tracking Number generated from Scenario 1 (e.g., `TRK-A1B2C3D4`) into the search bar and hit Enter.
3. *Expected Outcome:* You are directed to `/track/TRK-A1B2C3D4`. The tracking page displays the **public-restricted data** (e.g., tracking number, status, origin/destination cities, weight, addons) without requiring a login. **Crucially, no sender/receiver names, phone numbers, or financial details should be visible.**

### Step 3: Test Courier Dashboard
1. Log in as a **Courier** (e.g., `courier1` / `SecurePassword123!`).
   * *Expected Outcome:* You are immediately redirected to the **All Shipments** dashboard (`/app/shipments`).
   * *Observe:* The header now displays staff-specific navigation links (including "All Shipments", "Register Shipment") and a "Logout" button.
2. In the left sidebar, you should now see a new link: **My Tasks**. Click it.
   * *Expected Outcome:* You are navigated to the **Courier Dashboard** (`/app/my-tasks`).
   * You should see two sections: "My Pickups" and "My Deliveries". Both should initially be empty if no tasks are assigned to this courier.
   * **To test functionality:**
     * Log in as a `clerk`. Register a new shipment with a client address as the origin.
     * Assign this shipment to the `courier1` (this functionality is not yet built in the UI, but can be simulated via backend tools like Postman by updating the `current_courier_id` of the shipment to `courier1`'s ID).
     * Log back in as `courier1`. The shipment should now appear under "My Pickups".
     * Update a shipment's status to `OUT_FOR_DELIVERY` and assign it to `courier1` (again, via Postman or a future UI). It should appear under "My Deliveries".

---

