package com.smartbus.controller;

import com.smartbus.entity.*;
import com.smartbus.repository.*;
import com.smartbus.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final BusRepository busRepository;

    public NotificationApiController(NotificationService notificationService,
                                     UserRepository userRepository,
                                     BusRepository busRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.busRepository = busRepository;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(List.of());
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(notificationService.getUserNotifications(user));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Object> req, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Please login to subscribe"));
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Long busId = Long.valueOf(req.get("busId").toString());
        Bus bus = busRepository.findById(busId).orElseThrow();

        String alertType = (String) req.getOrDefault("alertType", "ALL");
        Integer distMeters = req.get("distanceMeters") != null ? Integer.valueOf(req.get("distanceMeters").toString()) : null;
        String targetStop = (String) req.get("targetStop");

        notificationService.subscribe(user, bus, alertType, distMeters, targetStop);
        return ResponseEntity.ok(Map.of("success", true, "message", "Subscribed to notifications for Bus " + bus.getBusNumber()));
    }
}
