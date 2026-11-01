package com.smartbus.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smartbus.dto.TicketVerificationDTO;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Year;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AuditLogRepository auditLogRepository;
    private final PaymentRepository paymentRepository;
    private final RouteServiceChangeRepository routeServiceChangeRepository;

    public TicketService(TicketRepository ticketRepository,
                         AuditLogRepository auditLogRepository,
                         PaymentRepository paymentRepository,
                         RouteServiceChangeRepository routeServiceChangeRepository) {
        this.ticketRepository = ticketRepository;
        this.auditLogRepository = auditLogRepository;
        this.paymentRepository = paymentRepository;
        this.routeServiceChangeRepository = routeServiceChangeRepository;
    }

    public String generateUniqueTicketNumber() {
        int year = Year.now().getValue();
        Random random = new Random();
        String number;
        do {
            int seq = 100000 + random.nextInt(900000);
            number = "TKT-" + year + "-" + seq;
        } while (ticketRepository.findByTicketNumber(number).isPresent());
        return number;
    }

    public String generateQrCodeBase64(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 250, 250);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    @Transactional
    public TicketVerificationDTO verifyTicket(String ticketNumber, String verifiedBy, boolean markAsUsed) {
        return verifyTicket(ticketNumber, verifiedBy, markAsUsed, null);
    }

    @Transactional
    public TicketVerificationDTO verifyTicketForBus(String ticketNumber, String verifiedBy, boolean markAsUsed, String busNumber) {
        return verifyTicket(ticketNumber, verifiedBy, markAsUsed, busNumber);
    }

    private TicketVerificationDTO verifyTicket(String ticketNumber, String verifiedBy, boolean markAsUsed, String expectedBusNumber) {
        TicketVerificationDTO dto = new TicketVerificationDTO();
        String normalizedTicketNumber = extractTicketNumber(ticketNumber);
        dto.setTicketNumber(normalizedTicketNumber);

        Optional<Ticket> opt = ticketRepository.findByTicketNumberForUpdate(normalizedTicketNumber);
        if (opt.isEmpty()) {
            dto.setValid(false);
            dto.setMessage("INVALID TICKET: Ticket number not found in the system.");
            dto.setStatus("NOT_FOUND");
            return dto;
        }

        Ticket ticket = opt.get();
        boolean assignedToBus = expectedBusNumber == null
                || ticket.getBusNumber().equalsIgnoreCase(expectedBusNumber)
                || (ticket.getBooking().getBus() != null
                    && ticket.getBooking().getBus().getBusNumber().equalsIgnoreCase(expectedBusNumber))
                || routeServiceChangeRepository.existsByRouteIdAndEffectiveDateAndActiveTrueAndReplacementBus_BusNumberIgnoreCase(
                    ticket.getBooking().getRoute().getId(), ticket.getTravelDate(), expectedBusNumber);
        if (!assignedToBus) {
            dto.setValid(false);
            dto.setStatus("NOT_ASSIGNED");
            dto.setMessage("INVALID TICKET: This ticket is not assigned to your bus or its approved replacement.");
            return dto;
        }

        dto.setPassengerName(ticket.getPassengerName());
        dto.setPassengerId(ticket.getPassengerId());
        dto.setBusNumber(ticket.getBusNumber());
        dto.setRouteName(ticket.getRouteName());
        dto.setFromStop(ticket.getFromStop());
        dto.setToStop(ticket.getToStop());
        dto.setTravelDate(ticket.getTravelDate() != null ? ticket.getTravelDate().toString() : "");
        dto.setDepartureTime(ticket.getDepartureTime() != null ? ticket.getDepartureTime().toString() : "");
        dto.setSeatNumber(ticket.getSeatNumber());
        dto.setFare(ticket.getFare());
        dto.setStatus(ticket.getVerificationStatus().name());
        Payment payment = ticket.getBooking() == null ? null : paymentRepository.findByBooking(ticket.getBooking()).orElse(null);
        dto.setPaymentStatus(payment == null ? "NOT PAID" : payment.getStatus().name());

        if (ticket.getVerificationStatus() == TicketStatus.CANCELLED) {
            dto.setValid(false);
            dto.setMessage("INVALID TICKET: This ticket has been CANCELLED.");
            return dto;
        }

        if (ticket.getVerificationStatus() == TicketStatus.USED) {
            dto.setValid(false);
            dto.setMessage("ALREADY USED: Ticket was already verified and used on " + ticket.getVerifiedAt() + " by " + ticket.getVerifiedBy() + ".");
            return dto;
        }

        if (ticket.getBooking() == null || ticket.getBooking().getStatus() != BookingStatus.CONFIRMED) {
            dto.setValid(false);
            dto.setMessage("INVALID TICKET: The booking is not confirmed.");
            return dto;
        }

        if (payment == null || payment.getStatus() != PaymentStatus.SUCCESS) {
            dto.setValid(false);
            dto.setMessage("INVALID TICKET: Payment has not been completed.");
            return dto;
        }

        if (ticket.getTravelDate() == null || !ticket.getTravelDate().equals(LocalDate.now())) {
            dto.setValid(false);
            dto.setMessage("INVALID TICKET: This ticket is not valid for travel today.");
            return dto;
        }

        if (markAsUsed) {
            ticket.setVerificationStatus(TicketStatus.USED);
            ticket.setVerifiedAt(LocalDateTime.now());
            ticket.setVerifiedBy(verifiedBy != null ? verifiedBy : "CONDUCTOR");
            ticketRepository.save(ticket);

            auditLogRepository.save(new AuditLog(
                    verifiedBy != null ? verifiedBy : "CONDUCTOR",
                    "STAFF",
                    "TICKET_VERIFIED",
                    "Ticket " + normalizedTicketNumber + " marked as USED for passenger " + ticket.getPassengerName(),
                    "127.0.0.1"
            ));

            dto.setStatus("USED");
            dto.setValid(true);
            dto.setMessage("VALID TICKET: Ticket verified successfully and marked as USED.");
        } else {
            dto.setValid(true);
            dto.setMessage("VALID TICKET: Confirmed active booking.");
        }

        return dto;
    }

    private String extractTicketNumber(String input) {
        if (input == null) return "";
        String scannedValue = input.trim();
        if (!scannedValue.startsWith("SMARTBUS|")) return scannedValue;
        for (String part : scannedValue.split("\\|")) {
            if (part.startsWith("TICKET:")) {
                return part.substring("TICKET:".length()).trim();
            }
        }
        return "";
    }
}
