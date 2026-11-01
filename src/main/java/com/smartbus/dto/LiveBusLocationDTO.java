package com.smartbus.dto;

public class LiveBusLocationDTO {
    private Long busId;
    private String busNumber;
    private String registrationNumber;
    private String gpsDeviceId;
    private Integer capacity;
    private String routeName;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private String status;
    private String nextStopName;
    private Double distanceToNextKm;
    private Integer etaMinutes;
    private String source; // GPS, PHONE_GPS, MANUAL, LAST_KNOWN
    private String lastUpdated;
    private String driverName;
    private String driverPhone;

    public LiveBusLocationDTO() {}

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getGpsDeviceId() { return gpsDeviceId; }
    public void setGpsDeviceId(String gpsDeviceId) { this.gpsDeviceId = gpsDeviceId; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNextStopName() { return nextStopName; }
    public void setNextStopName(String nextStopName) { this.nextStopName = nextStopName; }
    public Double getDistanceToNextKm() { return distanceToNextKm; }
    public void setDistanceToNextKm(Double distanceToNextKm) { this.distanceToNextKm = distanceToNextKm; }
    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
}
