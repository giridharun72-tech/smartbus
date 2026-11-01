package com.smartbus.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "route_stops")
public class RouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stop_id", nullable = false)
    private BusStop stop;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "distance_from_start_km")
    private Double distanceFromStartKm;

    @Column(name = "scheduled_minutes_from_start")
    private Integer scheduledMinutesFromStart;

    @Column(name = "fare_from_start")
    private Double fareFromStart;

    public RouteStop() {}

    public RouteStop(Route route, BusStop stop, Integer stopOrder, Double distanceFromStartKm, Integer scheduledMinutesFromStart, Double fareFromStart) {
        this.route = route;
        this.stop = stop;
        this.stopOrder = stopOrder;
        this.distanceFromStartKm = distanceFromStartKm;
        this.scheduledMinutesFromStart = scheduledMinutesFromStart;
        this.fareFromStart = fareFromStart;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }

    public BusStop getStop() { return stop; }
    public void setStop(BusStop stop) { this.stop = stop; }

    public Integer getStopOrder() { return stopOrder; }
    public void setStopOrder(Integer stopOrder) { this.stopOrder = stopOrder; }

    public Double getDistanceFromStartKm() { return distanceFromStartKm; }
    public void setDistanceFromStartKm(Double distanceFromStartKm) { this.distanceFromStartKm = distanceFromStartKm; }

    public Integer getScheduledMinutesFromStart() { return scheduledMinutesFromStart; }
    public void setScheduledMinutesFromStart(Integer scheduledMinutesFromStart) { this.scheduledMinutesFromStart = scheduledMinutesFromStart; }

    public Double getFareFromStart() { return fareFromStart; }
    public void setFareFromStart(Double fareFromStart) { this.fareFromStart = fareFromStart; }
}
