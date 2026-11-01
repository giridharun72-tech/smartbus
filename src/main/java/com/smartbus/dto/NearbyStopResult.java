package com.smartbus.dto;

import java.util.List;

public class NearbyStopResult {
    private Long stopId;
    private String stopName;
    private double latitude;
    private double longitude;
    private String landmark;
    private double distanceMeters;
    private String distanceDisplay;
    private int walkingMinutes;
    private List<String> availableBuses;
    private List<NearbyBusOption> busOptions;

    public NearbyStopResult() {}

    public Long getStopId() { return stopId; }
    public void setStopId(Long stopId) { this.stopId = stopId; }
    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
    public double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
    public String getDistanceDisplay() { return distanceDisplay; }
    public void setDistanceDisplay(String distanceDisplay) { this.distanceDisplay = distanceDisplay; }
    public int getWalkingMinutes() { return walkingMinutes; }
    public void setWalkingMinutes(int walkingMinutes) { this.walkingMinutes = walkingMinutes; }
    public List<String> getAvailableBuses() { return availableBuses; }
    public void setAvailableBuses(List<String> availableBuses) { this.availableBuses = availableBuses; }
    public List<NearbyBusOption> getBusOptions() { return busOptions; }
    public void setBusOptions(List<NearbyBusOption> busOptions) { this.busOptions = busOptions; }
}
