# SmartBus – Real-Time Public Bus Tracking, Reservation, BCU & Passenger Management System

**Developed by Team 16: Giridharun S (927625BEC076), Kanishkar N (927625BEC119), and Kavin B (927625BEC122)**

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.9-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17_or_newer-orange.svg)](https://www.oracle.com/java/)
[![Database](https://img.shields.io/badge/Database-MySQL_%2F_H2_Dual_Mode-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Academic_%2F_Open_Source-purple.svg)]()

> **"Track. Travel. Reserve. Stay Connected."**

SmartBus is a comprehensive, production-grade full-stack web application designed to solve public transit uncertainty. It interconnects **Passengers**, **Transport Operators (Admins)**, the central **Bus Control Unit (BCU)**, and **Drivers/Conductors** into a unified, high-reliability transit platform with real-time GPS tracking, smart arrival alerts, visual seat reservation, digital QR ticketing, and automated fallback when GPS connectivity is lost.

---

## 1. Key Highlights & Features

### A. Passenger / Commuter Portal
- **Chronological Bus Search (>= Requested Time):** Searching for `11:10 AM` returns all subsequent available buses (`11:10`, `11:15`, `11:30`, `11:45`, `12:00`...) strictly ordered by time.
- **Buses Near Me (Geolocation Discovery):** Instantly calculates nearest bus stops using device GPS or map clicks, providing walking estimates (~80m/min) and available bus lines.
- **Live Leaflet Tracking Map:** Real-time marker animations via WebSocket STOMP without page refreshes.
- **Signal-Aware Indicators:** Clearly tags location source as **LIVE GPS**, **MANUAL LOCATION UPDATE**, or **LAST KNOWN LOCATION**.
- **Interactive Visual Seat Reservation:** 2x2 cabin seat grid (`A1` to `D10`) with database locking to guarantee zero duplicate bookings.
- **Digital Tickets with QR Code:** Auto-generated unique ticket number (`TKT-2026-000845`) and embedded ZXing QR code.
- **Downloadable PDF Tickets:** Clean, printable PDF electronic boarding passes generated via OpenPDF.
- **Smart Proximity Arrival Notifications:** Proximity threshold alerts (500m, 1km, 2km, 5km) sent when bus approaches the passenger's boarding stop.
- **Forgotten Phone Support:** Passengers can supply their **Passenger ID** or **Ticket Number** to any conductor or BCU operator for instant boarding verification.

### B. Transport Operator (Admin) Portal
- **Fleet & Route Management:** Assign buses, routes, drivers, and timetable schedules.
- **One-Click Service Cancellation:** Marking a bus `CANCELLED` or `NOT OPERATING TODAY` automatically pushes real-time notifications to all subscribed commuters.
- **Financial Analytics & Earnings Dashboard:** Interactive Chart.js visual charts for today's revenue, weekly/monthly earnings, bus-wise revenue, and route shares.
- **Official PDF Reports:** Export Daily Revenue, Monthly Revenue, and Booking audit reports as PDFs.

### C. Bus Control Unit (BCU) Central Operations
- **Full Fleet Dispatch Map:** Live color-coded telemetry of all buses across all routes.
- **Automated GPS Heartbeat Monitoring:** Backend scheduled scanner inspects GPS updates every 5 seconds. If a bus drops updates for >30 seconds, it triggers `GPS_SIGNAL_LOST` and raises immediate BCU alerts.
- **Manual Location Mode Fallback:** BCU operators can authorize drivers to enter coordinates manually if GPS fails.
- **Passenger Registry & Verification:** Look up passenger records by **Passenger ID** (e.g. `PB2026001001`) and export full Passenger Travel History PDFs.
- **Incident Management:** Full lifecycle tracking (`OPEN` → `IN_PROGRESS` → `RESOLVED`) for breakdowns, route deviations, and emergencies.
- **Live Demonstration Simulation:** Built-in scenario controller allows advancing buses along realistic route waypoints or simulating sudden GPS loss on demand.

### D. Driver & Conductor Console
- **Touch-Optimized Layout:** High-contrast, large-button console suited for tablet or mobile mounts.
- **Emergency SOS & Breakdown Reporting:** Direct one-tap priority dispatch to the BCU control room.
- **Authorized Manual Location Mode:** Unlocks coordinate submission only when authorized by BCU.
- **Conductor Ticket Scanner:** Instant QR code and Ticket Number validation; prevents reuse by marking tickets as `USED`.

---

## 2. Technology Stack

- **Backend:** Java 25, Spring Boot 4.0.9 (Spring Framework 7.0.10), Spring Data JPA, Hibernate, Spring Security (BCrypt password hashing), Spring WebSocket (STOMP + SockJS), Bean Validation.
- **Database:** **MySQL** (Default production) + **H2 Database** (Zero-configuration embedded fallback in MySQL compatibility mode).
- **PDF & Barcode Generation:** OpenPDF (`com.github.librepdf:openpdf`), ZXing (`com.google.zxing:core` & `javase`).
- **Frontend:** HTML5, CSS3, JavaScript (ES6+), Bootstrap 5.3, Leaflet 1.9, Chart.js 4.4, SockJS, Stomp.js.

---

## 3. Free Render Demo

The repository includes `render.yaml` and a Dockerfile for deploying a sample-data demo on Render's free web-service tier:

1. In Render, choose **New → Blueprint**, connect the private `giridharun72-tech/smartbus` GitHub repository, and deploy the `render.yaml` Blueprint.
2. The Blueprint selects the `demo` profile, which uses embedded in-memory H2, seeded sample buses/routes, HTTPS cookies, and simulated demo GPS movement. No database credentials are required.
3. Wait for the Render health check to pass, then open the HTTPS URL. Register a passenger account to try passenger features. The sample staff accounts have unpredictable passwords and are not intended for public sign-in.

**Demo limitations:** Render's free web service spins down after inactivity. Its filesystem is ephemeral, so the H2 database and registrations are lost when the service spins down, restarts, or redeploys. The free tier is for testing and hobby demos, not production. Do not enter real passenger, payment, driver, or operational GPS data. For a live production deployment, use persistent MySQL, securely provision staff accounts, and retain the `prod` profile.

Never commit database passwords, API keys, or generated secrets.

---

## 4. Pre-configured Demo Accounts

The development seeder creates these usernames, but it does not use public default passwords. Set the environment variable for each role before the first development startup if you need to sign in as a seeded account:

| Role | Username | Password environment variable | Primary Interface |
| :--- | :--- | :--- | :--- |
| Passenger | `user` | `SMARTBUS_DEMO_USER_PASSWORD` | `/user/dashboard` |
| Admin | `admin` | `SMARTBUS_DEMO_ADMIN_PASSWORD` | `/admin/dashboard` |
| BCU Officer | `bcu` | `SMARTBUS_DEMO_BCU_PASSWORD` | `/bcu/dashboard` |
| Drivers | `driver21a`, `driver43b`, `driver17c` | `SMARTBUS_DEMO_DRIVER_PASSWORD` | `/driver/dashboard` |

Use unique passwords of at least 16 characters. If a password is not configured, the seeder assigns an unpredictable password that is not printed or stored in the repository. Set the environment variables before the first seed; existing seeded accounts are not automatically reset. These demo accounts are for local development only and must never be enabled in a production deployment. You can also register a passenger account on `/register`.

---

## 5. How to Run in VS Code

### Option A: Direct VS Code Run (Zero-Setup with Embedded Database)
1. Open VS Code.
2. Select **File > Open Folder...** and choose:
3. Press **F5** (or click **Run > Start Debugging**).
- VS Code will automatically use the pre-configured `.vscode/launch.json` configuration with the `dev` profile.
4. Open your browser and navigate to: http://localhost:8080
### Option B: Using the Terminal or Batch Script
Open PowerShell in the project directory and run:
```powershell
.\run.bat
Or with Maven directly:
& "C:\Users\Giridharun S\.tools\apache-maven-3.9.10\bin\mvn.cmd" spring-boot:run
Option C: Running with Local MySQL
1.Start your local MySQL server on port 3306.
2.Configure credentials in src/main/resources/application-mysql.properties or set environment variables:
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
3.Launch with the MySQL profile:
.\run.bat --spring.profiles.active=mysql
6. Use an Android Phone as the Live GPS Device
The driver's device sends its own high-accuracy browser GPS readings to SmartBus. A phone hotspot only connects the devices; it does not automatically share the phone's GPS with a page open on the laptop.

Start SmartBus on the laptop and connect the laptop to the Android phone's hotspot.

On the laptop, run ipconfig and find the laptop's IPv4 address on the Wi-Fi adapter connected to the phone hotspot. Do not use the hotspot gateway address.

On the laptop, sign in to the BCU account and open Bus & Driver Setup to register the bus and its driver login.

On the driver's phone, open the SmartBus site using a trusted HTTPS address. Sign in at /driver/login using the Driver ID/password created by the BCU.

The driver dashboard opens a bus-location dialog with the assigned route map. Tap Start sharing location, allow the browser's location prompt, and leave the dashboard open while the bus is moving.

Open the BCU fleet map or a passenger tracking page to see the driver's live GPS updates.
If the phone cannot connect to the page, confirm both devices are on the same hotspot, use the laptop's hotspot-side IPv4 address, and allow port 8080 through Windows Defender Firewall on the private network. Opening the app over plain HTTP by IP is sufficient for the page connection but not for browser GPS. For local development only, Chrome can trust the exact local origin through chrome://flags/#unsafely-treat-insecure-origin-as-secure; enter http://<laptop-ipv4>:8080, relaunch Chrome, and turn the flag off when finished. This relaxes browser security and is not suitable for production; use trusted HTTPS for deployment. The default dev profile uses in-memory H2; use the MySQL profile when the GPS history must persist in MySQL.
Driver-created fleet and real device GPS
Configure SMARTBUS_AADHAAR_HMAC_KEY as a stable secret of at least 32 characters before starting SmartBus. For a local PowerShell run, generate it once, store it in your Windows user environment, and restart VS Code so its tasks inherit it (do not commit it):
$bytes = New-Object byte[] 32 [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$key = [Convert]::ToBase64String($bytes)
[Environment]::SetEnvironmentVariable("SMARTBUS_AADHAAR_HMAC_KEY", $key, "User")
$env:SMARTBUS_AADHAAR_HMAC_KEY =$key
.\run.bat
Keep the same key for future runs and database backups. The app retains only the Aadhaar last four digits and a keyed fingerprint used for duplicate checks; it never stores or displays the full Aadhaar number.
Configure a separate non-empty SMARTBUS_GPS_API_KEY before connecting physical GPS hardware. Simulation controls are restricted to BCU/Admin accounts.

Sign in as a BCU officer and open Bus & Driver Setup from the BCU dashboard.

Enter the bus number, route, TN vehicle registration, seat capacity, driver ID/password, and driver details. Bus IDs are assigned by the system. Duplicate bus numbers, TN registrations, driver IDs, phone numbers, or Aadhaar numbers are rejected.

The driver opens /driver/login and signs in with the new Driver ID/password. The dashboard displays the assigned route map and a Start sharing location prompt; the driver taps it and allows browser location access.

The driver's device sends real GPS fixes to the server; the existing passenger live tracking map and BCU fleet map receive the updates in real time. The driver can stop sharing from the same dashboard.

Browser geolocation is available only on HTTPS or localhost by default. A phone opening the app at http://<laptop-ip>:8080 will not be allowed to share location by normal browser security rules. For another device, serve the app through HTTPS with a certificate trusted by that device, or use the documented Chrome flag only for local testing; keep the phone and server reachable on the same network.

BCU driver, bus, and route operations
On Bus & Driver Setup, use Edit beside a registered bus to update its bus/registration details, route, seat capacity, and the assigned driver's name, phone, and licence. Driver login IDs remain unchanged.

When registering a bus on a new route, add every stop in order with its latitude and longitude. Existing routes reuse their configured stops.

For a road closure or breakdown, choose the route and service date, optionally select the affected bus and replacement bus, and enter the detour stop names and coordinates. Publishing the change broadcasts it to route subscribers; passenger live-tracking pages update their map immediately. The change applies only to the selected date. Use End update and restore route to cancel it early.

Driver ticket checks accept a ticket number or scanned SmartBus QR content. The system checks payment, confirmed booking, travel date, assigned bus (including an approved replacement bus), and prior use. Complete Boarding marks a valid ticket as used; passenger accounts cannot use the staff verification endpoint.

Login pages no longer provide one-click demo credentials. Development demo accounts are for local testing only; configure unique credentials and HTTPS before any public deployment, and set SMARTBUS_COOKIE_SECURE=true when serving over HTTPS.

Public registration always creates a passenger account, regardless of any role value sent by the browser. New passwords must be 12-72 characters and include uppercase and lowercase letters, a number, and a symbol.

7. Master 30-Step Demonstration Walkthrough
Follow this exact sequence to demonstrate all capabilities to evaluators:

Step 1: Open http://localhost:8080 → Landing page with live fleet metrics.

Step 2: Sign in as Passenger user using the configured SMARTBUS_DEMO_USER_PASSWORD.

Step 3: Navigate to Buses Near Me → Verify nearby stops and walking estimates.

Step 4: Navigate to Search Buses → Search Gandhipuram to Ukkadam after 11:10 AM.

Step 5: Observe that buses are returned in chronological order (11:10, 11:15, 11:30, 11:45, 12:00...).

Step 6: Click Track Bus on Bus 21A → Live Leaflet map opens.

Step 7: Verify live telemetry: Speed, next stop, distance, ETA, and LIVE GPS badge.

Step 8: Click Notify Me → Configure 1 km proximity arrival alert.

Step 9: Click Book This Bus → Interactive visual seat grid loads.

Step 10: Select seat B2 → Fare calculates automatically (Rs. 25.00).

Step 11: Click Proceed to Pay → Confirm sandbox payment.

Step 12: Ticket is issued with unique number TKT-2026-xxxxxx and QR code.

Step 13: Click Download Official PDF → Open the professional PDF ticket.

Step 14: Open an Incognito window or logout → Login as Admin using SMARTBUS_DEMO_ADMIN_PASSWORD.

Step 15: View the new booking in Bookings and inspect the Chart.js revenue charts.

Step 16: In Admin Buses, click Cancel Bus on Bus 21A.

Step 17: Check User notification center → Cancellation alert was delivered instantly!

Step 18: Login as BCU Control using SMARTBUS_DEMO_BCU_PASSWORD → Open Fullscreen Fleet Map.

Step 19: Navigate to Simulation Panel.

Step 20: Click Advance Bus 21A to Next Stop → Watch telemetry update on live map.

Step 21: Click Cut GPS Signal → Simulates sudden GPS loss.

Step 22: BCU receives a critical GPS_SIGNAL_LOST alert with last known location.

Step 23: In BCU Signal Monitor, click Authorize Manual for Bus 21A.

Step 24: Open another tab, login as Driver driver21a using SMARTBUS_DEMO_DRIVER_PASSWORD.

Step 25: Open Manual Location Update → Permission is now unlocked!

Step 26: Select stop Town Hall and click Push Manual Location.

Step 27: Open the Passenger Tracking map → Source now says MANUAL LOCATION UPDATE!

Step 28: In BCU, navigate to Passenger Verification → Enter PB2026001001.

Step 29: Click Download Travel History PDF → Generates comprehensive history report.

Step 30: On the assigned Driver console, open Conductor Ticket Scanner → scan a QR code or enter its ticket number → confirm the travel, payment, and bus details → select Complete Boarding to mark it USED.
