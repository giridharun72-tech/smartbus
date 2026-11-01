package com.smartbus.controller;

import com.smartbus.dto.LiveBusLocationDTO;
import com.smartbus.dto.ManualLocationDTO;
import com.smartbus.dto.PhoneGpsLocationRequest;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import com.smartbus.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverApiController {

    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final BusLocationRepository locationRepository;
    private final ManualLocationUpdateRepository manualUpdateRepository;
    private final IncidentRepository incidentRepository;
    private final GpsSimulationService simulationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AdminBusService adminBusService;

    public DriverApiController(BusRepository busRepository,
                               DriverRepository driverRepository,
                               UserRepository userRepository,
                               BusLocationRepository locationRepository,
                               ManualLocationUpdateRepository manualUpdateRepository,
                               IncidentRepository incidentRepository,
                               GpsSimulationService simulationService,
                               SimpMessagingTemplate messagingTemplate,
                               AdminBusService adminBusService) {
        this.busRepository = busRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.manualUpdateRepository = manualUpdateRepository;
        this.incidentRepository = incidentRepository;
        this.simulationService = simulationService;
        this.messagingTemplate = messagingTemplate;
        this.adminBusService = adminBusService;
    }

    @PostMapping("/location")
    public ResponseEntity<?> updateDeviceLocation(@Valid @RequestBody PhoneGpsLocationRequest request,
                                                  Authentication auth) {
        Bus bus = resolveAssignedDriverBus(auth);
        LiveBusLocationDTO location = adminBusService.updatePhoneLocation(bus.getId(), request);
        return ResponseEntity.ok(location);
    }

    @PostMapping("/manual-location")
    public ResponseEntity<?> submitManualLocation(@RequestBody ManualLocationDTO dto, Authentication auth) {
        Bus bus = busRepository.findById(dto.getBusId())
                .orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        ensureBusAccess(bus, auth);

        if (!bus.isManualLocationAllowed()) {
            return ResponseEntity.status(403).body(Map.of("error", "Manual location mode has NOT been authorized by BCU."));
        }

        Driver driver = bus.getDriver();
        if (driver == null && auth != null) {
            userRepository.findByUsername(auth.getName())
                    .flatMap(driverRepository::findByUser)
                    .ifPresent(d -> bus.setDriver(d));
            driver = bus.getDriver();
        }

        LocalDateTime now = LocalDateTime.now();
        bus.setCurrentLatitude(dto.getLatitude());
        bus.setCurrentLongitude(dto.getLongitude());
        bus.setNextStopName(dto.getLandmark() != null ? dto.getLandmark() : "Manual Station");
        bus.setLastGpsUpdate(now);
        bus.setLocationSource(LocationSource.MANUAL);
        busRepository.save(bus);

        ManualLocationUpdate manual = new ManualLocationUpdate(
                bus,
                driver,
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getLandmark(),
                dto.getReason() != null ? dto.getReason() : "Driver manual position update",
                true
        );
        manualUpdateRepository.save(manual);

        BusLocation loc = new BusLocation(bus, dto.getLatitude(), dto.getLongitude(), 0.0, 0.0, now, LocationSource.MANUAL, bus.getNextStopName(), 1.0, 5);
        locationRepository.save(loc);

        // Broadcast to WebSocket with MANUAL source indicator
        LiveBusLocationDTO liveDTO = simulationService.buildLocationDTO(bus);
        liveDTO.setSource("MANUAL");
        var publicLocation = com.smartbus.dto.PublicBusLocationDTO.from(liveDTO);
        messagingTemplate.convertAndSend("/topic/bus-location", publicLocation);
        messagingTemplate.convertAndSend("/topic/bus-location/" + bus.getId(), publicLocation);

        return ResponseEntity.ok(Map.of("success", true, "message", "Manual location updated successfully", "source", "MANUAL"));
    }

    @PostMapping("/report-incident")
    public ResponseEntity<?> reportIncident(@RequestBody Map<String, Object> req, Authentication auth) {
        Long busId = Long.valueOf(req.get("busId").toString());
        String typeStr = (String) req.get("type");
        String desc = (String) req.get("description");

        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        ensureBusAccess(bus, auth);

        IncidentType type = IncidentType.valueOf(typeStr);
        if (type == IncidentType.BREAKDOWN) {
            bus.setStatus(BusStatus.BREAKDOWN);
        } else if (type == IncidentType.EMERGENCY) {
            bus.setStatus(BusStatus.EMERGENCY);
        }
        busRepository.save(bus);

        String ref = "INC-DRV-" + System.currentTimeMillis() % 100000;
        Incident inc = new Incident(ref, bus, bus.getDriver(), type, desc, "CRITICAL");
        incidentRepository.save(inc);

        messagingTemplate.convertAndSend("/topic/bcu-alerts", (Object) Map.of(
                "type", type.name(),
                "busNumber", bus.getBusNumber(),
                "message", "DRIVER REPORT: " + type + " reported on Bus " + bus.getBusNumber() + " - " + desc
        ));

        return ResponseEntity.ok(Map.of("success", true, "incidentReference", ref));
    }

    private Bus resolveAssignedDriverBus(Authentication auth) {
        if (auth == null) {
            throw new AccessDeniedException("Driver authentication is required.");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Driver account not found."));
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new AccessDeniedException("No driver profile is assigned to this account."));
        return busRepository.findByDriver(driver)
                .orElseThrow(() -> new IllegalStateException("No bus is assigned to this driver."));
    }

    private void ensureBusAccess(Bus bus, Authentication auth) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication is required.");
        }
        boolean isDriver = auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DRIVER".equals(authority.getAuthority()));
        if (isDriver) {
            Bus assignedBus = resolveAssignedDriverBus(auth);
            if (!assignedBus.getId().equals(bus.getId())) {
                throw new AccessDeniedException("Drivers can only update their assigned bus.");
            }
        }
    }
}
