package com.smartbus.dto;

import java.time.LocalTime;

public class BusSearchResult {
    private Long busId;
    private String busNumber;
    private String registrationNumber;
    private String routeName;
    private String source;
    private String destination;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String status;
    private int availableSeats;
    private double fare;
    private double distanceKm;
    private String distanceDisplay;
    private int etaMinutes;
    private String nextStopName;
    private String busType;

    public BusSearchResult() {}

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public String getDistanceDisplay() { return distanceDisplay; }
    public void setDistanceDisplay(String distanceDisplay) { this.distanceDisplay = distanceDisplay; }
    public int getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(int etaMinutes) { this.etaMinutes = etaMinutes; }
    public String getNextStopName() { return nextStopName; }
    public void setNextStopName(String nextStopName) { this.nextStopName = nextStopName; }
    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }
}
