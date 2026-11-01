package com.smartbus.service;

import com.smartbus.dto.*;
import com.smartbus.entity.SupportChatMessage;
import com.smartbus.entity.SupportConversation;
import com.smartbus.entity.User;
import com.smartbus.repository.SupportChatMessageRepository;
import com.smartbus.repository.SupportConversationRepository;
import com.smartbus.repository.PassengerProfileRepository;
import com.smartbus.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupportChatService {
    private final SupportConversationRepository conversationRepository;
    private final SupportChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PassengerProfileRepository profileRepository;

    public SupportChatService(SupportConversationRepository conversationRepository,
                              SupportChatMessageRepository messageRepository,
                              UserRepository userRepository,
                              PassengerProfileRepository profileRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public SupportThreadResponse passengerThread(String username) {
        User passenger = requireUser(username);
        return conversationRepository.findFirstByPassengerOrderByUpdatedAtDesc(passenger)
                .map(conversation -> {
                    markPassengerMessagesRead(conversation);
                    return thread(conversation);
                })
                .orElseGet(() -> new SupportThreadResponse(null, List.of()));
    }

    @Transactional
    public SupportThreadResponse sendPassengerMessage(String username, String content) {
        User passenger = requireUser(username);
        SupportConversation conversation = conversationRepository.findFirstByPassengerOrderByUpdatedAtDesc(passenger)
                .orElseGet(() -> {
                    SupportConversation created = new SupportConversation();
                    created.setPassenger(passenger);
                    return conversationRepository.save(created);
                });
        saveMessage(conversation, passenger, false, content);
        return thread(conversation);
    }

    @Transactional(readOnly = true)
    public List<SupportConversationResponse> staffConversations() {
        return conversationRepository.findAllByOrderByUpdatedAtDesc(PageRequest.of(0, 100)).stream()
                .map(conversation -> {
                    String latest = messageRepository
                            .findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                            .map(SupportChatMessage::getContent)
                            .orElse("");
                    long unread = messageRepository.countByConversationIdAndSenderIsStaffFalseAndReadByStaffFalse(conversation.getId());
                    var profile = profileRepository.findByUser(conversation.getPassenger()).orElse(null);
                    String passengerName = profile == null ? conversation.getPassenger().getUsername() : profile.getFullName();
                    String passengerId = profile == null ? "" : profile.getPassengerId();
                    return new SupportConversationResponse(conversation.getId(),
                            conversation.getPassenger().getUsername(), passengerName, passengerId,
                            conversation.getUpdatedAt(), unread, latest);
                })
                .toList();
    }

    @Transactional
    public SupportThreadResponse staffThread(Long conversationId) {
        SupportConversation conversation = requireConversation(conversationId);
        List<SupportChatMessage> unreadMessages =
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                        .filter(message -> !message.isSenderIsStaff() && !message.isReadByStaff())
                        .toList();
        if (!unreadMessages.isEmpty()) {
            unreadMessages.forEach(message -> message.setReadByStaff(true));
            messageRepository.saveAll(unreadMessages);
        }
        return thread(conversation);
    }

    @Transactional
    public SupportThreadResponse sendStaffMessage(Long conversationId, String username, String content) {
        SupportConversation conversation = requireConversation(conversationId);
        User staff = requireUser(username);
        saveMessage(conversation, staff, true, content);
        return thread(conversation);
    }

    private SupportThreadResponse thread(SupportConversation conversation) {
        List<SupportChatMessageResponse> messages =
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                        .map(message -> new SupportChatMessageResponse(
                                message.getId(), message.getSender().getUsername(),
                                message.isSenderIsStaff(), message.getContent(), message.getCreatedAt()))
                        .toList();
        return new SupportThreadResponse(conversation.getId(), messages);
    }

    private void saveMessage(SupportConversation conversation, User sender, boolean staff, String content) {
        SupportChatMessage message = new SupportChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setSenderIsStaff(staff);
        message.setContent(content.trim());
        message.setReadByStaff(staff);
        message.setReadByPassenger(!staff);
        messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private void markPassengerMessagesRead(SupportConversation conversation) {
        List<SupportChatMessage> unreadMessages =
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                        .filter(message -> message.isSenderIsStaff() && !message.isReadByPassenger())
                        .toList();
        if (!unreadMessages.isEmpty()) {
            unreadMessages.forEach(message -> message.setReadByPassenger(true));
            messageRepository.saveAll(unreadMessages);
        }
    }

    private SupportConversation requireConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found."));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not found."));
    }
}
