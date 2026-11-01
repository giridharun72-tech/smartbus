package com.smartbus.dto;

import java.time.LocalDate;
import java.util.List;

public record RouteServiceChangeResponse(
        Long id,
        Long routeId,
        String routeName,
        LocalDate effectiveDate,
        String reason,
        Long affectedBusId,
        String affectedBusNumber,
        Long replacementBusId,
        String replacementBusNumber,
        List<Stop> stops) {

    public record Stop(String name, Double latitude, Double longitude, Integer order) {}
}
