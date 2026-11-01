package com.smartbus.dto;

public class NearbyBusOption {
    private Long busId;
    private String busNumber;
    private String routeName;
    private String destination;
    private String busType;
    private String departureTime;
    private String arrivalTime;
    private String status;
    private String issue;
    private int busEtaMinutes;
    private int walkingMinutes;
    private int totalEtaMinutes;
    private String nextStopName;
    private boolean bookable;

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public int getBusEtaMinutes() { return busEtaMinutes; }
    public void setBusEtaMinutes(int busEtaMinutes) { this.busEtaMinutes = busEtaMinutes; }
    public int getWalkingMinutes() { return walkingMinutes; }
    public void setWalkingMinutes(int walkingMinutes) { this.walkingMinutes = walkingMinutes; }
    public int getTotalEtaMinutes() { return totalEtaMinutes; }
    public void setTotalEtaMinutes(int totalEtaMinutes) { this.totalEtaMinutes = totalEtaMinutes; }
    public String getNextStopName() { return nextStopName; }
    public void setNextStopName(String nextStopName) { this.nextStopName = nextStopName; }
    public boolean isBookable() { return bookable; }
    public void setBookable(boolean bookable) { this.bookable = bookable; }
}
