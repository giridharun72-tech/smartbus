package com.smartbus.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "route_service_changes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_route_service_change_day", columnNames = {"route_id", "effective_date"})
})
public class RouteServiceChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, length = 250)
    private String reason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "replacement_bus_id")
    private Bus replacementBus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "affected_bus_id")
    private Bus affectedBus;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_affected_bus_status", length = 30)
    private BusStatus previousAffectedBusStatus;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "serviceChange", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<RouteServiceChangeStop> stops = new ArrayList<>();

    public Long getId() { return id; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Bus getReplacementBus() { return replacementBus; }
    public void setReplacementBus(Bus replacementBus) { this.replacementBus = replacementBus; }
    public Bus getAffectedBus() { return affectedBus; }
    public void setAffectedBus(Bus affectedBus) { this.affectedBus = affectedBus; }
    public BusStatus getPreviousAffectedBusStatus() { return previousAffectedBusStatus; }
    public void setPreviousAffectedBusStatus(BusStatus previousAffectedBusStatus) { this.previousAffectedBusStatus = previousAffectedBusStatus; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<RouteServiceChangeStop> getStops() { return stops; }
    public void setStops(List<RouteServiceChangeStop> stops) { this.stops = stops; }
}
