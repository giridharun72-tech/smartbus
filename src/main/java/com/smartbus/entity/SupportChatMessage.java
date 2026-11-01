package com.smartbus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_chat_messages", indexes = {
        @Index(name = "idx_support_message_conversation_time", columnList = "conversation_id, created_at")
})
public class SupportChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private SupportConversation conversation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "sender_is_staff", nullable = false)
    private boolean senderIsStaff;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_by_staff", nullable = false)
    private boolean readByStaff;

    @Column(name = "read_by_passenger", nullable = false)
    private boolean readByPassenger;

    public Long getId() { return id; }
    public SupportConversation getConversation() { return conversation; }
    public void setConversation(SupportConversation conversation) { this.conversation = conversation; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public boolean isSenderIsStaff() { return senderIsStaff; }
    public void setSenderIsStaff(boolean senderIsStaff) { this.senderIsStaff = senderIsStaff; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isReadByStaff() { return readByStaff; }
    public void setReadByStaff(boolean readByStaff) { this.readByStaff = readByStaff; }
    public boolean isReadByPassenger() { return readByPassenger; }
    public void setReadByPassenger(boolean readByPassenger) { this.readByPassenger = readByPassenger; }
}
