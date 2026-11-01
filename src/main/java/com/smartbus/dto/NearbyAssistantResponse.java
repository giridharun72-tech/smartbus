package com.smartbus.dto;

import java.util.List;

public record NearbyAssistantResponse(String reply, List<TripSuggestion> suggestions, List<Source> sources) {
    public NearbyAssistantResponse(String reply, List<TripSuggestion> suggestions) {
        this(reply, suggestions, List.of());
    }

    public record Source(String title, String url) {}

    public record TripSuggestion(
            Long busId,
            String busNumber,
            String routeName,
            String destination,
            String busType,
            String status,
            String issue,
            String stopName,
            String distanceDisplay,
            int walkingMinutes,
            String departureTime,
            int busEtaMinutes,
            int totalEtaMinutes,
            boolean bookable) {}
}
