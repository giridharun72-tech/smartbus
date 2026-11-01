package com.smartbus.repository;

import com.smartbus.entity.SupportConversation;
import com.smartbus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SupportConversationRepository extends JpaRepository<SupportConversation, Long> {
    Optional<SupportConversation> findFirstByPassengerOrderByUpdatedAtDesc(User passenger);
    List<SupportConversation> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
