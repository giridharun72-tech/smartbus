package com.smartbus.controller;

import com.smartbus.dto.BookingRequestDTO;
import com.smartbus.entity.Seat;
import com.smartbus.entity.Ticket;
import com.smartbus.entity.User;
import com.smartbus.repository.UserRepository;
import com.smartbus.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingApiController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingApiController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO req, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Please login to complete booking"));
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            Ticket ticket = bookingService.createBookingAndTicket(user, req);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "ticketNumber", ticket.getTicketNumber(),
                    "passengerId", ticket.getPassengerId(),
                    "fare", ticket.getFare(),
                    "status", ticket.getVerificationStatus().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/seats")
    public ResponseEntity<List<Seat>> getSeats(
            @RequestParam Long busId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getSeatAvailability(busId, date));
    }
}
