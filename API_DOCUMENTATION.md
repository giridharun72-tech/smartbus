# SmartBus REST API & WebSocket STOMP Specification

## 1. Authentication Endpoints

### Register User & Auto-Generate Passenger ID
- **Endpoint:** `POST /api/auth/register`
- **Request Body:**
```json
{
  "fullName": "Kavin B",
  "username": "kavviinnnnn",
  "email": "kavin@example.com",
  "password": "password123",
  "phone": "8610595019",
  "address": "Namakkal",
  "emergencyContact": "9597414468",
  "role": "USER"
}
```
- **Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration successful! Passenger ID: PB2026001847",
  "username": "giridharun",
  "role": "ROLE_USER",
  "passengerId": "PB2026001847",
  "redirectUrl": "/user/dashboard"
}
```

---

## 2. Bus Search & Nearby Discovery

### Search Buses Chronologically (>= Time)
- **Endpoint:** `GET /api/buses/search?from=Gandhipuram&to=Ukkadam&date=2026-09-01&time=11:10`
- **Behavior:** Returns all buses departing at or after 11:10 AM, sorted chronologically ascending.
- **Sample Item in Response Array:**
```json
{
  "busId": 1,
  "busNumber": "21A",
  "registrationNumber": "TN-37-N-8821",
  "routeName": "Gandhipuram - Ukkadam Express (Route 5)",
  "source": "Gandhipuram",
  "destination": "Ukkadam",
  "departureTime": "11:10:00",
  "arrivalTime": "11:35:00",
  "status": "RUNNING",
  "availableSeats": 39,
  "fare": 25.0,
  "distanceDisplay": "1.8 km",
  "etaMinutes": 7,
  "nextStopName": "Central Market",
  "busType": "STANDARD"
}
```

### Buses Near Me (Geolocation)
- **Endpoint:** `GET /api/buses/nearby?lat=11.0168&lon=76.9558&radiusKm=5.0`
- **Response:** Array of nearest stops with straight-line distance, walking estimate, and passing bus lines.

---

## 3. GPS Ingestion & ESP32 Integration

### Real Hardware Ingestion
- **Endpoint:** `POST /api/gps/location`
- **Headers:** `X-API-KEY: SMARTBUS_GPS_KEY_2026`
- **Payload:**
```json
{
  "busNumber": "21A",
  "deviceId": "GPS-DEV-21A",
  "latitude": 11.0090,
  "longitude": 76.9650,
  "speed": 32.5,
  "apiKey": "SMARTBUS_GPS_KEY_2026"
}
```

### Demonstration Simulation
- `POST /api/gps/simulate/{busId}/step` &rarr; Advances bus by one waypoint.
- `POST /api/gps/simulate/{busId}/signal-loss` &rarr; Cuts off GPS to test timeout detection.
- `POST /api/gps/simulate/{busId}/resume` &rarr; Resumes transmission.

---

## 4. Admin Fleet Management

### Add a Bus with Device Location
- **Endpoint:** `POST /api/admin/buses`
- **Access:** Authenticated Admin session (the application uses Spring Security session cookies).
- **Success:** `201 Created`
- **Duplicate bus number:** `409 Conflict`
- **Invalid or missing fields / zero coordinates:** `400 Bad Request`
- **Payload:**
```json
{
  "busNumber": "Bus 25B",
  "route": "Gandhipuram - Ukkadam Express",
  "driverName": "Ramesh K",
  "totalSeats": 40,
  "currentLatitude": 10.959842,
  "currentLongitude": 78.080519,
  "status": "RUNNING"
}
```
- Coordinates are captured in the Admin fleet form using browser geolocation, which requires HTTPS or localhost.
- The newly created bus is saved with its initial GPS timestamp and broadcast to `/topic/fleet-updates`.

### Stream Live Location from an Android Phone
- Open `/admin/phone-gps` in Chrome on the phone, select the bus, and start sharing.
- **Endpoint:** `POST /api/admin/buses/{id}/location`
- **Access:** Authenticated Admin session on the phone.
- **Payload:** `{"latitude": 11.0168, "longitude": 76.9558, "speed": 12.5}` (speed in km/h)
- The latest fix is saved as a bus location and broadcast to `/topic/bus-location` and `/topic/bus-location/{busId}`.
- On a private hotspot network using plain HTTP, Android Chrome must be configured to treat the exact laptop origin as secure for browser geolocation; the phone GPS page displays the origin and setup instructions.

---

## 5. Seat Reservation & Ticketing

### Create Booking
- **Endpoint:** `POST /api/bookings`
- **Payload:**
```json
{
  "busId": 1,
  "routeId": 1,
  "fromStop": "Gandhipuram",
  "toStop": "Ukkadam",
  "travelDate": "2026-09-01",
  "seatNumbers": ["A12"]
}
```
- **Response:**
```json
{
  "success": true,
  "ticketNumber": "TKT-2026-000845",
  "passengerId": "PB2026001001",
  "fare": 25.0,
  "status": "VALID"
}
```

### Download Ticket PDF
- **Endpoint:** `GET /api/tickets/{ticketNumber}/pdf`
- **Response:** Binary `application/pdf` with header `Content-Disposition: attachment; filename="Ticket_TKT-2026-000845.pdf"`.

### Verify Ticket
- **Endpoint:** `POST /api/tickets/verify`
- **Payload:** `{"ticketNumber": "TKT-2026-000845", "markAsUsed": true}`

---

## 6. Central Bus Control Unit (BCU)

- `GET /api/bcu/fleet` &rarr; Real-time snapshot of all buses, telemetry, status, and location source.
- `POST /api/bcu/manual-location/enable?busId=1` &rarr; Grants permission for manual coordinate submission.
- `GET /api/bcu/passengers/{passengerId}` &rarr; Returns verified passenger data and active ticket.
- `GET /api/bcu/passengers/{passengerId}/history-pdf` &rarr; Generates Passenger Travel History PDF.

---

## 7. Real-Time WebSocket STOMP Channels

- **Connection URL:** `/ws` (with SockJS)
- **Topics:**
  - `/topic/bus-location`: Broadcasts live telemetry on coordinate updates.
  - `/topic/bus-location/{busId}`: Updates targeted to users viewing a specific bus.
  - `/topic/fleet-updates`: Newly registered buses with their initial GPS location.
  - `/topic/bcu-alerts`: Pushes high-priority alerts (`GPS_SIGNAL_LOST`, `BREAKDOWN`, `EMERGENCY`).
  - `/topic/notifications`: Broadcasts service announcements and cancellations.

---

## 8. Nearby Trip Assistant & Passenger Support Chat

### Local LLM and Nearby Trip Assistant
- **Name:** Neuronex, SmartBus's AI assistant.
- **Endpoint:** `POST /assistant/nearby/reply`
- **Access:** Public. Explicit bus-trip searches use SmartBus route, schedule, and service-status data. General questions use the local Ollama model. Current or time-sensitive questions (including current officeholders, elections, weather, live news, and questions mentioning a year) are grounded with Ollama Web Search and include source links.
- **CSRF:** Exempted from CSRF because this endpoint is public and read-only; it does not change application state.
- **Local model and web search:** Install Ollama locally, then run `ollama pull qwen2.5:3b`. The model uses `http://127.0.0.1:11434` and remains local. Current-information search uses Ollama's HTTPS `https://ollama.com/api/web_search` API. Create an Ollama API key at https://ollama.com/settings/keys and set it in the `OLLAMA_API_KEY` environment variable; do not commit the key. Only the user's question is sent to web search, not their account, role, location, or SmartBus records.
- **Privacy notice:** The chat widget tells users that current-information searches send their question to Ollama's web-search service. Users should not include personal or sensitive information in a question.
- **Windows key setup:** In PowerShell, set `$env:OLLAMA_API_KEY = Read-Host 'Ollama API key'` before launching SmartBus from that same environment. Restart SmartBus after changing the variable. The VS Code launch and run-task configurations pass through the environment variable without storing its value in the workspace.
- **Role and data boundaries:** The server determines the role from the authenticated session. Role-specific FAQs are handled by the application; the LLM receives only the question and a coarse role label, with no database context, private records, or tool access. It cannot perform app actions. Unauthenticated users receive guest guidance.
- **Prompt-injection handling:** Known instruction-override, prompt-extraction, role-impersonation, and sensitive-data extraction requests are refused before contacting Ollama. The local model is also instructed to treat user text as untrusted. Because no secrets or private records are sent to the model, prompt injection cannot retrieve them through this assistant.
- **Availability:** If web search is not configured or unavailable, time-sensitive questions return an explicit service-unavailable response rather than an unverified answer. General questions, built-in role FAQs, and SmartBus trip search remain available.
- **Payload:**
```json
{
  "message": "Fastest express bus to Ukkadam",
  "latitude": 11.0168,
  "longitude": 76.9558
}
```
- **Response:** A short text reply, up to three matching trip suggestions with stop distance, walk estimate, departure time, service status, and tracking/booking eligibility, plus source title/URL entries for live web-grounded answers.
- The site-wide chat widget also answers common greetings and predefined questions about bookings, fares, bus tracking, location permissions, account access, schedules, and contacting BCU/Admin. These FAQ answers do not require Ollama or a live trip lookup.
- FAQ replies are selected server-side from the authenticated session role (`USER`, `DRIVER`, `BCU`, or `ADMIN`); unauthenticated visitors receive guest guidance. The browser cannot choose or impersonate a role. Staff replies are instructional only and do not read or modify private operational, passenger, or financial records. Destination and route questions continue to search only public nearby trip data.

