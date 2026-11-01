package com.smartbus.service;

import com.smartbus.dto.CreateDriverBusRequest;
import com.smartbus.dto.BcuBusUpdateRequest;
import com.smartbus.dto.NewRouteStopRequest;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BcuFleetService {
    private static final Pattern TN_REGISTRATION =
            Pattern.compile("(?i)^TN[- ]?(\\d{1,2})[- ]?([A-Z]{1,3})[- ]?(\\d{1,4})$");

    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final BusStopRepository busStopRepository;
    private final RouteStopRepository routeStopRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String aadhaarHmacKey;

    public BcuFleetService(BusRepository busRepository,
                           RouteRepository routeRepository,
                           DriverRepository driverRepository,
                           BusStopRepository busStopRepository,
                           RouteStopRepository routeStopRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${smartbus.driver.aadhaar-hmac-key:}") String aadhaarHmacKey) {
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.driverRepository = driverRepository;
        this.busStopRepository = busStopRepository;
        this.routeStopRepository = routeStopRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.aadhaarHmacKey = aadhaarHmacKey;
    }

    @Transactional
    public Bus createBusWithDriver(CreateDriverBusRequest request) {
        if (aadhaarHmacKey == null || aadhaarHmacKey.length() < 32) {
            throw new IllegalStateException("Configure SMARTBUS_AADHAAR_HMAC_KEY with a stable secret of at least 32 characters before creating driver accounts.");
        }

        String driverId = request.getDriverId().trim().toUpperCase(Locale.ROOT);
        String busNumber = request.getBusNumber().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        String registrationNumber = normalizeRegistration(request.getRegistrationNumber());
        String phone = normalizePhone(request.getPhone());
        String aadhaar = request.getAadhaarNumber().trim();

        if (driverRepository.findByDriverCodeIgnoreCase(driverId).isPresent()
                || userRepository.existsByUsernameIgnoreCase(driverId)) {
            throw new IllegalArgumentException("That Driver ID is already in use.");
        }
        if (busRepository.findFirstByBusNumberIgnoreCase(busNumber).isPresent()) {
            throw new IllegalArgumentException("A bus with that bus number already exists.");
        }
        if (busRepository.existsByRegistrationNumberIgnoreCase(registrationNumber)) {
            throw new IllegalArgumentException("That vehicle registration number is already in use.");
        }
        if (phone.length() != 10) {
            throw new IllegalArgumentException("Enter a valid 10-digit Indian phone number, optionally prefixed by +91.");
        }
        boolean duplicatePhone = driverRepository.findAll().stream()
                .map(Driver::getPhone)
                .filter(existing -> existing != null)
                .anyMatch(existing -> normalizePhone(existing).equals(phone));
        if (duplicatePhone) {
            throw new IllegalArgumentException("That phone number is already registered to a driver.");
        }

        String fingerprint = fingerprint(aadhaar);
        if (driverRepository.findByAadhaarFingerprint(fingerprint).isPresent()) {
            throw new IllegalArgumentException("That Aadhaar number is already registered.");
        }

        String email = driverId.toLowerCase(Locale.ROOT) + "@driver.smartbus.local";
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("That Driver ID is already in use.");
        }
        User user = userRepository.save(new User(driverId, email, passwordEncoder.encode(request.getPassword()), Role.ROLE_DRIVER));
        Driver driver = new Driver(user, driverId, request.getDriverName().trim(), phone,
                request.getLicenseNumber().trim());
        driver.setAadhaarLastFour(aadhaar.substring(aadhaar.length() - 4));
        driver.setAadhaarFingerprint(fingerprint);
        driver = driverRepository.save(driver);

        String routeName = request.getRoute().trim();
        Route route = routeRepository.findFirstByNameIgnoreCase(routeName).orElse(null);
        if (route == null) {
            route = createRoute(routeName, request.getRouteStops());
        } else if (route.getRouteStops().isEmpty() && request.getRouteStops() != null
                && !request.getRouteStops().isEmpty()) {
            addRouteStops(route, request.getRouteStops());
        }

        Bus bus = new Bus();
        bus.setBusNumber(busNumber);
        bus.setRegistrationNumber(registrationNumber);
        bus.setBusType("STANDARD");
        bus.setCapacity(request.getTotalSeats());
        bus.setRoute(route);
        bus.setDriver(driver);
        bus.setAssignedDriverName(driver.getFullName());
        bus.setGpsDeviceId("GPS-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
        bus.setCurrentSpeed(0.0);
        bus.setNextStopName("Waiting for driver GPS");
        bus.setDistanceToNextStopKm(0.0);
        bus.setEtaMinutes(0);
        bus.setStatus(BusStatus.RUNNING);
        bus.setLastGpsUpdate(LocalDateTime.now());
        bus.setLocationSource(LocationSource.GPS);
        return busRepository.saveAndFlush(bus);
    }

    @Transactional
    public Bus updateBus(Long busId, BcuBusUpdateRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found."));
        Driver driver = bus.getDriver();
        if (driver == null) {
            throw new IllegalArgumentException("Assign a driver to this bus before editing driver details.");
        }

        String busNumber = request.getBusNumber().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        String registrationNumber = normalizeRegistration(request.getRegistrationNumber());
        String driverPhone = normalizePhone(request.getDriverPhone());
        String licenseNumber = request.getLicenseNumber().trim().toUpperCase(Locale.ROOT);
        if (driverPhone.length() != 10) {
            throw new IllegalArgumentException("Enter a valid 10-digit Indian phone number.");
        }
        if (busRepository.existsByBusNumberIgnoreCaseAndIdNot(busNumber, busId)) {
            throw new IllegalArgumentException("A bus with that bus number already exists.");
        }
        if (busRepository.existsByRegistrationNumberIgnoreCaseAndIdNot(registrationNumber, busId)) {
            throw new IllegalArgumentException("That vehicle registration number is already in use.");
        }
        if (driverRepository.existsByPhoneAndIdNot(driverPhone, driver.getId())) {
            throw new IllegalArgumentException("That phone number is already registered to another driver.");
        }
        if (driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(licenseNumber, driver.getId())) {
            throw new IllegalArgumentException("That driving licence number is already registered.");
        }

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new IllegalArgumentException("Selected route was not found."));
        driver.setFullName(request.getDriverName().trim());
        driver.setPhone(driverPhone);
        driver.setLicenseNumber(licenseNumber);
        driverRepository.save(driver);

        bus.setBusNumber(busNumber);
        bus.setRegistrationNumber(registrationNumber);
        bus.setCapacity(request.getTotalSeats());
        bus.setRoute(route);
        bus.setAssignedDriverName(driver.getFullName());
        return busRepository.save(bus);
    }

    private String normalizeRegistration(String input) {
        Matcher matcher = TN_REGISTRATION.matcher(input.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Vehicle number must be a valid Tamil Nadu plate beginning with TN.");
        }
        return ("TN-" + matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3))
                .toUpperCase(Locale.ROOT);
    }

    private String normalizePhone(String input) {
        String digits = input.replaceAll("\\D", "");
        if (digits.startsWith("0091") && digits.length() == 14) {
            return digits.substring(4);
        }
        if (digits.startsWith("91") && digits.length() == 12) {
            return digits.substring(2);
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            return digits.substring(1);
        }
        return digits;
    }

    private String fingerprint(String aadhaar) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(aadhaarHmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(aadhaar.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to securely process the Aadhaar number.", ex);
        }
    }

    private Route createRoute(String routeName, java.util.List<NewRouteStopRequest> stops) {
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("A new route needs at least two stops with names and latitude/longitude coordinates.");
        }
        String[] endpoints = routeName.split("\\s+-\\s+", 2);
        String source = endpoints[0].trim();
        String destination = endpoints.length > 1 ? endpoints[1].trim() : source;
        String routeNumber = "NEW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Route route = routeRepository.save(new Route(routeNumber, routeName, source, destination, 0.0, 0));
        addRouteStops(route, stops);
        return route;
    }

    private void addRouteStops(Route route, java.util.List<NewRouteStopRequest> stops) {
        for (int i = 0; i < stops.size(); i++) {
            NewRouteStopRequest input = stops.get(i);
            BusStop stop = busStopRepository.save(new BusStop(
                    input.getName().trim(), input.getLatitude(), input.getLongitude(), null));
            routeStopRepository.save(new RouteStop(route, stop, i + 1, null, null, null));
        }
    }
}
