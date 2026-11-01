package com.smartbus.dto;

import com.smartbus.entity.BusStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateBusRequest {
    @NotBlank
    @Size(max = 30)
    private String busNumber;

    @NotBlank
    @Size(max = 100)
    private String route;

    @NotBlank
    @Size(max = 100)
    private String driverName;

    @NotNull
    @Min(1)
    private Integer totalSeats = 40;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double currentLatitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double currentLongitude;

    @NotNull
    private BusStatus status = BusStatus.RUNNING;

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }

    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }

    public BusStatus getStatus() { return status; }
    public void setStatus(BusStatus status) { this.status = status; }
}
