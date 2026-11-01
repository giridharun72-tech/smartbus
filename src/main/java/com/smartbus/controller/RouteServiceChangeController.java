package com.smartbus.controller;

import com.smartbus.dto.RouteServiceChangeRequest;
import com.smartbus.dto.RouteServiceChangeResponse;
import com.smartbus.repository.RouteRepository;
import com.smartbus.repository.RouteStopRepository;
import com.smartbus.service.RouteServiceChangeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class RouteServiceChangeController {
    private final RouteServiceChangeService changeService;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    public RouteServiceChangeController(RouteServiceChangeService changeService,
                                        RouteRepository routeRepository,
                                        RouteStopRepository routeStopRepository) {
        this.changeService = changeService;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
    }

    @GetMapping("/api/routes/{routeId}/service-change")
    public ResponseEntity<RouteServiceChangeResponse> getChange(
            @PathVariable Long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!routeRepository.existsById(routeId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ofNullable(changeService.findActive(routeId, date));
    }

    @GetMapping("/api/routes/{routeId}/stops")
    public ResponseEntity<?> getRouteStops(@PathVariable Long routeId) {
        return routeRepository.findById(routeId)
                .map(route -> ResponseEntity.ok(routeStopRepository.findByRouteOrderByStopOrderAsc(route).stream()
                        .map(stop -> new RouteServiceChangeResponse.Stop(
                                stop.getStop().getName(), stop.getStop().getLatitude(),
                                stop.getStop().getLongitude(), stop.getStopOrder()))
                        .toList()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/bcu/routes/{routeId}/service-change")
    public ResponseEntity<?> saveChange(
            @PathVariable Long routeId,
            @Valid @RequestBody RouteServiceChangeRequest request) {
        try {
            return ResponseEntity.ok(changeService.save(routeId, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/api/bcu/routes/{routeId}/service-change")
    public ResponseEntity<Void> cancelChange(
            @PathVariable Long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            changeService.cancel(routeId, date);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
