package com.smartbus.service;

import com.smartbus.dto.LiveBusLocationDTO;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GpsSimulationService {

    private final BusRepository busRepository;
    private final BusLocationRepository locationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final IncidentRepository incidentRepository;
    private final AuditLogRepository auditLogRepository;

    private final Map<Long, Integer> busWaypointIndex = new ConcurrentHashMap<>();
    private final Set<Long> signalLostSimulatedBuses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static class Waypoint {
        public final double lat;
        public final double lon;
        public final double speed;
        public final String stopName;

        public Waypoint(double lat, double lon, double speed, String stopName) {
            this.lat = lat;
            this.lon = lon;
            this.speed = speed;
            this.stopName = stopName;
        }
    }

    private static final List<Waypoint> ROUTE_WAYPOINTS = List.of(
            new Waypoint(11.0168, 76.9558, 28.0, "Gandhipuram Bus Stand"),
            new Waypoint(11.0135, 76.9602, 34.0, "Cross Cut Road Junction"),
            new Waypoint(11.0090, 76.9650, 31.0, "Central Market"),
            new Waypoint(11.0030, 76.9675, 25.0, "Railway Station Junction"),
            new Waypoint(10.9980, 76.9650, 32.0, "Town Hall"),
            new Waypoint(10.9920, 76.9620, 30.0, "Big Bazaar Street"),
            new Waypoint(10.9870, 76.9580, 22.0, "Ukkadam Bus Terminal")
    );

    public GpsSimulationService(BusRepository busRepository,
                                BusLocationRepository locationRepository,
                                SimpMessagingTemplate messagingTemplate,
                                NotificationService notificationService,
                                IncidentRepository incidentRepository,
                                AuditLogRepository auditLogRepository) {
        this.busRepository = busRepository;
        this.locationRepository = locationRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.incidentRepository = incidentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public LiveBusLocationDTO stepBusSimulation(Long busId) {
        if (signalLostSimulatedBuses.contains(busId)) {
            throw new IllegalStateException("GPS Signal is currently cut off for this bus (Simulation Mode). Please resume GPS first.");
        }

        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found with ID: " + busId));

        int currentIndex = busWaypointIndex.getOrDefault(busId, 0);
        Waypoint wp = ROUTE_WAYPOINTS.get(currentIndex % ROUTE_WAYPOINTS.size());
        Waypoint nextWp = ROUTE_WAYPOINTS.get((currentIndex + 1) % ROUTE_WAYPOINTS.size());

        busWaypointIndex.put(busId, currentIndex + 1);

        double distKm = TrackingService.calculateDistanceKm(wp.lat, wp.lon, nextWp.lat, nextWp.lon);
        int etaMin = TrackingService.calculateEtaMinutes(distKm, wp.speed);

        LocalDateTime now = LocalDateTime.now();
        bus.setCurrentLatitude(wp.lat);
        bus.setCurrentLongitude(wp.lon);
        bus.setCurrentSpeed(wp.speed);
        bus.setNextStopName(nextWp.stopName);
        bus.setDistanceToNextStopKm(distKm);
        bus.setEtaMinutes(etaMin);
        bus.setLastGpsUpdate(now);
        bus.setLocationSource(LocationSource.GPS);
        if (bus.getStatus() == BusStatus.GPS_SIGNAL_LOST || bus.getStatus() == BusStatus.MANUAL_LOCATION_MODE) {
            bus.setStatus(BusStatus.RUNNING);
        }
        busRepository.save(bus);

        BusLocation loc = new BusLocation(bus, wp.lat, wp.lon, wp.speed, 0.0, now, LocationSource.GPS, nextWp.stopName, distKm, etaMin);
        locationRepository.save(loc);

        notificationService.checkProximityAlerts(bus, wp.lat, wp.lon);

        LiveBusLocationDTO dto = buildLocationDTO(bus);
        try {
            var publicLocation = com.smartbus.dto.PublicBusLocationDTO.from(dto);
            messagingTemplate.convertAndSend("/topic/bus-location", publicLocation);
            messagingTemplate.convertAndSend("/topic/bus-location/" + bus.getId(), publicLocation);
        } catch (Exception ignored) {}

        return dto;
    }

    @Transactional
    public void simulateSignalLoss(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found with ID: " + busId));

        signalLostSimulatedBuses.add(busId);
        bus.setLastGpsUpdate(LocalDateTime.now().minusSeconds(45));
        bus.setStatus(BusStatus.GPS_SIGNAL_LOST);
        busRepository.save(bus);

        if (incidentRepository.findByBusAndTypeAndStatus(
            bus, IncidentType.GPS_SIGNAL_LOST, IncidentStatus.OPEN).isEmpty()) {
            String incRef = "INC-GPS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Incident incident = new Incident(
                incRef,
                bus,
                bus.getDriver(),
                IncidentType.GPS_SIGNAL_LOST,
                "GPS heartbeat timeout detected. Signal lost for Bus " + bus.getBusNumber() + " at " + bus.getNextStopName(),
                "HIGH"
            );
            incidentRepository.save(incident);
        }

        auditLogRepository.save(new AuditLog(
                "BCU_SYSTEM",
                "SYSTEM",
                "GPS_SIGNAL_LOST_TRIGGERED",
                "Simulated GPS signal loss triggered for Bus " + bus.getBusNumber(),
                "127.0.0.1"
        ));

        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "GPS_SIGNAL_LOST");
        alert.put("busId", bus.getId());
        alert.put("busNumber", bus.getBusNumber());
        alert.put("lastKnownLocation", bus.getNextStopName() + " (" + bus.getCurrentLatitude() + ", " + bus.getCurrentLongitude() + ")");
        alert.put("lastUpdate", bus.getLastGpsUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        alert.put("message", "CRITICAL ALERT: GPS signal lost for Bus " + bus.getBusNumber() + ". Enable manual mode or contact driver.");

        try {
            messagingTemplate.convertAndSend("/topic/bcu-alerts", (Object) alert);
            messagingTemplate.convertAndSend("/topic/bus-location/" + bus.getId(),
                    com.smartbus.dto.PublicBusLocationDTO.from(buildLocationDTO(bus)));
        } catch (Exception ignored) {}
    }

    @Transactional
    public void resumeGps(Long busId) {
        signalLostSimulatedBuses.remove(busId);
        stepBusSimulation(busId);
    }

    public LiveBusLocationDTO buildLocationDTO(Bus bus) {
        LiveBusLocationDTO dto = new LiveBusLocationDTO();
        dto.setBusId(bus.getId());
        dto.setBusNumber(bus.getBusNumber());
        dto.setRegistrationNumber(bus.getRegistrationNumber());
        dto.setGpsDeviceId(bus.getGpsDeviceId());
        dto.setCapacity(bus.getCapacity());
        dto.setRouteName(bus.getRoute() != null ? bus.getRoute().getName() : "Local Route");
        dto.setLatitude(bus.getCurrentLatitude());
        dto.setLongitude(bus.getCurrentLongitude());
        dto.setSpeed(bus.getCurrentSpeed());
        dto.setStatus(bus.getStatus().name());
        dto.setNextStopName(bus.getNextStopName());
        dto.setDistanceToNextKm(bus.getDistanceToNextStopKm());
        dto.setEtaMinutes(bus.getEtaMinutes());
        dto.setSource(bus.getLocationSource().name());
        dto.setLastUpdated(bus.getLastGpsUpdate() != null ? bus.getLastGpsUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "N/A");
        if (bus.getDriver() != null) {
            dto.setDriverName(bus.getDriver().getFullName());
            dto.setDriverPhone(bus.getDriver().getPhone());
        } else {
            dto.setDriverName(bus.getAssignedDriverName());
        }
        return dto;
    }
}