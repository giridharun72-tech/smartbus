package com.smartbus.service;

import com.smartbus.dto.NewRouteStopRequest;
import com.smartbus.dto.RouteServiceChangeRequest;
import com.smartbus.dto.RouteServiceChangeResponse;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RouteServiceChangeService {
    private final RouteServiceChangeRepository changeRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public RouteServiceChangeService(    RouteServiceChangeRepository changeRepository,
    RouteRepository routeRepository,
    BusRepository busRepository,
    NotificationService notificationService,
                                     SimpMessagingTemplate messagingTemplate) {
        this.changeRepository = changeRepository;
        this.routeRepository = routeRepository;
        this.busRepository = busRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public RouteServiceChangeResponse findActive(Long routeId, LocalDate date) {
        return changeRepository.findByRouteIdAndEffectiveDateAndActiveTrue(routeId, date)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public RouteServiceChangeResponse save(Long routeId, RouteServiceChangeRequest request) {
        if (request.getEffectiveDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A route change cannot be applied to a past date.");
        }
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found."));
        RouteServiceChange change = changeRepository.findByRouteIdAndEffectiveDate(routeId, request.getEffectiveDate())
                .orElseGet(RouteServiceChange::new);

        Bus affectedBus = request.getAffectedBusId() == null ? null : busRepository.findById(request.getAffectedBusId())
                .orElseThrow(() -> new IllegalArgumentException("Affected bus not found."));
        Bus replacementBus = request.getReplacementBusId() == null ? null : busRepository.findById(request.getReplacementBusId())
                .orElseThrow(() -> new IllegalArgumentException("Replacement bus not found."));
        if (affectedBus != null && (affectedBus.getRoute() == null || !affectedBus.getRoute().getId().equals(routeId))) {
            throw new IllegalArgumentException("The affected bus must be assigned to the selected route.");
        }
        if (affectedBus != null && replacementBus != null && affectedBus.getId().equals(replacementBus.getId())) {
            throw new IllegalArgumentException("A bus cannot be both affected and its own replacement.");
        }
        Bus previousAffectedBus = change.getAffectedBus();
        if (request.getEffectiveDate().equals(LocalDate.now()) && previousAffectedBus != null
                && previousAffectedBus.getStatus() == BusStatus.BREAKDOWN
                && (affectedBus == null || !previousAffectedBus.getId().equals(affectedBus.getId()))) {
            previousAffectedBus.setStatus(change.getPreviousAffectedBusStatus() == null
                    ? BusStatus.RUNNING : change.getPreviousAffectedBusStatus());
            busRepository.save(previousAffectedBus);
        }

        if (request.getEffectiveDate().equals(LocalDate.now()) && affectedBus != null) {
            BusStatus previousStatus = previousAffectedBus != null
                    && previousAffectedBus.getId().equals(affectedBus.getId())
                    ? change.getPreviousAffectedBusStatus() : affectedBus.getStatus();
            change.setPreviousAffectedBusStatus(previousStatus == BusStatus.BREAKDOWN ? BusStatus.RUNNING : previousStatus);
        } else {
            change.setPreviousAffectedBusStatus(null);
        }
        change.setRoute(route);
        change.setEffectiveDate(request.getEffectiveDate());
        change.setReason(request.getReason().trim());
        change.setAffectedBus(affectedBus);
        change.setReplacementBus(replacementBus);
        change.setActive(true);
        change.getStops().clear();
        for (int i = 0; i < request.getStops().size(); i++) {
            NewRouteStopRequest stop = request.getStops().get(i);
            change.getStops().add(new RouteServiceChangeStop(
                    change, stop.getName().trim(), stop.getLatitude(), stop.getLongitude(), i + 1));
        }
        change = changeRepository.saveAndFlush(change);

        if (affectedBus != null && request.getEffectiveDate().equals(LocalDate.now())) {
            affectedBus.setStatus(BusStatus.BREAKDOWN);
            busRepository.save(affectedBus);
        }

        String message = "Route update for " + route.getName() + " on " + request.getEffectiveDate()
                + ": " + request.getReason().trim()
                + (replacementBus == null ? "" : " Replacement bus: " + replacementBus.getBusNumber() + ".");
        for (Bus bus : busRepository.findByRoute(route)) {
            notificationService.notifyBusSubscribers(bus, "Route service update", message, NotificationType.ROUTE_CHANGE);
        }
        RouteServiceChangeResponse response = toResponse(change);
        messagingTemplate.convertAndSend("/topic/route-changes/" + routeId, response);
        return response;
    }

    @Transactional
    public void cancel(Long routeId, LocalDate date) {
        RouteServiceChange change = changeRepository.findByRouteIdAndEffectiveDateAndActiveTrue(routeId, date)
                .orElseThrow(() -> new IllegalArgumentException("No active route change was found for that date."));
        change.setActive(false);
        if (date.equals(LocalDate.now()) && change.getAffectedBus() != null
                && change.getAffectedBus().getStatus() == BusStatus.BREAKDOWN) {
            change.getAffectedBus().setStatus(change.getPreviousAffectedBusStatus() == null
                    ? BusStatus.RUNNING : change.getPreviousAffectedBusStatus());
            busRepository.save(change.getAffectedBus());
        }
        changeRepository.save(change);
        String message = "The temporary route update for " + change.getRoute().getName() + " on " + date + " has ended.";
        for (Bus bus : busRepository.findByRoute(change.getRoute())) {
            notificationService.notifyBusSubscribers(bus, "Route service restored", message, NotificationType.ROUTE_CHANGE);
        }
        messagingTemplate.convertAndSend("/topic/route-changes/" + routeId, "");
    }

    private RouteServiceChangeResponse toResponse(RouteServiceChange change) {
        List<RouteServiceChangeResponse.Stop> stops = change.getStops().stream()
                .map(stop -> new RouteServiceChangeResponse.Stop(
                        stop.getName(), stop.getLatitude(), stop.getLongitude(), stop.getStopOrder()))
                .toList();
        return new RouteServiceChangeResponse(
                change.getId(), change.getRoute().getId(), change.getRoute().getName(),
                change.getEffectiveDate(), change.getReason(),
                change.getAffectedBus() == null ? null : change.getAffectedBus().getId(),
                change.getAffectedBus() == null ? null : change.getAffectedBus().getBusNumber(),
                change.getReplacementBus() == null ? null : change.getReplacementBus().getId(),
                change.getReplacementBus() == null ? null : change.getReplacementBus().getBusNumber(),
                stops);
    }
}
