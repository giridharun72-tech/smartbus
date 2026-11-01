package com.smartbus.service;

import com.smartbus.entity.*;
import com.smartbus.repository.*;
import com.smartbus.dto.NotificationMessageDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationSubscriptionRepository subscriptionRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Notification sendNotification(User user, String busNumber, String title, String message, NotificationType type) {
        Notification notification = new Notification(user, busNumber, title, message, type);
        Notification saved = notificationRepository.save(notification);

        NotificationMessageDTO event = NotificationMessageDTO.from(saved);
        if (user == null) {
            messagingTemplate.convertAndSend("/topic/notifications", event);
        } else {
            messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", event);
        }

        return saved;
    }

    @Transactional
    public void notifyBusSubscribers(Bus bus, String title, String message, NotificationType type) {
        List<NotificationSubscription> subs = subscriptionRepository.findByBusAndNotifiedForCurrentTripFalse(bus);
        for (NotificationSubscription sub : subs) {
            sendNotification(sub.getUser(), bus.getBusNumber(), title, message, type);
        }
        // Also send general broadcast notification
        sendNotification(null, bus.getBusNumber(), title, message, type);
    }

    @Transactional
    public void checkProximityAlerts(Bus bus, double busLat, double busLon) {
        if (bus == null || bus.getRoute() == null) return;

        List<NotificationSubscription> subs = subscriptionRepository.findByBusAndNotifiedForCurrentTripFalse(bus);
        for (NotificationSubscription sub : subs) {
            if (sub.getStopDistanceMeters() == null || sub.getTargetStopName() == null) continue;

            // Find target stop in route
            for (RouteStop rs : bus.getRoute().getRouteStops()) {
                if (rs.getStop().getName().equalsIgnoreCase(sub.getTargetStopName())) {
                    double distKm = TrackingService.calculateDistanceKm(busLat, busLon, rs.getStop().getLatitude(), rs.getStop().getLongitude());
                    double distMeters = distKm * 1000;

                    if (distMeters <= sub.getStopDistanceMeters()) {
                        String distText = TrackingService.formatDistance(distKm);
                        String msg = "Bus " + bus.getBusNumber() + " is approximately " + distText + " from " + sub.getTargetStopName() + ".";
                        sendNotification(sub.getUser(), bus.getBusNumber(), "Bus Approaching Stop", msg, NotificationType.PROXIMITY_ALERT);

                        sub.setNotifiedForCurrentTrip(true);
                        subscriptionRepository.save(sub);
                    }
                    break;
                }
            }
        }
    }

    @Transactional
    public NotificationSubscription subscribe(User user, Bus bus, String alertType, Integer distanceMeters, String targetStop) {
        NotificationSubscription sub = new NotificationSubscription(user, bus, alertType != null ? alertType : "ALL", distanceMeters, targetStop);
        return subscriptionRepository.save(sub);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
