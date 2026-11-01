package com.smartbus.repository;

import com.smartbus.entity.Bus;
import com.smartbus.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBusAndTravelDate(Bus bus, LocalDate travelDate);
    Optional<Seat> findByBusAndSeatNumberAndTravelDate(Bus bus, String seatNumber, LocalDate travelDate);
}
