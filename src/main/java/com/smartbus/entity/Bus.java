package com.smartbus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "buses")
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bus_number", unique = true, nullable = false, length = 30)
    private String busNumber; // e.g. 21A

    @Column(name = "registration_number", unique = true, nullable = false, length = 30)
    private String registrationNumber;

    @Column(name = "bus_type", length = 30)
    private String busType = "STANDARD"; // STANDARD, AC, EXPRESS

    @Column(nullable = false)
    private Integer capacity = 40;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BusStatus status = BusStatus.RUNNING;

    @Column(name = "manual_location_allowed")
    private boolean manualLocationAllowed = false;

    @Column(name = "last_gps_update")
    private LocalDateTime lastGpsUpdate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "assigned_driver_name", length = 100)
    private String assignedDriverName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private Route route;

    @Column(name = "gps_device_id", length = 50)
    private String gpsDeviceId;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "current_speed")
    private Double currentSpeed = 0.0;

    @Column(name = "next_stop_name", length = 100)
    private String nextStopName;

    @Column(name = "distance_to_next_stop_km")
    private Double distanceToNextStopKm = 0.0;

    @Column(name = "eta_minutes")
    private Integer etaMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", length = 20)
    private LocationSource locationSource = LocationSource.GPS;

    public Bus() {}

    public Bus(String busNumber, String registrationNumber, String busType, Integer capacity, Route route, Driver driver, String gpsDeviceId) {
        this.busNumber = busNumber;
        this.registrationNumber = registrationNumber;
        this.busType = busType;
        this.capacity = capacity;
        this.route = route;
        this.driver = driver;
        this.gpsDeviceId = gpsDeviceId;
        this.status = BusStatus.RUNNING;
        this.manualLocationAllowed = false;
        this.lastGpsUpdate = LocalDateTime.now();
        this.locationSource = LocationSource.GPS;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public BusStatus getStatus() { return status; }
    public void setStatus(BusStatus status) { this.status = status; }

    public boolean isManualLocationAllowed() { return manualLocationAllowed; }
    public void setManualLocationAllowed(boolean manualLocationAllowed) { this.manualLocationAllowed = manualLocationAllowed; }

    public LocalDateTime getLastGpsUpdate() { return lastGpsUpdate; }
    public void setLastGpsUpdate(LocalDateTime lastGpsUpdate) { this.lastGpsUpdate = lastGpsUpdate; }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }

    public String getAssignedDriverName() { return assignedDriverName; }
    public void setAssignedDriverName(String assignedDriverName) { this.assignedDriverName = assignedDriverName; }

    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }

    public String getGpsDeviceId() { return gpsDeviceId; }
    public void setGpsDeviceId(String gpsDeviceId) { this.gpsDeviceId = gpsDeviceId; }

    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }

    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }

    public Double getCurrentSpeed() { return currentSpeed; }
    public void setCurrentSpeed(Double currentSpeed) { this.currentSpeed = currentSpeed; }

    public String getNextStopName() { return nextStopName; }
    public void setNextStopName(String nextStopName) { this.nextStopName = nextStopName; }

    public Double getDistanceToNextStopKm() { return distanceToNextStopKm; }
    public void setDistanceToNextStopKm(Double distanceToNextStopKm) { this.distanceToNextStopKm = distanceToNextStopKm; }

    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }

    public LocationSource getLocationSource() { return locationSource; }
    public void setLocationSource(LocationSource locationSource) { this.locationSource = locationSource; }
}
