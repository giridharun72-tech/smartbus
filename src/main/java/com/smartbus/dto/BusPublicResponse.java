package com.smartbus.dto;

import com.smartbus.entity.Bus;

import java.time.LocalDateTime;

public record BusPublicResponse(
        Long id,
        String busNumber,
        String busType,
        String status,
        String routeName,
        Double latitude,
        Double longitude,
        Double speed,
        String nextStopName,
        Integer etaMinutes,
        String locationSource,
        LocalDateTime lastGpsUpdate) {

    public static BusPublicResponse from(Bus bus) {
        return new BusPublicResponse(
                bus.getId(),
                bus.getBusNumber(),
                bus.getBusType(),
                bus.getStatus() == null ? null : bus.getStatus().name(),
                bus.getRoute() == null ? null : bus.getRoute().getName(),
                bus.getCurrentLatitude(),
                bus.getCurrentLongitude(),
                bus.getCurrentSpeed(),
                bus.getNextStopName(),
                bus.getEtaMinutes(),
                bus.getLocationSource() == null ? null : bus.getLocationSource().name(),
                bus.getLastGpsUpdate());
    }
}
