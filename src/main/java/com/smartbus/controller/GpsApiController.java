package com.smartbus.controller;

import com.smartbus.dto.GpsUpdateDTO;
import com.smartbus.dto.LiveBusLocationDTO;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import com.smartbus.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/gps")
public class GpsApiController {

    private final BusRepository busRepository;
    private final BusLocationRepository locationRepository;
    private final GpsSimulationService simulationService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${smartbus.gps.api-key:}")
    private String configuredApiKey;

    public GpsApiController(BusRepository busRepository,
                            BusLocationRepository locationRepository,
                            GpsSimulationService simulationService,
                            NotificationService notificationService,
                            SimpMessagingTemplate messagingTemplate) {
        this.busRepository = busRepository;
        this.locationRepository = locationRepository;
        this.simulationService = simulationService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Physical ESP32 / GPS Hardware Endpoint
     */
    @PostMapping("/location")
    public ResponseEntity<?> receiveGpsLocation(@RequestBody GpsUpdateDTO dto,
                                                @RequestHeader(value = "X-API-KEY", required = false) String headerKey) {
        String key = (headerKey != null) ? headerKey : dto.getApiKey();
        if (configuredApiKey == null || configuredApiKey.isBlank() || key == null
                || !MessageDigest.isEqual(configuredApiKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized GPS device key"));
        }

        Bus bus = busRepository.findByBusNumber(dto.getBusNumber())
                .or(() -> busRepository.findByGpsDeviceId(dto.getDeviceId()))
                .orElse(null);

        if (bus == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bus or Device ID not recognized"));
        }

        LocalDateTime now = LocalDateTime.now();
        bus.setCurrentLatitude(dto.getLatitude());
        bus.setCurrentLongitude(dto.getLongitude());
        bus.setCurrentSpeed(dto.getSpeed() != null ? dto.getSpeed() : 30.0);
        bus.setLastGpsUpdate(now);
        bus.setLocationSource(LocationSource.GPS);
        if (bus.getStatus() == BusStatus.GPS_SIGNAL_LOST) {
            bus.setStatus(BusStatus.RUNNING);
        }
        busRepository.save(bus);

        BusLocation loc = new BusLocation(bus, dto.getLatitude(), dto.getLongitude(), dto.getSpeed(), 0.0, now, LocationSource.GPS, bus.getNextStopName(), bus.getDistanceToNextStopKm(), bus.getEtaMinutes());
        locationRepository.save(loc);

        // Proximity checks
        notificationService.checkProximityAlerts(bus, dto.getLatitude(), dto.getLongitude());

        // Broadcast to WebSocket
        LiveBusLocationDTO liveDTO = simulationService.buildLocationDTO(bus);
        var publicLocation = com.smartbus.dto.PublicBusLocationDTO.from(liveDTO);
        messagingTemplate.convertAndSend("/topic/bus-location", publicLocation);
        messagingTemplate.convertAndSend("/topic/bus-location/" + bus.getId(), publicLocation);

        return ResponseEntity.ok(Map.of("success", true, "busNumber", bus.getBusNumber(), "timestamp", now.toString()));
    }

    /**
     * Demonstration Simulation Endpoints
     */
    @PostMapping("/simulate/{busId}/step")
    public ResponseEntity<LiveBusLocationDTO> stepBus(@PathVariable Long busId) {
        LiveBusLocationDTO dto = simulationService.stepBusSimulation(busId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/simulate/{busId}/signal-loss")
    public ResponseEntity<Map<String, Object>> simulateSignalLoss(@PathVariable Long busId) {
        simulationService.simulateSignalLoss(busId);
        return ResponseEntity.ok(Map.of("success", true, "message", "GPS signal loss triggered successfully for Bus ID: " + busId));
    }

    @PostMapping("/simulate/{busId}/resume")
    public ResponseEntity<Map<String, Object>> resumeGps(@PathVariable Long busId) {
        simulationService.resumeGps(busId);
        return ResponseEntity.ok(Map.of("success", true, "message", "GPS updates resumed successfully"));
    }
}
