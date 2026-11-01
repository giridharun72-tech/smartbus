package com.smartbus.controller;

import com.smartbus.dto.LiveBusLocationDTO;
import com.smartbus.dto.CreateDriverBusRequest;
import com.smartbus.dto.BcuBusUpdateRequest;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import com.smartbus.service.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bcu")
public class BcuApiController {

    private final BusRepository busRepository;
    private final PassengerProfileRepository profileRepository;
    private final TicketRepository ticketRepository;
    private final IncidentRepository incidentRepository;
    private final GpsSimulationService simulationService;
    private final PdfService pdfService;
    private final AuditLogRepository auditLogRepository;
    private final BcuFleetService bcuFleetService;

    public BcuApiController(BusRepository busRepository,
                            PassengerProfileRepository profileRepository,
                            TicketRepository ticketRepository,
                            IncidentRepository incidentRepository,
                            GpsSimulationService simulationService,
                            PdfService pdfService,
                            AuditLogRepository auditLogRepository,
                            BcuFleetService bcuFleetService) {
        this.busRepository = busRepository;
        this.profileRepository = profileRepository;
        this.ticketRepository = ticketRepository;
        this.incidentRepository = incidentRepository;
        this.simulationService = simulationService;
        this.pdfService = pdfService;
        this.auditLogRepository = auditLogRepository;
        this.bcuFleetService = bcuFleetService;
    }

    @PostMapping("/buses")
    public ResponseEntity<?> createBusWithDriver(@Valid @RequestBody CreateDriverBusRequest request) {
        try {
            Bus bus = bcuFleetService.createBusWithDriver(request);
            return ResponseEntity.status(201).body(Map.of(
                    "busId", bus.getId(),
                    "busNumber", bus.getBusNumber(),
                    "registrationNumber", bus.getRegistrationNumber(),
                    "routeName", bus.getRoute().getName(),
                    "driverId", bus.getDriver().getDriverCode(),
                    "driverName", bus.getDriver().getFullName()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409)
                    .body(Map.of("message", "A bus, driver ID, phone number, or Aadhaar number is already registered."));
        }
    }

    @PutMapping("/buses/{busId}")
    public ResponseEntity<?> updateBus(@PathVariable Long busId,
                                       @Valid @RequestBody BcuBusUpdateRequest request) {
        try {
            Bus bus = bcuFleetService.updateBus(busId, request);
            return ResponseEntity.ok(Map.of(
                    "busId", bus.getId(),
                    "busNumber", bus.getBusNumber(),
                    "driverName", bus.getDriver().getFullName(),
                    "routeName", bus.getRoute().getName()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body(Map.of("message", "The bus or driver details conflict with an existing record."));
        }
    }

    @GetMapping("/fleet")
    public ResponseEntity<List<LiveBusLocationDTO>> getFleetLive() {
        List<Bus> buses = busRepository.findAll();
        List<LiveBusLocationDTO> dtos = buses.stream()
                .map(simulationService::buildLocationDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/manual-location/enable")
    public ResponseEntity<?> enableManualLocation(@RequestParam Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        bus.setManualLocationAllowed(true);
        bus.setStatus(BusStatus.MANUAL_LOCATION_MODE);
        busRepository.save(bus);

        auditLogRepository.save(new AuditLog("BCU_OFFICER", "ROLE_BCU", "MANUAL_LOCATION_ENABLED", "Manual location entry authorized for Bus " + bus.getBusNumber(), "127.0.0.1"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Manual location mode enabled for Bus " + bus.getBusNumber()));
    }

    @PostMapping("/manual-location/disable")
    public ResponseEntity<?> disableManualLocation(@RequestParam Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        bus.setManualLocationAllowed(false);
        bus.setStatus(BusStatus.RUNNING);
        busRepository.save(bus);

        auditLogRepository.save(new AuditLog("BCU_OFFICER", "ROLE_BCU", "MANUAL_LOCATION_DISABLED", "Manual location mode revoked for Bus " + bus.getBusNumber(), "127.0.0.1"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Manual location mode disabled"));
    }

    @GetMapping("/passengers/{passengerId}")
    public ResponseEntity<?> getPassengerVerification(@PathVariable String passengerId) {
        Optional<PassengerProfile> profileOpt = profileRepository.findByPassengerId(passengerId);
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PassengerProfile profile = profileOpt.get();
        List<Ticket> tickets = ticketRepository.findByPassengerIdOrderByTravelDateDesc(passengerId);
        Ticket activeTicket = tickets.stream().filter(t -> t.getVerificationStatus() == TicketStatus.VALID).findFirst().orElse(null);

        Map<String, Object> res = new HashMap<>();
        res.put("passengerId", profile.getPassengerId());
        res.put("fullName", profile.getFullName());
        res.put("phone", profile.getPhone());
        res.put("activeTicket", activeTicket);
        res.put("totalTrips", tickets.size());

        auditLogRepository.save(new AuditLog("BCU_STAFF", "ROLE_BCU", "PASSENGER_DATA_ACCESSED", "Accessed records for Passenger ID: " + passengerId, "127.0.0.1"));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/passengers/{passengerId}/history-pdf")
    public ResponseEntity<byte[]> downloadTravelHistoryPdf(@PathVariable String passengerId) {
        byte[] pdfBytes = pdfService.generateTravelHistoryPdf(passengerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Passenger_Travel_History_" + passengerId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<Incident>> getIncidents() {
        return ResponseEntity.ok(incidentRepository.findAllByOrderByReportedAtDesc());
    }

    @PostMapping("/incidents/{id}/resolve")
    public ResponseEntity<?> resolveIncident(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolution(payload.getOrDefault("resolution", "Resolved by BCU Control Center"));
        incidentRepository.save(incident);

        return ResponseEntity.ok(Map.of("success", true, "message", "Incident marked as RESOLVED"));
    }
}
