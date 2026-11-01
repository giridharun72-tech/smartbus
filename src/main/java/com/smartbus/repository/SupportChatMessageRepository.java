package com.smartbus.repository;

import com.smartbus.entity.SupportChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportChatMessageRepository extends JpaRepository<SupportChatMessage, Long> {
    List<SupportChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    Optional<SupportChatMessage> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
    long countByConversationIdAndSenderIsStaffFalseAndReadByStaffFalse(Long conversationId);
    long countByConversationIdAndSenderIsStaffTrueAndReadByPassengerFalse(Long conversationId);
}
