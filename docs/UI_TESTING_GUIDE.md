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

### Step 3: Register a Shipment with Addons (Originating from Address)
1. Scroll down to the **Receiver** section. Fill in details to test the "Guest Receiver" feature (a recipient without a system account):
   * **Full Name:** `Maria Ivanova`
   * **Phone:** `0888777666`
2. Scroll to the **Destination** section:
   * **Type:** Select `Office`
   * **City Filter:** Select `Plovdiv` (or another available city).
   * **Office:** Select the available office from the dropdown.
3. **Crucial for Pickup Test:** Scroll back up to the **Origin** section (if visible, otherwise assume it's an address pickup). For this test, ensure the shipment is registered with a **client address as the origin**, not an office. (If the form defaults to office, you'll need to manually adjust the `originOfficeId` to `null` and provide `originAddress` via Postman for a true address pickup test).
4. Scroll to the **Package & Payment** section:
   * **Weight:** `5.0`
5. Scroll to the **Additional Services** section:
   * Check the box for **Fragile (+ 5.00 BGN)**.
   * Check the box for **SMS Notification (+ 0.20 BGN)**.
6. Click the large **Confirm Shipment** button at the bottom.
   * *Expected Outcome:* The shipment is saved to the database, and you are automatically redirected back to the **All Shipments** dashboard.

### Step 4: Assign Courier for Pickup
1. On the **All Shipments** dashboard, locate the row for the shipment you just created (it should be `REGISTERED` and have an address origin).
2. In the "Actions (Staff)" column, you should now see an **Assign Pickup** button. Click it.
3. A small form will appear in the table row. Select a courier from the dropdown (e.g., `courier1`).
4. Click **Confirm**.
   * *Expected Outcome:* An alert "Pickup assigned successfully!" appears. The form disappears, and the shipment row updates to show "With: [Courier Name]" in the Status column. The status remains `REGISTERED`.

### Step 5: Edit a REGISTERED Shipment
1. On the **All Shipments** dashboard, click on the row of the shipment you just created to navigate to its details page.
2. In the header of the details page, you should see an **Edit Shipment** button. Click it.
3. You are navigated to the **Edit Shipment** page. Change the **Weight** to `7.5` and add a new service like "Heavy Duty Oversize".
4. Click **Save Changes**.
   * *Expected Outcome:* You are redirected back to the Shipment Details page. The Weight should now show `7.5 kg`, and the new addon badge should be visible. The Total Price should also be recalculated and updated.

### Step 6: Perform an "Office Pickup" Delivery
1. On the **All Shipments** dashboard, locate the row for a shipment that is `REGISTERED` and originates from an office.
2. The package is currently `REGISTERED`. Click **Mark In Transit**.
   * *Expected Outcome:* A confirmation dialog appears. Click "OK". The status updates to `IN_TRANSIT`.
3. Now click **Arrived at Office** (simulating the package reached the destination office).
   * *Expected Outcome:* The status updates to `AT_DELIVERY_OFFICE`.
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
   * You should see two sections: "My Pickups" and "My Deliveries". The shipment you assigned in Step 4 of the previous scenario should now appear under "My Pickups".
   * **To test Deliveries functionality:** Update a shipment's status to `OUT_FOR_DELIVERY` and assign it to `courier1` (via Postman or a future UI). It should appear under "My Deliveries".

---

## 👑 The "Admin User Management" Scenario

This flow verifies that an administrator can view and manage all user accounts in the system.

### Step 1: Log in as the Administrator
1. Open your browser and navigate to `http://localhost:4200`.
2. Click **Login** in the top navigation bar.
3. Enter the seeded credentials:
   * **Username:** `admin`
   * **Password:** `password123`
4. Click **Submit**.
   * *Expected Outcome:* You should be instantly redirected to the **All Shipments Dashboard** (`/app/shipments`).
   * *Observe:* The sidebar now contains a new link: **User Management**.

### Step 2: View and Manage Users
1. Click the **User Management** link in the sidebar.
2. You are navigated to the User Management page.
   * *Observe:* You should see two tabs: "Staff" and "Clients".
3. **Staff Tab:**
   * Verify that the table shows all registered employees (admin, clerk, courier).
   * Find the row for the `clerk` user. Click the **Deactivate** button.
   * *Expected Outcome:* The page reloads, and the `clerk`'s status badge should now be "Inactive".
   * Click the **Activate** button for the same user.
   * *Expected Outcome:* The page reloads, and the status badge should return to "Active".
4. **Clients Tab:**
   * Click the "Clients" tab.
   * Verify that the table shows all registered clients.
   * Find a client and perform the same Deactivate/Activate cycle.
   * *Expected Outcome:* The client's status should toggle correctly.

### Step 3: Create and Edit an Employee
1. On the User Management page, under the "Staff" tab, click the **Create New Employee** button.
2. You are navigated to the **Create Employee** page. Fill out the form to create a new `COURIER`.
3. Click **Create Employee**.
   * *Expected Outcome:* You are redirected back to the User Management page, and the new courier should appear in the "Staff" table.
4. Find the row for the newly created courier and click the **Edit** button.
5. You are navigated to the **Edit Employee** page. Change the employee's salary or name.
6. Click **Save Changes**.
   * *Expected Outcome:* You are redirected back to the User Management page, and the table should reflect the updated information.

### Step 4: Verify Access Control
1. Log out as the `admin`.
2. Log in as a `clerk`.
   * *Expected Outcome:* The "User Management" link should **not** be visible in the sidebar.
   * Manually navigate to `http://localhost:4200/app/admin/user-management`.
   * *Expected Outcome:* You should be immediately redirected back to the default dashboard (`/app/shipments`), and the admin page should not load.
3. Repeat Step 4 for a `courier` and a `client`. The outcome should be the same.
