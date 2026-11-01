package com.smartbus.dto;

import java.time.LocalDate;
import java.util.List;

public class BookingRequestDTO {
    private Long busId;
    private Long routeId;
    private String fromStop;
    private String toStop;
    private LocalDate travelDate;
    private List<String> seatNumbers;

    public BookingRequestDTO() {}

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public String getFromStop() { return fromStop; }
    public void setFromStop(String fromStop) { this.fromStop = fromStop; }
    public String getToStop() { return toStop; }
    public void setToStop(String toStop) { this.toStop = toStop; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    public List<String> getSeatNumbers() { return seatNumbers; }
    public void setSeatNumbers(List<String> seatNumbers) { this.seatNumbers = seatNumbers; }
}
