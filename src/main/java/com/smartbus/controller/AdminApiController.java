package com.smartbus.controller;

import com.smartbus.dto.EarningSummaryDTO;
import com.smartbus.dto.AdminBusResponse;
import com.smartbus.dto.CreateBusRequest;
import com.smartbus.dto.PhoneGpsLocationRequest;
import com.smartbus.entity.Bus;
import com.smartbus.entity.BusStatus;
import com.smartbus.repository.*;
import com.smartbus.service.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminApiController {

    private final BusService busService;
    private final BusRepository busRepository;
    private final BookingRepository bookingRepository;
    private final PdfService pdfService;
    private final AdminBusService adminBusService;

    public AdminApiController(BusService busService,
                              BusRepository busRepository,
                              BookingRepository bookingRepository,
                              PdfService pdfService,
                              AdminBusService adminBusService) {
        this.busService = busService;
        this.busRepository = busRepository;
        this.bookingRepository = bookingRepository;
        this.pdfService = pdfService;
        this.adminBusService = adminBusService;
    }

    @PostMapping("/buses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addBus(@Valid @RequestBody CreateBusRequest request) {
        try {
            AdminBusResponse createdBus = adminBusService.addBus(request);
            return ResponseEntity.status(201).body(createdBus);
        } catch (BusAlreadyExistsException ex) {
            return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            if (busRepository.findFirstByBusNumberIgnoreCase(request.getBusNumber().trim()).isPresent()) {
                return ResponseEntity.status(409)
                        .body(Map.of("message", "A bus with number '" + request.getBusNumber().trim() + "' already exists."));
            }
            throw ex;
        }
    }

    @PostMapping("/buses/{id}/location")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePhoneGpsLocation(@PathVariable Long id,
                                                    @Valid @RequestBody PhoneGpsLocationRequest request) {
        try {
            return ResponseEntity.ok(adminBusService.updatePhoneLocation(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/earnings")
    public ResponseEntity<Map<String, Object>> getEarnings() {
        LocalDate today = LocalDate.now();
        double todayRev = bookingRepository.calculateRevenueOnDate(today);
        double weekRev = bookingRepository.calculateRevenueBetween(today.minusDays(7), today);
        double monthRev = bookingRepository.calculateRevenueBetween(today.minusDays(30), today);
        double totalRev = bookingRepository.findAll().stream().mapToDouble(b -> b.getTotalFare() != null ? b.getTotalFare() : 0.0).sum();

        List<Object[]> busEarnings = bookingRepository.findBusWiseEarnings();
        List<Object[]> routeEarnings = bookingRepository.findRouteWiseEarnings();

        Map<String, Object> res = new HashMap<>();
        res.put("todayRevenue", todayRev);
        res.put("weeklyRevenue", weekRev);
        res.put("monthlyRevenue", monthRev);
        res.put("totalRevenue", totalRev);
        res.put("busEarnings", busEarnings);
        res.put("routeEarnings", routeEarnings);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/buses/{id}/status")
    public ResponseEntity<?> updateBusStatus(@PathVariable Long id,
                                             @RequestBody Map<String, String> payload,
                                             Authentication auth) {
        String statusStr = payload.get("status");
        BusStatus newStatus = BusStatus.valueOf(statusStr);
        String username = auth != null ? auth.getName() : "ADMIN";

        Bus updated = busService.updateBusStatus(id, newStatus, username);
        return ResponseEntity.ok(Map.of("success", true, "busNumber", updated.getBusNumber(), "status", updated.getStatus().name()));
    }

    @GetMapping("/reports/revenue-pdf")
    public ResponseEntity<byte[]> downloadRevenuePdf(
            @RequestParam(defaultValue = "Daily Revenue Report") String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        byte[] pdfBytes = pdfService.generateRevenueReportPdf(reportType, date != null ? date : LocalDate.now());
        String fileName = reportType.replace(" ", "_") + "_" + (date != null ? date : LocalDate.now()) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
