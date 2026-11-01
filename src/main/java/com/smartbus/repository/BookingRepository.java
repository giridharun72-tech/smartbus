package com.smartbus.repository;

import com.smartbus.entity.Booking;
import com.smartbus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
    Optional<Booking> findByBookingReference(String bookingReference);
    List<Booking> findByTravelDate(LocalDate travelDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.travelDate = :date AND b.status = 'CONFIRMED'")
    Long countConfirmedBookingsOnDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.travelDate = :date AND b.status = 'CONFIRMED'")
    Double calculateRevenueOnDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.travelDate BETWEEN :start AND :end AND b.status = 'CONFIRMED'")
    Double calculateRevenueBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT b.bus.busNumber, COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.status = 'CONFIRMED' GROUP BY b.bus.busNumber")
    List<Object[]> findBusWiseEarnings();

    @Query("SELECT b.route.name, COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.status = 'CONFIRMED' GROUP BY b.route.name")
    List<Object[]> findRouteWiseEarnings();
}
