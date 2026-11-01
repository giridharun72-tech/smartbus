package com.smartbus.service;

import com.smartbus.dto.NotificationMessageDTO;
import com.smartbus.entity.Notification;
import com.smartbus.entity.NotificationType;
import com.smartbus.entity.Role;
import com.smartbus.entity.User;
import com.smartbus.repository.NotificationRepository;
import com.smartbus.repository.NotificationSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void sendsPersonalNotificationsOnlyToTheUsersQueueAsMinimalDtos() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationSubscriptionRepository subscriptionRepository = mock(NotificationSubscriptionRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        NotificationService service = new NotificationService(
                notificationRepository, subscriptionRepository, messagingTemplate);
        User user = new User("passenger", "private@example.test", "hashed", Role.ROLE_USER);
        Notification notification = new Notification(user, "21A", "Bus update", "Arriving soon",
                NotificationType.PROXIMITY_ALERT);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        service.sendNotification(user, "21A", "Bus update", "Arriving soon",
                NotificationType.PROXIMITY_ALERT);

        verify(messagingTemplate).convertAndSendToUser(
                eq("passenger"), eq("/queue/notifications"), any(NotificationMessageDTO.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/notifications"), any(NotificationMessageDTO.class));
    }

    @Test
    void publicBroadcastsUseTheMinimalNotificationDto() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationSubscriptionRepository subscriptionRepository = mock(NotificationSubscriptionRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        NotificationService service = new NotificationService(
                notificationRepository, subscriptionRepository, messagingTemplate);
        Notification notification = new Notification(null, "21A", "Service update", "Diversion active",
                NotificationType.ROUTE_CHANGE);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        service.sendNotification(null, "21A", "Service update", "Diversion active",
                NotificationType.ROUTE_CHANGE);

        org.mockito.ArgumentCaptor<Object> payload = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications"), payload.capture());
        assertThat(payload.getValue()).isInstanceOf(NotificationMessageDTO.class);
    }
}
