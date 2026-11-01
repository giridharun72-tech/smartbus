package com.smartbus.repository;

import com.smartbus.entity.PassengerProfile;
import com.smartbus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PassengerProfileRepository extends JpaRepository<PassengerProfile, Long> {
    Optional<PassengerProfile> findByUser(User user);
    Optional<PassengerProfile> findByPassengerId(String passengerId);
    boolean existsByPassengerId(String passengerId);
}
