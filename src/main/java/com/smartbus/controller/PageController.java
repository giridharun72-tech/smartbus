package com.smartbus.controller;

import com.smartbus.entity.*;
import com.smartbus.repository.*;
import com.smartbus.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Controller
public class PageController {

    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final BusStopRepository busStopRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PassengerProfileRepository profileRepository;
    private final DriverRepository driverRepository;
    private final IncidentRepository incidentRepository;
    private final SearchService searchService;

    public PageController(BusRepository busRepository,
                          RouteRepository routeRepository,
                          BusStopRepository busStopRepository,
                          BookingRepository bookingRepository,
                          TicketRepository ticketRepository,
                          UserRepository userRepository,
                          PassengerProfileRepository profileRepository,
                          DriverRepository driverRepository,
                          IncidentRepository incidentRepository,
                          SearchService searchService) {
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.busStopRepository = busStopRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.driverRepository = driverRepository;
        this.incidentRepository = incidentRepository;
        this.searchService = searchService;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("totalBuses", busRepository.count());
        model.addAttribute("totalRoutes", routeRepository.count());
        model.addAttribute("totalStops", busStopRepository.count());
        model.addAttribute("runningBuses", busRepository.findByStatus(BusStatus.RUNNING).size());
        model.addAttribute("routes", routeRepository.findAll());
        model.addAttribute("stops", busStopRepository.findAll());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/driver/login")
    public String driverLogin(Model model) {
        model.addAttribute("driverLogin", true);
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/guest/search")
    public String guestSearch(Model model) {
        model.addAttribute("stops", busStopRepository.findAll());
        model.addAttribute("routes", routeRepository.findAll());
        return "user/search";
    }

    // USER ROUTES
    @GetMapping("/user/dashboard")
    public String userDashboard(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        PassengerProfile profile = user != null ? profileRepository.findByUser(user).orElse(null) : null;
        model.addAttribute("user", user);
        model.addAttribute("profile", profile);

        List<Booking> bookings = user != null ? bookingRepository.findByUserOrderByCreatedAtDesc(user) : List.of();
        model.addAttribute("bookings", bookings);
        model.addAttribute("upcomingBooking", bookings.isEmpty() ? null : bookings.get(0));
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("stops", busStopRepository.findAll());
        return "user/dashboard";
    }

    @GetMapping("/user/search")
    public String userSearch(Model model) {
        model.addAttribute("stops", busStopRepository.findAll());
        model.addAttribute("routes", routeRepository.findAll());
        return "user/search";
    }

    @GetMapping("/user/nearby")
    public String userNearby(Model model, Authentication auth) {
        model.addAttribute("stops", busStopRepository.findAll());
        model.addAttribute("isPassenger", auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        return "user/nearby";
    }

    @GetMapping("/user/tracking/{busId}")
    public String userTracking(@PathVariable Long busId, Model model) {
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        model.addAttribute("bus", bus);
        model.addAttribute("stops", bus.getRoute() != null ? bus.getRoute().getRouteStops() : List.of());
        return "user/tracking";
    }

    @GetMapping("/user/booking/{busId}")
    public String userBooking(@PathVariable Long busId,
                              @RequestParam(required = false) String from,
                              @RequestParam(required = false) String to,
                              Model model) {
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        model.addAttribute("bus", bus);
        model.addAttribute("fromStop", from != null ? from : (bus.getRoute() != null ? bus.getRoute().getSource() : ""));
        model.addAttribute("toStop", to != null ? to : (bus.getRoute() != null ? bus.getRoute().getDestination() : ""));
        model.addAttribute("today", LocalDate.now());
        return "user/booking";
    }

    @GetMapping("/user/ticket/{ticketNumber}")
    public String viewTicket(@PathVariable String ticketNumber, Model model, Authentication auth) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketNumber));
        boolean privileged = auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_BCU"));
        boolean owner = auth != null && ticket.getBooking() != null
                && ticket.getBooking().getUser() != null
                && ticket.getBooking().getUser().getUsername().equals(auth.getName());
        if (!privileged && !owner) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
        }
        model.addAttribute("ticket", ticket);
        return "user/ticket";
    }

    @GetMapping("/user/history")
    public String travelHistory(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        PassengerProfile profile = user != null ? profileRepository.findByUser(user).orElse(null) : null;
        String passengerId = profile != null ? profile.getPassengerId() : "";

        List<Ticket> tickets = !passengerId.isBlank() ? ticketRepository.findByPassengerIdOrderByTravelDateDesc(passengerId) : List.of();
        model.addAttribute("tickets", tickets);
        model.addAttribute("passengerId", passengerId);
        return "user/history";
    }

    @GetMapping("/user/notifications")
    public String userNotifications(Model model, Authentication auth) {
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("stops", busStopRepository.findAll());
        return "user/notifications";
    }

    @GetMapping("/user/favorites")
    public String userFavorites(Model model) {
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("stops", busStopRepository.findAll());
        return "user/favorites";
    }

    // ADMIN ROUTES
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalBuses", busRepository.count());
        model.addAttribute("runningBuses", busRepository.findByStatus(BusStatus.RUNNING).size());
        model.addAttribute("cancelledBuses", busRepository.findByStatus(BusStatus.CANCELLED).size());
        model.addAttribute("delayedBuses", busRepository.findByStatus(BusStatus.DELAYED).size());
        model.addAttribute("todayBookings", bookingRepository.countConfirmedBookingsOnDate(LocalDate.now()));
        model.addAttribute("todayRevenue", bookingRepository.calculateRevenueOnDate(LocalDate.now()));
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("drivers", driverRepository.findAll());
        model.addAttribute("recentBookings", bookingRepository.findAll());
        return "admin/dashboard";
    }

    @GetMapping("/admin/assistant-chat")
    public String adminAssistantChat() {
        return "staff/support-chat";
    }

    @GetMapping("/admin/buses")
    public String adminBuses(Model model) {
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("routes", routeRepository.findAll());
        model.addAttribute("drivers", driverRepository.findAll());
        return "admin/buses";
    }

    @GetMapping("/admin/phone-gps")
    public String adminPhoneGps(Model model) {
        model.addAttribute("buses", busRepository.findAll());
        return "admin/phone-gps";
    }

    @GetMapping("/admin/routes")
    public String adminRoutes(Model model) {
        model.addAttribute("routes", routeRepository.findAll());
        model.addAttribute("stops", busStopRepository.findAll());
        return "admin/routes";
    }

    @GetMapping("/admin/drivers")
    public String adminDrivers(Model model) {
        model.addAttribute("drivers", driverRepository.findAll());
        model.addAttribute("buses", busRepository.findAll());
        return "admin/drivers";
    }

    @GetMapping("/admin/schedules")
    public String adminSchedules(Model model) {
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("routes", routeRepository.findAll());
        return "admin/schedules";
    }

    @GetMapping("/admin/bookings")
    public String adminBookings(Model model) {
        model.addAttribute("bookings", bookingRepository.findAll());
        return "admin/bookings";
    }

    @GetMapping("/admin/earnings")
    public String adminEarnings(Model model) {
        model.addAttribute("todayRevenue", bookingRepository.calculateRevenueOnDate(LocalDate.now()));
        model.addAttribute("buses", busRepository.findAll());
        return "admin/earnings";
    }

    @GetMapping("/admin/reports")
    public String adminReports(Model model) {
        model.addAttribute("today", LocalDate.now());
        return "admin/reports";
    }

    // BCU ROUTES
    @GetMapping("/bcu/dashboard")
    public String bcuDashboard(Model model) {
        model.addAttribute("totalBuses", busRepository.count());
        model.addAttribute("runningBuses", busRepository.findByStatus(BusStatus.RUNNING).size());
        model.addAttribute("delayedBuses", busRepository.findByStatus(BusStatus.DELAYED).size());
        model.addAttribute("signalLostBuses", busRepository.findByStatus(BusStatus.GPS_SIGNAL_LOST).size());
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("incidents", incidentRepository.findAllByOrderByReportedAtDesc());
        return "bcu/dashboard";
    }

    @GetMapping("/bcu/assistant-chat")
    public String bcuAssistantChat() {
        return "staff/support-chat";
    }

    @GetMapping("/bcu/buses")
    public String bcuBuses(Model model) {
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("routes", routeRepository.findAll());
        model.addAttribute("today", LocalDate.now());
        return "bcu/buses";
    }

    @GetMapping("/bcu/fleet-map")
    public String bcuFleetMap(Model model) {
        model.addAttribute("buses", busRepository.findAll());
        return "bcu/fleet-map";
    }

    @GetMapping("/bcu/signal-monitor")
    public String bcuSignalMonitor(Model model) {
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("signalLostBuses", busRepository.findByStatus(BusStatus.GPS_SIGNAL_LOST));
        return "bcu/signal-monitor";
    }

    @GetMapping("/bcu/verify-passenger")
    public String bcuVerifyPassenger() {
        return "bcu/verify-passenger";
    }

    @GetMapping("/bcu/incidents")
    public String bcuIncidents(Model model) {
        model.addAttribute("incidents", incidentRepository.findAllByOrderByReportedAtDesc());
        return "bcu/incidents";
    }

    @GetMapping("/bcu/simulation")
    public String bcuSimulation(Model model) {
        model.addAttribute("buses", busRepository.findAll());
        return "bcu/simulation";
    }

    // DRIVER ROUTES
    @GetMapping("/driver/dashboard")
    public String driverDashboard(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        Driver driver = user != null ? driverRepository.findByUser(user).orElse(null) : null;
        Bus bus = driver != null ? busRepository.findByDriver(driver).orElse(null) : null;

        model.addAttribute("driver", driver);
        model.addAttribute("bus", bus);
        model.addAttribute("stops", bus != null && bus.getRoute() != null ? bus.getRoute().getRouteStops() : List.of());
        return "driver/dashboard";
    }

    @GetMapping("/driver/manual-location")
    public String driverManualLocation(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        Driver driver = user != null ? driverRepository.findByUser(user).orElse(null) : null;
        Bus bus = driver != null ? busRepository.findByDriver(driver).orElse(null) : null;

        model.addAttribute("driver", driver);
        model.addAttribute("bus", bus);
        model.addAttribute("stops", bus != null && bus.getRoute() != null ? bus.getRoute().getRouteStops() : List.of());
        return "driver/manual-location";
    }

    @GetMapping("/driver/verify-ticket")
    public String driverVerifyTicket() {
        return "driver/verify-ticket";
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
