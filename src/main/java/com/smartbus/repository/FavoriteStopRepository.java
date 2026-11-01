package com.smartbus.repository;

import com.smartbus.entity.BusStop;
import com.smartbus.entity.FavoriteStop;
import com.smartbus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteStopRepository extends JpaRepository<FavoriteStop, Long> {
    List<FavoriteStop> findByUser(User user);
    Optional<FavoriteStop> findByUserAndStop(User user, BusStop stop);
    boolean existsByUserAndStop(User user, BusStop stop);
}
