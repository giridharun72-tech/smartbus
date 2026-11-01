package com.smartbus.service;

import com.smartbus.dto.BookingRequestDTO;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final AuditLogRepository auditLogRepository;

    public BookingService(BookingRepository bookingRepository,
                          SeatRepository seatRepository,
                          BusRepository busRepository,
                          RouteRepository routeRepository,
                          PassengerProfileRepository passengerProfileRepository,
                          PaymentService paymentService,
                          TicketService ticketService,
                          TicketRepository ticketRepository,
                          AuditLogRepository auditLogRepository) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.passengerProfileRepository = passengerProfileRepository;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public synchronized Ticket createBookingAndTicket(User user, BookingRequestDTO req) {
        Bus bus = busRepository.findById(req.getBusId())
                .orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        Route route = routeRepository.findById(req.getRouteId())
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));

        LocalDate date = (req.getTravelDate() != null) ? req.getTravelDate() : LocalDate.now();
        List<String> seatsRequested = req.getSeatNumbers() != null && !req.getSeatNumbers().isEmpty()
                ? req.getSeatNumbers() : List.of("A1");

        // Double booking prevention with database lock / verification
        for (String seatNum : seatsRequested) {
            Optional<Seat> existing = seatRepository.findByBusAndSeatNumberAndTravelDate(bus, seatNum, date);
            if (existing.isPresent() && existing.get().getStatus() == SeatStatus.BOOKED) {
                throw new IllegalStateException("Seat " + seatNum + " is already booked for this bus and date.");
            }
        }

        // Calculate Fare (base 25 or calculated from route)
        double farePerSeat = 25.0;
        double totalFare = farePerSeat * seatsRequested.size();

        String bookingRef = "BK-" + Year.now().getValue() + "-" + (100000 + new Random().nextInt(900000));
        String seatNumbersStr = String.join(", ", seatsRequested);

        Booking booking = new Booking(
                bookingRef,
                user,
                bus,
                route,
                req.getFromStop() != null ? req.getFromStop() : route.getSource(),
                req.getToStop() != null ? req.getToStop() : route.getDestination(),
                date,
                LocalTime.of(11, 15),
                seatNumbersStr,
                seatsRequested.size(),
                totalFare
        );
        Booking savedBooking = bookingRepository.save(booking);

        // Mark seats as BOOKED
        for (String seatNum : seatsRequested) {
            Optional<Seat> optSeat = seatRepository.findByBusAndSeatNumberAndTravelDate(bus, seatNum, date);
            Seat seat;
            if (optSeat.isPresent()) {
                seat = optSeat.get();
                seat.setStatus(SeatStatus.BOOKED);
                seat.setBooking(savedBooking);
            } else {
                seat = new Seat(bus, seatNum, date, savedBooking, SeatStatus.BOOKED);
            }
            seatRepository.save(seat);
        }

        // Process Mock Sandbox Payment
        Payment payment = paymentService.processSandboxPayment(savedBooking, totalFare, "MOCK_UPI_GATEWAY");

        // Find Passenger ID
        Optional<PassengerProfile> profileOpt = passengerProfileRepository.findByUser(user);
        String passengerId = profileOpt.map(PassengerProfile::getPassengerId).orElse("PB" + Year.now().getValue() + "999999");
        String passengerName = profileOpt.map(PassengerProfile::getFullName).orElse(user.getUsername());

        // Generate Ticket
        String ticketNumber = ticketService.generateUniqueTicketNumber();
        String qrData = "SMARTBUS|TICKET:" + ticketNumber + "|PASSENGER:" + passengerId + "|BUS:" + bus.getBusNumber() + "|SEAT:" + seatNumbersStr + "|DATE:" + date;
        String qrBase64 = ticketService.generateQrCodeBase64(qrData);

        Ticket ticket = new Ticket(
                ticketNumber,
                savedBooking,
                passengerName,
                passengerId,
                bus.getBusNumber(),
                route.getName() + " (" + route.getRouteNumber() + ")",
                booking.getFromStop(),
                booking.getToStop(),
                date,
                booking.getDepartureTime(),
                seatNumbersStr,
                totalFare,
                qrBase64
        );
        Ticket savedTicket = ticketRepository.save(ticket);

        auditLogRepository.save(new AuditLog(
                user.getUsername(),
                user.getRole().name(),
                "BOOKING_CREATED",
                "Booking reference " + bookingRef + " confirmed, Ticket Number " + ticketNumber,
                "127.0.0.1"
        ));

        return savedTicket;
    }

    @Transactional(readOnly = true)
    public List<Seat> getSeatAvailability(Long busId, LocalDate travelDate) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        LocalDate date = (travelDate != null) ? travelDate : LocalDate.now();
        return seatRepository.findByBusAndTravelDate(bus, date);
    }
}
