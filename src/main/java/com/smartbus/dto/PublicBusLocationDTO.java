package com.smartbus.dto;

public record PublicBusLocationDTO(
        Long busId,
        String busNumber,
        String routeName,
        Double latitude,
        Double longitude,
        Double speed,
        String status,
        String nextStopName,
        Double distanceToNextKm,
        Integer etaMinutes,
        String source,
        String lastUpdated) {

    public static PublicBusLocationDTO from(LiveBusLocationDTO location) {
        return new PublicBusLocationDTO(
                location.getBusId(),
                location.getBusNumber(),
                location.getRouteName(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSpeed(),
                location.getStatus(),
                location.getNextStopName(),
                location.getDistanceToNextKm(),
                location.getEtaMinutes(),
                location.getSource(),
                location.getLastUpdated());
    }
}
