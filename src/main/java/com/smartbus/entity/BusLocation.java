package com.smartbus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_locations")
public class BusLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double speed;
    private Double heading;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationSource source = LocationSource.GPS;

    @Column(name = "next_stop_name", length = 100)
    private String nextStopName;

    @Column(name = "distance_to_next_stop_km")
    private Double distanceToNextStopKm;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    public BusLocation() {}

    public BusLocation(Bus bus, Double latitude, Double longitude, Double speed, Double heading, LocalDateTime timestamp, LocationSource source, String nextStopName, Double distanceToNextStopKm, Integer etaMinutes) {
        this.bus = bus;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.timestamp = timestamp;
        this.source = source;
        this.nextStopName = nextStopName;
        this.distanceToNextStopKm = distanceToNextStopKm;
        this.etaMinutes = etaMinutes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public Double getHeading() { return heading; }
    public void setHeading(Double heading) { this.heading = heading; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public LocationSource getSource() { return source; }
    public void setSource(LocationSource source) { this.source = source; }

    public String getNextStopName() { return nextStopName; }
    public void setNextStopName(String nextStopName) { this.nextStopName = nextStopName; }

    public Double getDistanceToNextStopKm() { return distanceToNextStopKm; }
    public void setDistanceToNextStopKm(Double distanceToNextStopKm) { this.distanceToNextStopKm = distanceToNextStopKm; }

    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }
}
