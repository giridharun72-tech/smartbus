package com.smartbus.service;

import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SignalLossMonitoringService {

    private final BusRepository busRepository;
    private final IncidentRepository incidentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditLogRepository auditLogRepository;

    @Value("${smartbus.gps.signal-timeout-seconds:30}")
    private int signalTimeoutSeconds;

    public SignalLossMonitoringService(BusRepository busRepository,
                                       IncidentRepository incidentRepository,
                                       SimpMessagingTemplate messagingTemplate,
                                       AuditLogRepository auditLogRepository) {
        this.busRepository = busRepository;
        this.incidentRepository = incidentRepository;
        this.messagingTemplate = messagingTemplate;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Checks all buses every 5 seconds for heartbeat timeout.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void monitorGpsHeartbeat() {
        List<Bus> activeBuses = busRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Bus bus : activeBuses) {
            // Only monitor running or delayed buses
            if (bus.getStatus() != BusStatus.RUNNING && bus.getStatus() != BusStatus.DELAYED) {
                continue;
            }

            if (bus.getLastGpsUpdate() == null) continue;

            long elapsedSeconds = Duration.between(bus.getLastGpsUpdate(), now).getSeconds();

            if (elapsedSeconds >= signalTimeoutSeconds) {
                // Heartbeat timeout detected
                bus.setStatus(BusStatus.GPS_SIGNAL_LOST);
                busRepository.save(bus);

                if (incidentRepository.findByBusAndTypeAndStatus(
                    bus, IncidentType.GPS_SIGNAL_LOST, IncidentStatus.OPEN).isEmpty()) {
                    String incRef = "INC-SIG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    Incident incident = new Incident(
                        incRef,
                        bus,
                        bus.getDriver(),
                        IncidentType.GPS_SIGNAL_LOST,
                        "GPS Heartbeat Timeout (" + elapsedSeconds + "s without update). Bus " + bus.getBusNumber() + " last seen near " + bus.getNextStopName(),
                        "HIGH"
                    );
                    incidentRepository.save(incident);
                }

                auditLogRepository.save(new AuditLog(
                        "SYSTEM_MONITOR",
                        "BCU",
                        "SIGNAL_LOSS_DETECTED",
                        "Signal loss alert for Bus " + bus.getBusNumber() + " (" + elapsedSeconds + "s elapsed)",
                        "127.0.0.1"
                ));

                // Broadcast alert to BCU
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "GPS_SIGNAL_LOST");
                alert.put("busId", bus.getId());
                alert.put("busNumber", bus.getBusNumber());
                alert.put("elapsedSeconds", elapsedSeconds);
                alert.put("lastKnownLocation", bus.getNextStopName() != null ? bus.getNextStopName() : "In Transit");
                alert.put("lastUpdate", bus.getLastGpsUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                alert.put("message", "GPS SIGNAL LOST: Bus " + bus.getBusNumber() + " has not sent GPS data for " + elapsedSeconds + " seconds.");

                try {
                    messagingTemplate.convertAndSend("/topic/bcu-alerts", (Object) alert);
                } catch (Exception ignored) {}
            }
        }
    }
}
