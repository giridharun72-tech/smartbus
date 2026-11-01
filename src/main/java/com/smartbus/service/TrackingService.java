package com.smartbus.service;

import org.springframework.stereotype.Service;

@Service
public class TrackingService {

    /**
     * Calculates the great-circle distance between two points using the Haversine formula.
     * @return distance in kilometers
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Formats distance: meters when under 1 km, kilometers when above 1 km.
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1.0) {
            int meters = (int) Math.round(distanceKm * 1000);
            return meters + " m";
        } else {
            return String.format("%.1f km", distanceKm);
        }
    }

    /**
     * Estimates walking time assuming average human walking speed of ~4.8 km/h (80 m/min).
     */
    public static int calculateWalkingMinutes(double distanceKm) {
        double minutes = (distanceKm * 1000) / 80.0;
        return (int) Math.max(1, Math.round(minutes));
    }

    /**
     * Calculates ETA in minutes based on distance and speed. Fallback average speed: 25 km/h.
     */
    public static int calculateEtaMinutes(double distanceKm, Double speedKmH) {
        double effectiveSpeed = (speedKmH != null && speedKmH > 5.0) ? speedKmH : 25.0;
        double hours = distanceKm / effectiveSpeed;
        return (int) Math.max(1, Math.round(hours * 60));
    }
}
