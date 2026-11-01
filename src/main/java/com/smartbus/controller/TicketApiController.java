package com.smartbus.controller;

import com.smartbus.dto.TicketVerificationDTO;
import com.smartbus.dto.TicketVerificationRequest;
import com.smartbus.entity.Bus;
import com.smartbus.entity.Driver;
import com.smartbus.entity.User;
import com.smartbus.entity.Ticket;
import com.smartbus.repository.BusRepository;
import com.smartbus.repository.DriverRepository;
import com.smartbus.repository.TicketRepository;
import com.smartbus.repository.UserRepository;
import com.smartbus.service.PdfService;
import com.smartbus.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketApiController {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final PdfService pdfService;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final BusRepository busRepository;

    public TicketApiController(TicketRepository ticketRepository,
                               TicketService ticketService,
                               PdfService pdfService,
                               UserRepository userRepository,
                               DriverRepository driverRepository,
                               BusRepository busRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
        this.pdfService = pdfService;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.busRepository = busRepository;
    }

    @GetMapping("/{ticketNumber}")
    public ResponseEntity<Ticket> getTicket(@PathVariable String ticketNumber, Authentication auth) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .filter(ticket -> canAccessTicket(ticket, auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{ticketNumber}/pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable String ticketNumber, Authentication auth) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .filter(found -> canAccessTicket(found, auth))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Ticket not found"));

        byte[] pdfBytes = pdfService.generateTicketPdf(ticket);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Ticket_" + ticket.getTicketNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/verify")
    public ResponseEntity<TicketVerificationDTO> verifyTicket(@Valid @RequestBody TicketVerificationRequest req, Authentication auth) {
        boolean markAsUsed = req.isMarkAsUsed();
        String verifiedBy = auth != null ? auth.getName() : "CONDUCTOR";

        boolean isDriver = auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DRIVER"));
        TicketVerificationDTO res;
        if (isDriver) {
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            Driver driver = user == null ? null : driverRepository.findByUser(user).orElse(null);
            Bus bus = driver == null ? null : busRepository.findByDriver(driver).orElse(null);
            if (bus == null) return ResponseEntity.status(403).build();
            res = ticketService.verifyTicketForBus(req.getTicketNumber(), verifiedBy, markAsUsed, bus.getBusNumber());
        } else {
            res = ticketService.verifyTicket(req.getTicketNumber(), verifiedBy, markAsUsed);
        }
        return ResponseEntity.ok(res);
    }

    private boolean canAccessTicket(Ticket ticket, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean privileged = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_BCU"));
        return privileged || (ticket.getBooking() != null
                && ticket.getBooking().getUser() != null
                && ticket.getBooking().getUser().getUsername().equals(auth.getName()));
    }
}
