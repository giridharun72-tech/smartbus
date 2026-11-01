package com.smartbus.service;

import com.smartbus.dto.BusSearchResult;
import com.smartbus.dto.NearbyBusOption;
import com.smartbus.dto.NearbyStopResult;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final BusRepository busRepository;
    private final ScheduleRepository scheduleRepository;
    private final RouteRepository routeRepository;
    private final BusStopRepository busStopRepository;
    private final RouteStopRepository routeStopRepository;
    private final SeatRepository seatRepository;
    private final IncidentRepository incidentRepository;

    public SearchService(BusRepository busRepository,
                         ScheduleRepository scheduleRepository,
                         RouteRepository routeRepository,
                         BusStopRepository busStopRepository,
                         RouteStopRepository routeStopRepository,
                         SeatRepository seatRepository,
                         IncidentRepository incidentRepository) {
        this.busRepository = busRepository;
        this.scheduleRepository = scheduleRepository;
        this.routeRepository = routeRepository;
        this.busStopRepository = busStopRepository;
        this.routeStopRepository = routeStopRepository;
        this.seatRepository = seatRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public List<BusSearchResult> searchBuses(String fromLocation, String toLocation, LocalDate travelDate, LocalTime requestedTime, String busNumberFilter) {
        LocalDate date = (travelDate != null) ? travelDate : LocalDate.now();
        LocalTime time = (requestedTime != null) ? requestedTime : LocalTime.of(0, 0);

        List<Schedule> schedules = scheduleRepository.findAllAvailableFromTime(time);
        List<BusSearchResult> results = new ArrayList<>();

        for (Schedule s : schedules) {
            Bus bus = s.getBus();
            Route route = s.getRoute();

            if (bus == null || route == null) continue;

            if (busNumberFilter != null && !busNumberFilter.isBlank()) {
                if (!bus.getBusNumber().equalsIgnoreCase(busNumberFilter.trim())) {
                    continue;
                }
            }

            // Check if route serves fromLocation and toLocation
            boolean matchesRoute = true;
            double calculatedFare = 25.0; // fallback base fare
            double travelDistanceKm = route.getTotalDistanceKm() != null ? route.getTotalDistanceKm() : 12.0;

            if (fromLocation != null && !fromLocation.isBlank() && toLocation != null && !toLocation.isBlank()) {
                List<RouteStop> stops = route.getRouteStops();
                int fromIndex = -1;
                int toIndex = -1;
                Double fromFare = 0.0;
                Double toFare = 0.0;
                Double fromDist = 0.0;
                Double toDist = 0.0;

                for (RouteStop rs : stops) {
                    String stopName = rs.getStop().getName().toLowerCase();
                    if (stopName.contains(fromLocation.trim().toLowerCase()) && fromIndex == -1) {
                        fromIndex = rs.getStopOrder();
                        fromFare = rs.getFareFromStart() != null ? rs.getFareFromStart() : 0.0;
                        fromDist = rs.getDistanceFromStartKm() != null ? rs.getDistanceFromStartKm() : 0.0;
                    }
                    if (stopName.contains(toLocation.trim().toLowerCase())) {
                        toIndex = rs.getStopOrder();
                        toFare = rs.getFareFromStart() != null ? rs.getFareFromStart() : 25.0;
                        toDist = rs.getDistanceFromStartKm() != null ? rs.getDistanceFromStartKm() : 10.0;
                    }
                }

                if (fromIndex != -1 && toIndex != -1 && fromIndex < toIndex) {
                    calculatedFare = Math.max(15.0, Math.abs(toFare - fromFare));
                    travelDistanceKm = Math.max(1.0, Math.abs(toDist - fromDist));
                } else if (!route.getSource().toLowerCase().contains(fromLocation.toLowerCase()) ||
                           !route.getDestination().toLowerCase().contains(toLocation.toLowerCase())) {
                    // Does not match route
                    matchesRoute = false;
                }
            }

            if (!matchesRoute) continue;

            // Calculate available seats
            List<Seat> bookedSeats = seatRepository.findByBusAndTravelDate(bus, date);
            long bookedCount = bookedSeats.stream().filter(st -> st.getStatus() == SeatStatus.BOOKED).count();
            int availableSeats = Math.max(0, bus.getCapacity() - (int) bookedCount);

            // Compute distance from bus current location to fromStop or route source
            double busDistKm = 1.8;
            if (bus.getDistanceToNextStopKm() != null && bus.getDistanceToNextStopKm() > 0) {
                busDistKm = bus.getDistanceToNextStopKm();
            }

            int eta = TrackingService.calculateEtaMinutes(busDistKm, bus.getCurrentSpeed());

            BusSearchResult res = new BusSearchResult();
            res.setBusId(bus.getId());
            res.setBusNumber(bus.getBusNumber());
            res.setRegistrationNumber(bus.getRegistrationNumber());
            res.setRouteName(route.getName() + " (" + route.getRouteNumber() + ")");
            res.setSource(fromLocation != null && !fromLocation.isBlank() ? fromLocation : route.getSource());
            res.setDestination(toLocation != null && !toLocation.isBlank() ? toLocation : route.getDestination());
            res.setDepartureTime(s.getDepartureTime());
            res.setArrivalTime(s.getArrivalTime());
            res.setStatus(bus.getStatus().name());
            res.setAvailableSeats(availableSeats);
            res.setFare(calculatedFare);
            res.setDistanceKm(travelDistanceKm);
            res.setDistanceDisplay(TrackingService.formatDistance(busDistKm));
            res.setEtaMinutes(eta);
            res.setNextStopName(bus.getNextStopName() != null ? bus.getNextStopName() : "Origin");
            res.setBusType(bus.getBusType());

            results.add(res);
        }

        // Strictly sort chronologically by departure time >= requested time
        results.sort(Comparator.comparing(BusSearchResult::getDepartureTime));
        return results;
    }

    @Transactional(readOnly = true)
    public List<NearbyStopResult> findNearbyStops(double userLat, double userLon, double radiusKm) {
        List<BusStop> allStops = busStopRepository.findAll();
        List<NearbyStopResult> nearby = new ArrayList<>();

        for (BusStop stop : allStops) {
            double distKm = TrackingService.calculateDistanceKm(userLat, userLon, stop.getLatitude(), stop.getLongitude());
            if (distKm <= radiusKm) {
                NearbyStopResult dto = new NearbyStopResult();
                dto.setStopId(stop.getId());
                dto.setStopName(stop.getName());
                dto.setLatitude(stop.getLatitude());
                dto.setLongitude(stop.getLongitude());
                dto.setLandmark(stop.getLandmark());
                dto.setDistanceMeters(Math.round(distKm * 1000));
                dto.setDistanceDisplay(TrackingService.formatDistance(distKm));
                dto.setWalkingMinutes(TrackingService.calculateWalkingMinutes(distKm));

                // Find buses passing through this stop
                List<RouteStop> routeStops = routeStopRepository.findAll().stream()
                        .filter(rs -> rs.getStop().getId().equals(stop.getId()))
                        .collect(Collectors.toList());

                Set<String> busNumbers = new LinkedHashSet<>();
                List<NearbyBusOption> busOptions = new ArrayList<>();
                LocalTime now = LocalTime.now();
                for (RouteStop rs : routeStops) {
                    List<Bus> busesOnRoute = busRepository.findAll().stream()
                            .filter(b -> b.getRoute() != null && b.getRoute().getId().equals(rs.getRoute().getId()))
                            .collect(Collectors.toList());
                    for (Bus b : busesOnRoute) {
                        busNumbers.add(b.getBusNumber());
                    List<Schedule> allSchedules = scheduleRepository.findByBusAndCancelledFalse(b).stream()
                        .sorted(Comparator.comparing(Schedule::getDepartureTime))
                        .collect(Collectors.toList());
                    List<Schedule> schedules = allSchedules.stream()
                        .filter(s -> !s.getDepartureTime().isBefore(now))
                        .limit(3)
                        .collect(Collectors.toList());
                    if (schedules.isEmpty()) {
                        schedules = allSchedules.stream().limit(3).collect(Collectors.toList());
                    }
                    for (Schedule schedule : schedules) {
                        NearbyBusOption option = new NearbyBusOption();
                        int busEta = b.getStatus() == BusStatus.RUNNING
                            ? TrackingService.calculateEtaMinutes(
                                b.getDistanceToNextStopKm() != null ? b.getDistanceToNextStopKm() : 1.0,
                                b.getCurrentSpeed())
                            : 0;
                        String issue = incidentRepository.findFirstByBusAndStatusInOrderByReportedAtDesc(
                                b, List.of(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS))
                            .map(i -> i.getType().name().replace('_', ' ') + ": " + i.getDescription())
                            .orElse(null);
                        option.setBusId(b.getId());
                        option.setBusNumber(b.getBusNumber());
                        option.setRouteName(rs.getRoute().getName() + " (" + rs.getRoute().getRouteNumber() + ")");
                        option.setDestination(rs.getRoute().getDestination());
                        option.setBusType(b.getBusType());
                        option.setDepartureTime(schedule.getDepartureTime().toString());
                        option.setArrivalTime(schedule.getArrivalTime().toString());
                        option.setStatus(b.getStatus().name());
                        option.setIssue(issue);
                        option.setBusEtaMinutes(busEta);
                        option.setWalkingMinutes(dto.getWalkingMinutes());
                        option.setTotalEtaMinutes(Math.max(busEta, minutesUntil(schedule.getDepartureTime(), now)) + dto.getWalkingMinutes());
                        option.setNextStopName(b.getNextStopName());
                        option.setBookable(b.getStatus() != BusStatus.CANCELLED && issue == null);
                        busOptions.add(option);
                    }
                    }
                }
                dto.setAvailableBuses(new ArrayList<>(busNumbers));
                busOptions.sort(Comparator.comparingInt(NearbyBusOption::getTotalEtaMinutes));
                dto.setBusOptions(busOptions);
                nearby.add(dto);
            }
        }

        // Sort ascending by distance
        nearby.sort(Comparator.comparingDouble(NearbyStopResult::getDistanceMeters));
        return nearby;
    }

    private int minutesUntil(LocalTime departure, LocalTime now) {
        long minutes = java.time.Duration.between(now, departure).toMinutes();
        return (int) (minutes >= 0 ? minutes : minutes + (24 * 60));
    }
}
