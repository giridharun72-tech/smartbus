package com.smartbus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
public class CreateDriverBusRequest {
    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$", message = "Driver ID must be 3-30 letters, numbers, hyphens, or underscores.")
    private String driverId;

    @NotBlank
    @Size(min = 12, max = 72)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
            message = "Password must include uppercase and lowercase letters, a number, and a symbol.")
    private String password;

    @NotBlank
    @Size(max = 100)
    private String driverName;

    @NotBlank
    @Pattern(regexp = "^[+0-9()\\s-]{8,20}$", message = "Enter a valid phone number.")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{12}$", message = "Aadhaar number must contain exactly 12 digits.")
    private String aadhaarNumber;

    @NotBlank
    @Size(max = 50)
    private String licenseNumber;

    @NotBlank
    @Size(max = 30)
    private String busNumber;

    @NotBlank
    @Pattern(regexp = "(?i)^TN[- ]?\\d{1,2}[- ]?[A-Z]{1,3}[- ]?\\d{1,4}$",
            message = "Vehicle number must be a valid Tamil Nadu plate beginning with TN.")
    private String registrationNumber;

    @NotBlank
    @Size(max = 100)
    private String route;

    @NotNull
    @Min(1)
    private Integer totalSeats = 40;

    @Valid
    @Size(max = 50)
    private List<NewRouteStopRequest> routeStops = new ArrayList<>();

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public List<NewRouteStopRequest> getRouteStops() { return routeStops; }
    public void setRouteStops(List<NewRouteStopRequest> routeStops) { this.routeStops = routeStops; }
}