### Session security
- State-changing browser requests use CSRF protection. The shared browser script sends the `X-XSRF-TOKEN` header from the CSRF cookie for same-origin requests; API clients must supply a valid CSRF token unless the endpoint uses a separate device key.
- `/api/gps/location` is exempt from CSRF because it requires the configured `SMARTBUS_GPS_API_KEY` in the `X-API-KEY` header or request body. Keep that key private and configure it before accepting device updates.
- Render runs with the `prod` profile and `SMARTBUS_COOKIE_SECURE=true`; production deployments must use HTTPS, a persistent database, and separately provisioned administrator accounts. The development seeder is disabled outside the `dev` profile.

### Passenger Support Conversation
- **Access:** Authenticated `ROLE_USER`; one private conversation per passenger.
- **`GET /support-chat/conversation`** returns the passenger's messages and marks staff replies as read.
- **`POST /support-chat/conversation/messages`** sends a passenger message.
- **Payload:** `{"content": "Is the Ukkadam road diversion active today?"}`
- **CSRF:** Include the authenticated session cookie and Spring Security CSRF token (`X-XSRF-TOKEN`) on state-changing requests.

### BCU/Admin Support Inbox
- **Pages:** `/bcu/assistant-chat` and `/admin/assistant-chat`
- **Access:** BCU and Admin staff roles only.
- **`GET /support-chat/staff/conversations`** lists the 100 most recently updated conversations, passenger identity, latest message, and unread count.
- **`GET /support-chat/staff/conversations/{conversationId}/messages`** reads a conversation and marks passenger messages as read.
- **`POST /support-chat/staff/conversations/{conversationId}/messages`** sends a staff reply.
- **Privacy:** Support conversations use authenticated REST requests and are never published to public WebSocket topics.