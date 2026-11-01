package com.smartbus.repository;

import com.smartbus.entity.Bus;
import com.smartbus.entity.FavoriteBus;
import com.smartbus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteBusRepository extends JpaRepository<FavoriteBus, Long> {
    List<FavoriteBus> findByUser(User user);
    Optional<FavoriteBus> findByUserAndBus(User user, Bus bus);
    boolean existsByUserAndBus(User user, Bus bus);
}
