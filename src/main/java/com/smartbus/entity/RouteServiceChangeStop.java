package com.smartbus.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "route_service_change_stops")
public class RouteServiceChangeStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_change_id", nullable = false)
    private RouteServiceChange serviceChange;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    public RouteServiceChangeStop() {}

    public RouteServiceChangeStop(RouteServiceChange serviceChange, String name, Double latitude, Double longitude, Integer stopOrder) {
        this.serviceChange = serviceChange;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.stopOrder = stopOrder;
    }

    public Long getId() { return id; }
    public RouteServiceChange getServiceChange() { return serviceChange; }
    public String getName() { return name; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Integer getStopOrder() { return stopOrder; }
}
