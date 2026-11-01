# SmartBus Relational Database Schema & Data Dictionary

The SmartBus platform uses a relational schema with relational integrity, foreign key cascades, unique constraints, and indexes.

---

## 1. Entity-Relationship Diagram Overview

```
 [User] 1 <--- 1 [PassengerProfile]
   |    1 <--- 1 [Driver]
   |    1 <--- * [Booking] 1 <--- 1 [Ticket]
   |             |         1 <--- 1 [Payment]
   |             |         1 <--- * [Seat]
   |             |
   |             v
 [Bus] * <--- 1 [Route] 1 <--- * [RouteStop] * ---> 1 [BusStop]
   |
   |---> 1 <--- * [Schedule]
   |---> 1 <--- * [BusLocation]
   |---> 1 <--- * [ManualLocationUpdate]
   |---> 1 <--- * [Incident]
   |---> 1 <--- * [NotificationSubscription]
```

---

## 2. Table Specifications

### 1. `users`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | User ID |
| `username` | VARCHAR(60) | UNIQUE, NOT NULL | Login handle |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | User email |
| `password` | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| `role` | VARCHAR(20) | NOT NULL | ROLE_USER, ROLE_ADMIN, ROLE_BCU, ROLE_DRIVER |
| `enabled` | BOOLEAN | DEFAULT TRUE | Account active flag |
| `created_at` | DATETIME | NOT NULL | Registration timestamp |
| `updated_at` | DATETIME | NOT NULL | Last profile update |

### 2. `passenger_profiles`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Profile ID |
| `user_id` | BIGINT | UNIQUE, FOREIGN KEY (`users.id`) | Owning user |
| `passenger_id` | VARCHAR(30) | UNIQUE, NOT NULL | Universal Passenger ID (e.g. `PB2026001001`) |
| `full_name` | VARCHAR(100) | NOT NULL | Passenger full name |
| `phone` | VARCHAR(20) | NULLABLE | Primary mobile number |
| `address` | VARCHAR(255) | NULLABLE | Residential address |
| `emergency_contact` | VARCHAR(50) | NULLABLE | Emergency contact phone |

### 3. `drivers`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Driver ID |
| `user_id` | BIGINT | UNIQUE, FOREIGN KEY (`users.id`) | Driver user account |
| `driver_code` | VARCHAR(30) | UNIQUE, NOT NULL | Driver badge code (e.g. `DRV-101`) |
| `full_name` | VARCHAR(100) | NOT NULL | Driver legal name |
| `phone` | VARCHAR(20) | NULLABLE | Contact mobile |
| `license_number` | VARCHAR(50) | NOT NULL | Heavy Vehicle Driving License # |
| `status` | VARCHAR(20) | DEFAULT 'ACTIVE' | ACTIVE, ON_TRIP, OFF_DUTY |

### 4. `bus_stops`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Stop ID |
| `name` | VARCHAR(100) | NOT NULL | Bus stop title (e.g. `Central Market`) |
| `latitude` | DOUBLE | NOT NULL | GPS Latitude |
| `longitude` | DOUBLE | NOT NULL | GPS Longitude |
| `landmark` | VARCHAR(150) | NULLABLE | Visual reference point |

### 5. `routes` & `route_stops`
- `routes`: `id`, `route_number` (e.g. `Route 5`), `name`, `source`, `destination`, `total_distance_km`, `estimated_duration_min`.
- `route_stops`: `id`, `route_id`, `stop_id`, `stop_order`, `distance_from_start_km`, `scheduled_minutes_from_start`, `fare_from_start`.

### 6. `buses`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Bus primary ID |
| `bus_number` | VARCHAR(30) | UNIQUE, NOT NULL | Fleet number (e.g. `21A`) |
| `registration_number` | VARCHAR(30) | UNIQUE, NOT NULL | Vehicle RTO number (e.g. `TN-37-N-8821`) |
| `bus_type` | VARCHAR(30) | DEFAULT 'STANDARD' | STANDARD, AC, EXPRESS |
| `capacity` | INT | DEFAULT 40 | Total seating capacity |
| `status` | VARCHAR(30) | NOT NULL | RUNNING, DELAYED, CANCELLED, GPS_SIGNAL_LOST, etc. |
| `manual_location_allowed` | BOOLEAN | DEFAULT FALSE | Authorized by BCU for manual location entry |
| `last_gps_update` | DATETIME | NULLABLE | Timestamp of last received GPS coordinate |
| `driver_id` | BIGINT | FOREIGN KEY (`drivers.id`) | Assigned driver |
| `route_id` | BIGINT | FOREIGN KEY (`routes.id`) | Assigned route |
| `gps_device_id` | VARCHAR(50) | NULLABLE | Telemetry hardware device identifier |
| `current_latitude` | DOUBLE | NULLABLE | Last known latitude |
| `current_longitude` | DOUBLE | NULLABLE | Last known longitude |
| `current_speed` | DOUBLE | DEFAULT 0.0 | Telemetry speed in km/h |
| `next_stop_name` | VARCHAR(100) | NULLABLE | Approaching stop title |
| `location_source` | VARCHAR(20) | DEFAULT 'GPS' | GPS, MANUAL, LAST_KNOWN |

### 7. `bookings`, `seats`, `payments`, `tickets`
- `bookings`: `id`, `booking_reference`, `user_id`, `bus_id`, `route_id`, `from_stop`, `to_stop`, `travel_date`, `departure_time`, `seat_numbers`, `number_of_seats`, `total_fare`, `status`.
- `seats`: `id`, `bus_id`, `seat_number` (`A1` to `D10`), `travel_date`, `booking_id`, `status` (`AVAILABLE, BOOKED, LOCKED`).
  - Unique Constraint: `(bus_id, seat_number, travel_date)`.
- `payments`: `id`, `payment_reference`, `booking_id`, `amount`, `transaction_id`, `status`, `payment_method`, `timestamp`.
- `tickets`: `id`, `ticket_number` (e.g. `TKT-2026-000845`), `booking_id`, `passenger_name`, `passenger_id`, `bus_number`, `route_name`, `from_stop`, `to_stop`, `travel_date`, `departure_time`, `seat_number`, `fare`, `qr_code_base64`, `verification_status` (`VALID, USED, CANCELLED`), `verified_at`, `verified_by`.

### 8. `notifications` & `notification_subscriptions`
- Stores user subscriptions for bus cancellations, delays, and smart proximity thresholds (`500m`, `1km`, `2km`, `5km`).

### 9. `incidents` & `manual_location_updates`
- `incidents`: `id`, `incident_reference`, `bus_id`, `driver_id`, `type` (`GPS_SIGNAL_LOST, BREAKDOWN, EMERGENCY, ROUTE_DEVIATION, DELAY`), `description`, `priority`, `status` (`OPEN, IN_PROGRESS, RESOLVED`), `reported_at`, `resolved_at`.
- `manual_location_updates`: `id`, `bus_id`, `driver_id`, `latitude`, `longitude`, `landmark`, `reason`, `bcu_authorized`, `timestamp`.