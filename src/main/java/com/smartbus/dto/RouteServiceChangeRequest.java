package com.smartbus.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class RouteServiceChangeRequest {
    @NotNull
    private LocalDate effectiveDate;

    @NotBlank
    @Size(max = 250)
    private String reason;

    private Long replacementBusId;

    private Long affectedBusId;

    @NotNull
    @Size(min = 2, max = 50)
    @Valid
    private List<NewRouteStopRequest> stops;

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getReplacementBusId() { return replacementBusId; }
    public void setReplacementBusId(Long replacementBusId) { this.replacementBusId = replacementBusId; }
    public Long getAffectedBusId() { return affectedBusId; }
    public void setAffectedBusId(Long affectedBusId) { this.affectedBusId = affectedBusId; }
    public List<NewRouteStopRequest> getStops() { return stops; }
    public void setStops(List<NewRouteStopRequest> stops) { this.stops = stops; }
}
