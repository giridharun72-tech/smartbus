package com.smartbus.controller;

import com.smartbus.dto.*;
import com.smartbus.service.SupportChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/support-chat")
public class SupportChatController {
    private final SupportChatService chatService;

    public SupportChatController(SupportChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversation")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SupportThreadResponse> getPassengerThread(Authentication authentication) {
        return ResponseEntity.ok(chatService.passengerThread(authentication.getName()));
    }

    @PostMapping("/conversation/messages")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SupportThreadResponse> sendPassengerMessage(
            @Valid @RequestBody SupportMessageRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(chatService.sendPassengerMessage(authentication.getName(), request.getContent()));
    }

    @GetMapping("/staff/conversations")
    @PreAuthorize("hasAnyRole('ADMIN', 'BCU')")
    public ResponseEntity<List<SupportConversationResponse>> listConversations() {
        return ResponseEntity.ok(chatService.staffConversations());
    }

    @GetMapping("/staff/conversations/{conversationId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'BCU')")
    public ResponseEntity<SupportThreadResponse> getStaffThread(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.staffThread(conversationId));
    }

    @PostMapping("/staff/conversations/{conversationId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'BCU')")
    public ResponseEntity<SupportThreadResponse> sendStaffMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SupportMessageRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(chatService.sendStaffMessage(
                conversationId, authentication.getName(), request.getContent()));
    }
}
