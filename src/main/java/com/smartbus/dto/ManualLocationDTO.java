package com.smartbus.dto;

public class ManualLocationDTO {
    private Long busId;
    private Double latitude;
    private Double longitude;
    private String landmark;
    private String reason;

    public ManualLocationDTO() {}

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
