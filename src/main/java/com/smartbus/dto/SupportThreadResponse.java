package com.smartbus.dto;

import java.util.List;

public record SupportThreadResponse(Long conversationId, List<SupportChatMessageResponse> messages) {}
