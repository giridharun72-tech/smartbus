package com.smartbus.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.smartbus.entity.Booking;
import com.smartbus.entity.Ticket;
import com.smartbus.repository.BookingRepository;
import com.smartbus.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
public class PdfService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;

    public PdfService(TicketRepository ticketRepository, BookingRepository bookingRepository) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
    }

    public byte[] generateTicketPdf(Ticket ticket) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Colors
            Color primaryColor = new Color(13, 110, 253);
            Color darkColor = new Color(33, 37, 41);
            Color lightGray = new Color(245, 247, 250);

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryColor);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkColor);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, darkColor);
            Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(25, 135, 84));

            // Header Banner
            Paragraph title = new Paragraph("SmartBus Public Transport System", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph subtitle = new Paragraph("Official Electronic Boarding Pass & Reservation Receipt", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            doc.add(subtitle);

            // Status Badge Table
            PdfPTable statusTable = new PdfPTable(2);
            statusTable.setWidthPercentage(100);
            statusTable.setWidths(new float[]{70, 30});

            PdfPCell ticketCell = new PdfPCell(new Phrase("TICKET NUMBER: " + ticket.getTicketNumber(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, darkColor)));
            ticketCell.setBorder(Rectangle.NO_BORDER);
            ticketCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            statusTable.addCell(ticketCell);

            PdfPCell badgeCell = new PdfPCell(new Phrase("STATUS: " + ticket.getVerificationStatus().name(), statusFont));
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            statusTable.addCell(badgeCell);

            doc.add(statusTable);
            doc.add(new Paragraph(" "));

            // Details Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);
            table.setSpacingAfter(15);

            addTableRow(table, "Passenger Name:", ticket.getPassengerName(), labelFont, valueFont);
            addTableRow(table, "Passenger ID:", ticket.getPassengerId(), labelFont, valueFont);
            addTableRow(table, "Bus Number:", ticket.getBusNumber(), labelFont, valueFont);
            addTableRow(table, "Route:", ticket.getRouteName(), labelFont, valueFont);
            addTableRow(table, "Origin (From):", ticket.getFromStop(), labelFont, valueFont);
            addTableRow(table, "Destination (To):", ticket.getToStop(), labelFont, valueFont);
            addTableRow(table, "Travel Date:", ticket.getTravelDate() != null ? ticket.getTravelDate().toString() : "N/A", labelFont, valueFont);
            addTableRow(table, "Departure Time:", ticket.getDepartureTime() != null ? ticket.getDepartureTime().toString() : "Scheduled", labelFont, valueFont);
            addTableRow(table, "Reserved Seat(s):", ticket.getSeatNumber(), labelFont, valueFont);
            addTableRow(table, "Total Fare Paid:", String.format("Rs. %.2f (PAID)", ticket.getFare()), labelFont, valueFont);

            doc.add(table);

            // Embed QR Code
            if (ticket.getQrCodeBase64() != null && ticket.getQrCodeBase64().contains(",")) {
                String base64Data = ticket.getQrCodeBase64().split(",")[1];
                byte[] imgBytes = Base64.getDecoder().decode(base64Data);
                Image qrImg = Image.getInstance(imgBytes);
                qrImg.setAlignment(Element.ALIGN_CENTER);
                qrImg.scaleToFit(140, 140);
                doc.add(qrImg);

                Paragraph scanText = new Paragraph("Present this QR code or Ticket Number to the Driver/Conductor for boarding verification.", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY));
                scanText.setAlignment(Element.ALIGN_CENTER);
                scanText.setSpacingBefore(5);
                doc.add(scanText);
            }

            // Footer instructions
            doc.add(new Paragraph(" "));
            LineSeparator line = new LineSeparator();
            line.setLineColor(Color.LIGHT_GRAY);
            doc.add(line);

            Paragraph footer = new Paragraph("Important: Passengers who forget their mobile device can provide their Ticket Number or Passenger ID to the Conductor for immediate digital verification.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating ticket PDF", e);
        }
    }

    public byte[] generateTravelHistoryPdf(String passengerId) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(13, 110, 253));
            Paragraph title = new Paragraph("Passenger Travel History Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph sub = new Paragraph("Passenger ID: " + passengerId + " | Generated on: " + LocalDate.now(), FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            doc.add(sub);

            List<Ticket> tickets = ticketRepository.findByPassengerIdOrderByTravelDateDesc(passengerId);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{18, 12, 18, 16, 16, 10, 10});

            String[] headers = {"Ticket #", "Date", "Bus / Route", "From", "To", "Seat", "Status"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                cell.setBackgroundColor(new Color(33, 37, 41));
                cell.setPadding(6);
                table.addCell(cell);
            }

            for (Ticket t : tickets) {
                table.addCell(createCell(t.getTicketNumber()));
                table.addCell(createCell(t.getTravelDate() != null ? t.getTravelDate().toString() : ""));
                table.addCell(createCell(t.getBusNumber() + " - " + t.getRouteName()));
                table.addCell(createCell(t.getFromStop()));
                table.addCell(createCell(t.getToStop()));
                table.addCell(createCell(t.getSeatNumber()));
                table.addCell(createCell(t.getVerificationStatus().name()));
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating travel history PDF", e);
        }
    }

    public byte[] generateRevenueReportPdf(String reportType, LocalDate date) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(13, 110, 253));
            Paragraph title = new Paragraph("Transport Operations - " + reportType, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph sub = new Paragraph("Report Date: " + (date != null ? date.toString() : LocalDate.now().toString()), FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            doc.add(sub);

            List<Booking> bookings = bookingRepository.findAll();

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{20, 15, 25, 15, 10, 15});

            String[] headers = {"Booking Ref", "Date", "Route", "Passenger", "Seats", "Fare"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                cell.setBackgroundColor(new Color(13, 110, 253));
                cell.setPadding(6);
                table.addCell(cell);
            }

            double totalRevenue = 0.0;
            for (Booking b : bookings) {
                table.addCell(createCell(b.getBookingReference()));
                table.addCell(createCell(b.getTravelDate().toString()));
                table.addCell(createCell(b.getRoute() != null ? b.getRoute().getName() : ""));
                table.addCell(createCell(b.getUser().getUsername()));
                table.addCell(createCell(b.getNumberOfSeats().toString()));
                table.addCell(createCell("Rs. " + b.getTotalFare()));
                totalRevenue += b.getTotalFare();
            }

            doc.add(table);

            Paragraph total = new Paragraph("Total Operations Revenue: Rs. " + String.format("%.2f", totalRevenue), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(25, 135, 84)));
            total.setSpacingBefore(15);
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating revenue report PDF", e);
        }
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setPadding(6);
        c1.setBackgroundColor(new Color(245, 247, 250));
        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        c2.setPadding(6);
        table.addCell(c1);
        table.addCell(c2);
    }

    private PdfPCell createCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setPadding(5);
        return cell;
    }
}
