package com.smartbus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manual_location_updates")
public class ManualLocationUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 150)
    private String landmark;

    @Column(length = 255)
    private String reason;

    @Column(name = "bcu_authorized", nullable = false)
    private boolean bcuAuthorized = true;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public ManualLocationUpdate() {}

    public ManualLocationUpdate(Bus bus, Driver driver, Double latitude, Double longitude, String landmark, String reason, boolean bcuAuthorized) {
        this.bus = bus;
        this.driver = driver;
        this.latitude = latitude;
        this.longitude = longitude;
        this.landmark = landmark;
        this.reason = reason;
        this.bcuAuthorized = bcuAuthorized;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isBcuAuthorized() { return bcuAuthorized; }
    public void setBcuAuthorized(boolean bcuAuthorized) { this.bcuAuthorized = bcuAuthorized; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
