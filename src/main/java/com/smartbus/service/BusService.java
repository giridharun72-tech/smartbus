package com.smartbus.service;

import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BusService {

    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final ScheduleRepository scheduleRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;

    public BusService(BusRepository busRepository,
                      RouteRepository routeRepository,
                      DriverRepository driverRepository,
                      ScheduleRepository scheduleRepository,
                      NotificationService notificationService,
                      AuditLogRepository auditLogRepository) {
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.driverRepository = driverRepository;
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Bus> getBusById(Long id) {
        return busRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Bus> getBusByNumber(String busNumber) {
        return busRepository.findByBusNumber(busNumber);
    }

    @Transactional
    public Bus updateBusStatus(Long busId, BusStatus newStatus, String username) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found with ID: " + busId));

        BusStatus oldStatus = bus.getStatus();
        bus.setStatus(newStatus);
        Bus updated = busRepository.save(bus);

        // Audit Log
        auditLogRepository.save(new AuditLog(
                username != null ? username : "SYSTEM",
                "ADMIN",
                "BUS_STATUS_UPDATED",
                "Bus " + bus.getBusNumber() + " status changed from " + oldStatus + " to " + newStatus,
                "127.0.0.1"
        ));

        // Push automatic notifications to subscribed passengers if cancelled or delayed
        if (newStatus == BusStatus.CANCELLED) {
            notificationService.notifyBusSubscribers(
                    bus,
                    "Bus Service Cancelled",
                    "Bus " + bus.getBusNumber() + " will not operate today due to service cancellation.",
                    NotificationType.CANCELLATION
            );
        } else if (newStatus == BusStatus.DELAYED) {
            notificationService.notifyBusSubscribers(
                    bus,
                    "Bus Service Delayed",
                    "Bus " + bus.getBusNumber() + " is currently delayed. We apologize for the inconvenience.",
                    NotificationType.DELAY
            );
        } else if (newStatus == BusStatus.BREAKDOWN) {
            notificationService.notifyBusSubscribers(
                    bus,
                    "Bus Technical Breakdown",
                    "Bus " + bus.getBusNumber() + " has reported a technical issue. Alternative services are being arranged.",
                    NotificationType.EMERGENCY
            );
        }

        return updated;
    }

    @Transactional
    public Bus saveBus(Bus bus) {
        return busRepository.save(bus);
    }
}
