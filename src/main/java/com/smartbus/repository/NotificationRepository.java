package com.smartbus.repository;

import com.smartbus.entity.Notification;
import com.smartbus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserIsNullOrderByCreatedAtDesc();
}
