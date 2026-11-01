package com.smartbus.config;

import com.smartbus.entity.*;
import com.smartbus.repository.*;
import com.smartbus.service.TicketService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    @Value("${SMARTBUS_DEMO_USER_PASSWORD:}")
    private String demoUserPassword;

    @Value("${SMARTBUS_DEMO_ADMIN_PASSWORD:}")
    private String demoAdminPassword;

    @Value("${SMARTBUS_DEMO_BCU_PASSWORD:}")
    private String demoBcuPassword;

    @Value("${SMARTBUS_DEMO_DRIVER_PASSWORD:}")
    private String demoDriverPassword;

    private final UserRepository userRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final DriverRepository driverRepository;
    private final BusStopRepository busStopRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final BusRepository busRepository;
    private final ScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final NotificationRepository notificationRepository;
    private final IncidentRepository incidentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TicketService ticketService;

    public DataSeeder(UserRepository userRepository,
                      PassengerProfileRepository passengerProfileRepository,
                      DriverRepository driverRepository,
                      BusStopRepository busStopRepository,
                      RouteRepository routeRepository,
                      RouteStopRepository routeStopRepository,
                      BusRepository busRepository,
                      ScheduleRepository scheduleRepository,
                      BookingRepository bookingRepository,
                      SeatRepository seatRepository,
                      PaymentRepository paymentRepository,
                      TicketRepository ticketRepository,
                      NotificationRepository notificationRepository,
                      IncidentRepository incidentRepository,
                      PasswordEncoder passwordEncoder,
                      TicketService ticketService) {
        this.userRepository = userRepository;
        this.passengerProfileRepository = passengerProfileRepository;
        this.driverRepository = driverRepository;
        this.busStopRepository = busStopRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.busRepository = busRepository;
        this.scheduleRepository = scheduleRepository;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.notificationRepository = notificationRepository;
        this.incidentRepository = incidentRepository;
        this.passwordEncoder = passwordEncoder;
        this.ticketService = ticketService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return; // already seeded
        }

        // 1. SEED USERS & ROLES
        User user1 = userRepository.save(new User("user", "user@smartbus.com", passwordEncoder.encode(demoPassword(demoUserPassword)), Role.ROLE_USER));
        passengerProfileRepository.save(new PassengerProfile(user1, "PB2026001001", "Giridharun S", "9876543210", "124 Gandhipuram Cross, Coimbatore", "9876543211"));

        User adminUser = userRepository.save(new User("admin", "admin@smartbus.com", passwordEncoder.encode(demoPassword(demoAdminPassword)), Role.ROLE_ADMIN));
        User bcuUser = userRepository.save(new User("bcu", "bcu@smartbus.com", passwordEncoder.encode(demoPassword(demoBcuPassword)), Role.ROLE_BCU));

        String driverPassword = demoPassword(demoDriverPassword);
        User driverUser1 = userRepository.save(new User("driver21a", "driver21a@smartbus.com", passwordEncoder.encode(driverPassword), Role.ROLE_DRIVER));
        Driver driver1 = driverRepository.save(new Driver(driverUser1, "DRV-101", "Murugan K", "+91 94431 22334", "TN-37-2018-0091"));

        User driverUser2 = userRepository.save(new User("driver43b", "driver43b@smartbus.com", passwordEncoder.encode(driverPassword), Role.ROLE_DRIVER));
        Driver driver2 = driverRepository.save(new Driver(driverUser2, "DRV-102", "Senthil Nathan", "+91 94432 33445", "TN-37-2019-0142"));

        User driverUser3 = userRepository.save(new User("driver17c", "driver17c@smartbus.com", passwordEncoder.encode(driverPassword), Role.ROLE_DRIVER));
        Driver driver3 = driverRepository.save(new Driver(driverUser3, "DRV-103", "Ravi Kumar", "+91 94433 44556", "TN-37-2020-0205"));

        // 2. SEED BUS STOPS
        BusStop s1 = busStopRepository.save(new BusStop("Gandhipuram Bus Stand", 11.0168, 76.9558, "Central Bus Stand Terminal"));
        BusStop s2 = busStopRepository.save(new BusStop("Cross Cut Road", 11.0135, 76.9602, "Near Ramraj Cotton"));
        BusStop s3 = busStopRepository.save(new BusStop("Central Market", 11.0090, 76.9650, "Flower Market Entrance"));
        BusStop s4 = busStopRepository.save(new BusStop("Railway Station", 11.0030, 76.9675, "Coimbatore Junction East Gate"));
        BusStop s5 = busStopRepository.save(new BusStop("Town Hall", 10.9980, 76.9650, "Victoria Town Hall Tower"));
        BusStop s6 = busStopRepository.save(new BusStop("Big Bazaar Street", 10.9920, 76.9620, "Commercial Square"));
        BusStop s7 = busStopRepository.save(new BusStop("Ukkadam Bus Terminal", 10.9870, 76.9580, "South Terminal & Lake View"));

        BusStop s8 = busStopRepository.save(new BusStop("Coimbatore International Airport", 11.0298, 77.0434, "Terminal 1 Departure Gate"));
        BusStop s9 = busStopRepository.save(new BusStop("Hope College", 11.0232, 77.0142, "Avinashi Road Junction"));
        BusStop s10 = busStopRepository.save(new BusStop("Peelamedu", 11.0205, 76.9985, "PSG Tech Entrance"));
        BusStop s11 = busStopRepository.save(new BusStop("Lakshmi Mills", 11.0178, 76.9805, "Avinashi Road Signal"));

        BusStop s12 = busStopRepository.save(new BusStop("CHIL SEZ Tech Park", 11.0820, 76.9940, "Saravanampatti IT Park"));
        BusStop s13 = busStopRepository.save(new BusStop("Saravanampatti Checkpost", 11.0590, 76.9890, "Sathy Road Junction"));
        BusStop s14 = busStopRepository.save(new BusStop("Ganapathy", 11.0370, 76.9740, "Bus Stop Signal"));

        // 3. SEED ROUTES
        Route r5 = routeRepository.save(new Route("Route 5", "Gandhipuram - Ukkadam Express", "Gandhipuram", "Ukkadam", 7.5, 25));
        routeStopRepository.save(new RouteStop(r5, s1, 1, 0.0, 0, 0.0));
        routeStopRepository.save(new RouteStop(r5, s2, 2, 1.2, 4, 10.0));
        routeStopRepository.save(new RouteStop(r5, s3, 3, 2.8, 9, 15.0));
        routeStopRepository.save(new RouteStop(r5, s4, 4, 4.3, 14, 20.0));
        routeStopRepository.save(new RouteStop(r5, s5, 5, 5.5, 18, 22.0));
        routeStopRepository.save(new RouteStop(r5, s6, 6, 6.4, 21, 25.0));
        routeStopRepository.save(new RouteStop(r5, s7, 7, 7.5, 25, 25.0));

        Route r12 = routeRepository.save(new Route("Route 12", "Airport - Gandhipuram City Connect", "Airport", "Gandhipuram", 11.2, 35));
        routeStopRepository.save(new RouteStop(r12, s8, 1, 0.0, 0, 0.0));
        routeStopRepository.save(new RouteStop(r12, s9, 2, 4.0, 12, 15.0));
        routeStopRepository.save(new RouteStop(r12, s10, 3, 6.5, 19, 20.0));
        routeStopRepository.save(new RouteStop(r12, s11, 4, 8.8, 26, 25.0));
        routeStopRepository.save(new RouteStop(r12, s1, 5, 11.2, 35, 30.0));

        Route r18 = routeRepository.save(new Route("Route 18", "Tech Park - Central Terminal", "Tech Park", "Gandhipuram", 9.8, 30));
        routeStopRepository.save(new RouteStop(r18, s12, 1, 0.0, 0, 0.0));
        routeStopRepository.save(new RouteStop(r18, s13, 2, 3.2, 10, 15.0));
        routeStopRepository.save(new RouteStop(r18, s14, 3, 6.5, 20, 20.0));
        routeStopRepository.save(new RouteStop(r18, s1, 4, 9.8, 30, 25.0));

        Route r24 = routeRepository.save(new Route("Route 24", "Railway Station - Hope College Metro", "Railway Station", "Hope College", 8.2, 28));
        routeStopRepository.save(new RouteStop(r24, s4, 1, 0.0, 0, 0.0));
        routeStopRepository.save(new RouteStop(r24, s11, 2, 3.5, 11, 15.0));
        routeStopRepository.save(new RouteStop(r24, s10, 3, 5.8, 19, 20.0));
        routeStopRepository.save(new RouteStop(r24, s9, 4, 8.2, 28, 25.0));

        // 4. SEED 10+ BUSES
        Bus b21A = new Bus("21A", "TN-37-N-8821", "STANDARD", 40, r5, driver1, "GPS-DEV-21A");
        b21A.setCurrentLatitude(11.0090);
        b21A.setCurrentLongitude(76.9650);
        b21A.setCurrentSpeed(32.0);
        b21A.setNextStopName("Central Market");
        b21A.setDistanceToNextStopKm(1.8);
        b21A.setEtaMinutes(7);
        busRepository.save(b21A);

        Bus b43B = new Bus("43B", "TN-37-N-8843", "EXPRESS", 40, r5, driver2, "GPS-DEV-43B");
        b43B.setCurrentLatitude(11.0168);
        b43B.setCurrentLongitude(76.9558);
        b43B.setCurrentSpeed(28.0);
        b43B.setNextStopName("Cross Cut Road");
        b43B.setDistanceToNextStopKm(3.2);
        b43B.setEtaMinutes(12);
        busRepository.save(b43B);

        Bus b17C = new Bus("17C", "TN-37-N-8817", "AC", 36, r12, driver3, "GPS-DEV-17C");
        b17C.setCurrentLatitude(11.0205);
        b17C.setCurrentLongitude(76.9985);
        b17C.setCurrentSpeed(35.0);
        b17C.setNextStopName("Peelamedu");
        b17C.setDistanceToNextStopKm(2.4);
        b17C.setEtaMinutes(8);
        busRepository.save(b17C);

        Bus b11D = new Bus("11D", "TN-37-N-8811", "STANDARD", 40, r12, null, "GPS-DEV-11D");
        b11D.setCurrentLatitude(11.0298);
        b11D.setCurrentLongitude(77.0434);
        b11D.setCurrentSpeed(0.0);
        b11D.setNextStopName("Hope College");
        b11D.setDistanceToNextStopKm(4.0);
        b11D.setEtaMinutes(15);
        busRepository.save(b11D);

        Bus b5E = new Bus("5E", "TN-37-N-8805", "STANDARD", 40, r18, null, "GPS-DEV-5E");
        b5E.setCurrentLatitude(11.0590);
        b5E.setCurrentLongitude(76.9890);
        b5E.setCurrentSpeed(30.0);
        b5E.setNextStopName("Ganapathy");
        b5E.setDistanceToNextStopKm(3.3);
        b5E.setEtaMinutes(10);
        busRepository.save(b5E);

        Bus b7A = new Bus("7A", "TN-37-N-8807", "EXPRESS", 40, r18, null, "GPS-DEV-7A");
        b7A.setCurrentLatitude(11.0820);
        b7A.setCurrentLongitude(76.9940);
        b7A.setCurrentSpeed(25.0);
        b7A.setNextStopName("Saravanampatti Checkpost");
        b7A.setDistanceToNextStopKm(3.2);
        b7A.setEtaMinutes(11);
        busRepository.save(b7A);

        Bus b9B = new Bus("9B", "TN-37-N-8809", "STANDARD", 40, r24, null, "GPS-DEV-9B");
        b9B.setCurrentLatitude(11.0030);
        b9B.setCurrentLongitude(76.9675);
        b9B.setCurrentSpeed(22.0);
        b9B.setNextStopName("Lakshmi Mills");
        b9B.setDistanceToNextStopKm(3.5);
        b9B.setEtaMinutes(11);
        busRepository.save(b9B);

        Bus b14F = new Bus("14F", "TN-37-N-8814", "AC", 36, r24, null, "GPS-DEV-14F");
        b14F.setCurrentLatitude(11.0178);
        b14F.setCurrentLongitude(76.9805);
        b14F.setCurrentSpeed(30.0);
        b14F.setNextStopName("Peelamedu");
        b14F.setDistanceToNextStopKm(2.3);
        b14F.setEtaMinutes(7);
        busRepository.save(b14F);

        Bus b22C = new Bus("22C", "TN-37-N-8822", "STANDARD", 40, r5, null, "GPS-DEV-22C");
        b22C.setCurrentLatitude(10.9980);
        b22C.setCurrentLongitude(76.9650);
        b22C.setCurrentSpeed(31.0);
        b22C.setNextStopName("Big Bazaar Street");
        b22C.setDistanceToNextStopKm(1.2);
        b22C.setEtaMinutes(4);
        busRepository.save(b22C);

        Bus b33A = new Bus("33A", "TN-37-N-8833", "EXPRESS", 40, r12, null, "GPS-DEV-33A");
        b33A.setCurrentLatitude(11.0232);
        b33A.setCurrentLongitude(77.0142);
        b33A.setCurrentSpeed(33.0);
        b33A.setNextStopName("Peelamedu");
        b33A.setDistanceToNextStopKm(2.5);
        b33A.setEtaMinutes(8);
        busRepository.save(b33A);

        // 5. SEED TIMETABLE SCHEDULES (Includes >= 11:10 AM staggered departures for exact demonstration)
        scheduleRepository.save(new Schedule(b21A, r5, LocalTime.of(11, 10), LocalTime.of(11, 35), "DAILY"));
        scheduleRepository.save(new Schedule(b43B, r5, LocalTime.of(11, 15), LocalTime.of(11, 40), "DAILY"));
        scheduleRepository.save(new Schedule(b22C, r5, LocalTime.of(11, 30), LocalTime.of(11, 55), "DAILY"));
        scheduleRepository.save(new Schedule(b21A, r5, LocalTime.of(11, 45), LocalTime.of(12, 10), "DAILY"));
        scheduleRepository.save(new Schedule(b43B, r5, LocalTime.of(12, 0), LocalTime.of(12, 25), "DAILY"));
        scheduleRepository.save(new Schedule(b22C, r5, LocalTime.of(12, 15), LocalTime.of(12, 40), "DAILY"));

        scheduleRepository.save(new Schedule(b17C, r12, LocalTime.of(11, 12), LocalTime.of(11, 47), "DAILY"));
        scheduleRepository.save(new Schedule(b11D, r12, LocalTime.of(11, 25), LocalTime.of(12, 0), "DAILY"));
        scheduleRepository.save(new Schedule(b33A, r12, LocalTime.of(11, 50), LocalTime.of(12, 25), "DAILY"));

        scheduleRepository.save(new Schedule(b5E, r18, LocalTime.of(11, 10), LocalTime.of(11, 40), "DAILY"));
        scheduleRepository.save(new Schedule(b7A, r18, LocalTime.of(11, 35), LocalTime.of(12, 5), "DAILY"));

        scheduleRepository.save(new Schedule(b9B, r24, LocalTime.of(11, 20), LocalTime.of(11, 48), "DAILY"));
        scheduleRepository.save(new Schedule(b14F, r24, LocalTime.of(11, 40), LocalTime.of(12, 8), "DAILY"));

        // 6. SEED INITIAL BOOKING & CONFIRMED TICKET
        LocalDate today = LocalDate.now();
        Booking initBooking = new Booking("BK-2026-000845", user1, b21A, r5, "Gandhipuram", "Ukkadam", today, LocalTime.of(11, 10), "A12", 1, 25.0);
        bookingRepository.save(initBooking);

        seatRepository.save(new Seat(b21A, "A12", today, initBooking, SeatStatus.BOOKED));
        paymentRepository.save(new Payment("PAY-2026-000845", initBooking, 25.0, "TXN-DEMO-2026001", PaymentStatus.SUCCESS, "UPI_GPAY"));

        String qr = ticketService.generateQrCodeBase64("SMARTBUS|TICKET:TKT-2026-000845|PASSENGER:PB2026001001|BUS:21A|SEAT:A12|DATE:" + today);
        ticketRepository.save(new Ticket(
                "TKT-2026-000845",
                initBooking,
                "Giridharun S",
                "PB2026001001",
                "21A",
                "Route 5 (Gandhipuram - Ukkadam)",
                "Gandhipuram",
                "Ukkadam",
                today,
                LocalTime.of(11, 10),
                "A12",
                25.0,
                qr
        ));

        // 7. SEED NOTIFICATIONS
        notificationRepository.save(new Notification(
                user1,
                "21A",
                "Booking Confirmed",
                "Your reservation for Bus 21A on Seat A12 is confirmed. Ticket # TKT-2026-000845",
                NotificationType.BOOKING_CONFIRMED
        ));

        // 8. SEED SAMPLE INCIDENT
        incidentRepository.save(new Incident(
                "INC-2026-001",
                b11D,
                null,
                IncidentType.DELAY,
                "15 minutes traffic delay near Hope College junction due to metro line road work.",
                "MEDIUM"
        ));

        System.out.println(">>> SmartBus Database successfully seeded with 10+ buses, 4 routes, 14 stops, sample schedules, bookings, and demo accounts.");
    }

    private String demoPassword(String configuredPassword) {
        if (configuredPassword == null || configuredPassword.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        if (configuredPassword.length() < 16) {
            throw new IllegalStateException("Configured demo passwords must contain at least 16 characters.");
        }
        return configuredPassword;
    }
}
