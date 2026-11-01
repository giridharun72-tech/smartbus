package com.smartbus.dto;

import com.smartbus.entity.Notification;
import com.smartbus.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationMessageDTO(
        Long id,
        String busNumber,
        String title,
        String message,
        NotificationType type,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationMessageDTO from(Notification notification) {
        return new NotificationMessageDTO(
                notification.getId(),
                notification.getBusNumber(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
