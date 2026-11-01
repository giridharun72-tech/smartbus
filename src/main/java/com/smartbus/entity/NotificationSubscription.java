package com.smartbus.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_subscriptions")
public class NotificationSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "alert_type", length = 30)
    private String alertType = "ALL"; // ALL, CANCELLATION, DELAY, PROXIMITY

    @Column(name = "stop_distance_meters")
    private Integer stopDistanceMeters; // e.g. 500, 1000, 2000, 5000

    @Column(name = "target_stop_name", length = 100)
    private String targetStopName;

    @Column(name = "notified_for_current_trip")
    private boolean notifiedForCurrentTrip = false;

    public NotificationSubscription() {}

    public NotificationSubscription(User user, Bus bus, String alertType, Integer stopDistanceMeters, String targetStopName) {
        this.user = user;
        this.bus = bus;
        this.alertType = alertType;
        this.stopDistanceMeters = stopDistanceMeters;
        this.targetStopName = targetStopName;
        this.notifiedForCurrentTrip = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public Integer getStopDistanceMeters() { return stopDistanceMeters; }
    public void setStopDistanceMeters(Integer stopDistanceMeters) { this.stopDistanceMeters = stopDistanceMeters; }

    public String getTargetStopName() { return targetStopName; }
    public void setTargetStopName(String targetStopName) { this.targetStopName = targetStopName; }

    public boolean isNotifiedForCurrentTrip() { return notifiedForCurrentTrip; }
    public void setNotifiedForCurrentTrip(boolean notifiedForCurrentTrip) { this.notifiedForCurrentTrip = notifiedForCurrentTrip; }
}
