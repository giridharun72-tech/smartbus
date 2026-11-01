package com.smartbus.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", unique = true, nullable = false, length = 30)
    private String ticketNumber; // e.g. TKT-2026-000845

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", unique = true, nullable = false)
    private Booking booking;

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(name = "passenger_id", nullable = false, length = 30)
    private String passengerId;

    @Column(name = "bus_number", nullable = false, length = 30)
    private String busNumber;

    @Column(name = "route_name", nullable = false, length = 100)
    private String routeName;

    @Column(name = "from_stop", nullable = false, length = 100)
    private String fromStop;

    @Column(name = "to_stop", nullable = false, length = 100)
    private String toStop;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "seat_number", nullable = false, length = 50)
    private String seatNumber;

    @Column(nullable = false)
    private Double fare;

    @Lob
    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private TicketStatus verificationStatus = TicketStatus.VALID;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 50)
    private String verifiedBy;

    public Ticket() {}

    public Ticket(String ticketNumber, Booking booking, String passengerName, String passengerId, String busNumber, String routeName, String fromStop, String toStop, LocalDate travelDate, LocalTime departureTime, String seatNumber, Double fare, String qrCodeBase64) {
        this.ticketNumber = ticketNumber;
        this.booking = booking;
        this.passengerName = passengerName;
        this.passengerId = passengerId;
        this.busNumber = busNumber;
        this.routeName = routeName;
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.travelDate = travelDate;
        this.departureTime = departureTime;
        this.seatNumber = seatNumber;
        this.fare = fare;
        this.qrCodeBase64 = qrCodeBase64;
        this.verificationStatus = TicketStatus.VALID;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public String getFromStop() { return fromStop; }
    public void setFromStop(String fromStop) { this.fromStop = fromStop; }

    public String getToStop() { return toStop; }
    public void setToStop(String toStop) { this.toStop = toStop; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public Double getFare() { return fare; }
    public void setFare(Double fare) { this.fare = fare; }

    public String getQrCodeBase64() { return qrCodeBase64; }
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }

    public TicketStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(TicketStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
}
