package com.smartbus.dto;

import java.time.LocalDateTime;

public record SupportChatMessageResponse(
        Long id,
        String sender,
        boolean staff,
        String content,
        LocalDateTime createdAt) {}
