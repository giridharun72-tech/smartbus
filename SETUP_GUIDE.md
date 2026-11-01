# SmartBus Setup & Configuration Guide

## 1. Prerequisites
- **Java 25 or newer** (Temurin, OpenJDK, or Oracle JDK)
- VS Code (with "Extension Pack for Java" recommended)

---

## 2. Running Immediately (Zero-Setup Development Mode)

By default, SmartBus starts in `dev` mode using an embedded H2 database running in full **MySQL-compatibility mode**. All seed data (10+ buses, 4 routes, 14 stops, sample schedules, bookings, and demo accounts) is populated automatically.

### Running from VS Code:
1. Launch VS Code.
2. Open the project folder:
   ```
   C:\Users\Giridharun S\.gemini\antigravity\scratch\smartbus
   ```
3. Press **F5**.
4. Visit `http://localhost:8080` in your web browser.

### Running via Terminal:
```powershell
.\run.bat
```
Or with Maven:
```powershell
& "C:\Users\Giridharun S\.tools\apache-maven-3.9.10\bin\mvn.cmd" spring-boot:run
```

---

## 3. Switching to Production MySQL

When you have MySQL installed or running:
1. Ensure MySQL is running on port `3306`.
2. Open `src/main/resources/application.properties` and change:
   ```properties
   spring.profiles.active=mysql
   ```
3. Update your credentials in `src/main/resources/application-mysql.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```
4. Start the application. Hibernate will automatically create the database `smartbus_db` (using `createDatabaseIfNotExist=true`) and initialize all tables and seed records!

---

## 4. Hardware ESP32 & GPS Integration

To send live coordinates from an ESP32 microcontroller with a NEO-6M or SIM800 GPS module:

### Sample HTTP POST Payload:
```http
POST /api/gps/location HTTP/1.1
Host: your-server-ip:8080
Content-Type: application/json
X-API-KEY: SMARTBUS_GPS_KEY_2026

{
  "busNumber": "21A",
  "deviceId": "GPS-DEV-21A",
  "latitude": 11.009055,
  "longitude": 76.965042,
  "speed": 34.2,
  "timestamp": "2026-09-01T11:30:25"
}
```
The server authenticates the device key, checks for route waypoints, computes ETA, updates the database, and immediately broadcasts the updated coordinates over WebSockets to all passengers and BCU monitors.