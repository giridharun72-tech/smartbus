package com.smartbus.dto;

import java.time.LocalDateTime;

public record SupportConversationResponse(
        Long id,
        String passengerUsername,
        String passengerName,
        String passengerId,
        LocalDateTime updatedAt,
        long unreadCount,
        String latestMessage) {}
