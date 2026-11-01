package com.smartbus.service;

import com.smartbus.dto.TicketVerificationDTO;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock private TicketRepository ticketRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RouteServiceChangeRepository routeServiceChangeRepository;

    private TicketService ticketService;
    private Ticket ticket;
    private Payment payment;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, auditLogRepository, paymentRepository, routeServiceChangeRepository);

        Route route = new Route();
        route.setId(5L);
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setRoute(route);

        ticket = new Ticket();
        ticket.setTicketNumber("TKT-2026-123456");
        ticket.setBooking(booking);
        ticket.setPassengerName("Test Passenger");
        ticket.setPassengerId("PB2026001001");
        ticket.setBusNumber("21A");
        ticket.setRouteName("Route 5");
        ticket.setFromStop("Start");
        ticket.setToStop("End");
        ticket.setTravelDate(LocalDate.now());
        ticket.setSeatNumber("A1");
        ticket.setFare(25.0);
        ticket.setVerificationStatus(TicketStatus.VALID);

        payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.SUCCESS);

        when(ticketRepository.findByTicketNumberForUpdate("TKT-2026-123456")).thenReturn(Optional.of(ticket));
        lenient().when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
    }

    @Test
    void acceptsQrPayloadForAssignedBusAndMarksBoardingUsed() {
        String qrPayload = "SMARTBUS|TICKET:TKT-2026-123456|PASSENGER:PB2026001001|BUS:21A|SEAT:A1";

        TicketVerificationDTO result = ticketService.verifyTicketForBus(qrPayload, "driver21a", true, "21A");

        assertTrue(result.isValid());
        assertEquals("TKT-2026-123456", result.getTicketNumber());
        assertEquals("USED", result.getStatus());
        assertEquals("SUCCESS", result.getPaymentStatus());
        verify(ticketRepository).save(ticket);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void rejectsTicketForAnotherBusWithoutMarkingItUsed() {
        TicketVerificationDTO result = ticketService.verifyTicketForBus("TKT-2026-123456", "driver43b", true, "43B");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("not assigned to your bus"));
        assertNull(result.getPassengerName());
        assertEquals(TicketStatus.VALID, ticket.getVerificationStatus());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void acceptsPrimaryBusTicketForApprovedReplacementBus() {
        when(routeServiceChangeRepository
                .existsByRouteIdAndEffectiveDateAndActiveTrueAndReplacementBus_BusNumberIgnoreCase(5L, LocalDate.now(), "43B"))
                .thenReturn(true);

        TicketVerificationDTO result = ticketService.verifyTicketForBus("TKT-2026-123456", "driver43b", false, "43B");

        assertTrue(result.isValid());
        assertEquals("21A", result.getBusNumber());
    }

    @Test
    void rejectsTicketsNotValidForToday() {
        ticket.setTravelDate(LocalDate.now().minusDays(1));

        TicketVerificationDTO result = ticketService.verifyTicket("TKT-2026-123456", "bcu", false);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("not valid for travel today"));
    }

    @Test
    void reportsPreviouslyUsedTicketEvenAfterTravelDate() {
        ticket.setTravelDate(LocalDate.now().minusDays(1));
        ticket.setVerificationStatus(TicketStatus.USED);
        ticket.setVerifiedBy("driver21a");

        TicketVerificationDTO result = ticketService.verifyTicket("TKT-2026-123456", "driver21a", false);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("ALREADY USED"));
    }
}
