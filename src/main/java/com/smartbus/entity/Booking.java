package com.smartbus.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", unique = true, nullable = false, length = 40)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "from_stop", nullable = false, length = 100)
    private String fromStop;

    @Column(name = "to_stop", nullable = false, length = 100)
    private String toStop;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "seat_numbers", length = 100)
    private String seatNumbers; // e.g. "A1, A2"

    @Column(name = "number_of_seats", nullable = false)
    private Integer numberOfSeats = 1;

    @Column(name = "total_fare", nullable = false)
    private Double totalFare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Booking() {}

    public Booking(String bookingReference, User user, Bus bus, Route route, String fromStop, String toStop, LocalDate travelDate, LocalTime departureTime, String seatNumbers, Integer numberOfSeats, Double totalFare) {
        this.bookingReference = bookingReference;
        this.user = user;
        this.bus = bus;
        this.route = route;
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.travelDate = travelDate;
        this.departureTime = departureTime;
        this.seatNumbers = seatNumbers;
        this.numberOfSeats = numberOfSeats;
        this.totalFare = totalFare;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }

    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }

    public String getFromStop() { return fromStop; }
    public void setFromStop(String fromStop) { this.fromStop = fromStop; }

    public String getToStop() { return toStop; }
    public void setToStop(String toStop) { this.toStop = toStop; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public String getSeatNumbers() { return seatNumbers; }
    public void setSeatNumbers(String seatNumbers) { this.seatNumbers = seatNumbers; }

    public Integer getNumberOfSeats() { return numberOfSeats; }
    public void setNumberOfSeats(Integer numberOfSeats) { this.numberOfSeats = numberOfSeats; }

    public Double getTotalFare() { return totalFare; }
    public void setTotalFare(Double totalFare) { this.totalFare = totalFare; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
