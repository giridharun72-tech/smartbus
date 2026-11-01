package com.smartbus.repository;

import com.smartbus.entity.Booking;
import com.smartbus.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketNumber(String ticketNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.ticketNumber = :ticketNumber")
    Optional<Ticket> findByTicketNumberForUpdate(@Param("ticketNumber") String ticketNumber);
    Optional<Ticket> findByBooking(Booking booking);
    List<Ticket> findByPassengerIdOrderByTravelDateDesc(String passengerId);
}
