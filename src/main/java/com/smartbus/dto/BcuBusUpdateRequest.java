package com.smartbus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BcuBusUpdateRequest {
    @NotBlank
    @Size(max = 30)
    private String busNumber;

    @NotBlank
    @Pattern(regexp = "(?i)^TN[- ]?\\d{1,2}[- ]?[A-Z]{1,3}[- ]?\\d{1,4}$")
    private String registrationNumber;

    @NotNull
    private Long routeId;

    @NotNull
    @Min(1)
    private Integer totalSeats;

    @NotBlank
    @Size(max = 100)
    private String driverName;

    @NotBlank
    @Pattern(regexp = "^[+0-9()\\s-]{8,20}$")
    private String driverPhone;

    @NotBlank
    @Size(max = 50)
    private String licenseNumber;

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}
