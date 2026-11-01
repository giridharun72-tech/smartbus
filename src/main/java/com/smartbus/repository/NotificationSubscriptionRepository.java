package com.smartbus.repository;

import com.smartbus.entity.Bus;
import com.smartbus.entity.NotificationSubscription;
import com.smartbus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {
    List<NotificationSubscription> findByBusAndNotifiedForCurrentTripFalse(Bus bus);
    List<NotificationSubscription> findByUser(User user);
    boolean existsByUserAndBus(User user, Bus bus);
}
