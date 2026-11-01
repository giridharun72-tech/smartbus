package com.smartbus.controller;

import com.smartbus.dto.BusSearchResult;
import com.smartbus.dto.NearbyStopResult;
import com.smartbus.dto.BusPublicResponse;
import com.smartbus.repository.BusRepository;
import com.smartbus.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/buses")
public class BusApiController {

    private final SearchService searchService;
    private final BusRepository busRepository;

    public BusApiController(SearchService searchService, BusRepository busRepository) {
        this.searchService = searchService;
        this.busRepository = busRepository;
    }

    @GetMapping
    public ResponseEntity<List<BusPublicResponse>> getAllBuses() {
        List<BusPublicResponse> buses = busRepository.findAll().stream()
                .map(BusPublicResponse::from)
                .toList();
        return ResponseEntity.ok(buses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusPublicResponse> getBusById(@PathVariable Long id) {
        return busRepository.findById(id)
                .map(BusPublicResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BusSearchResult>> searchBuses(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(required = false) String busNumber) {

        List<BusSearchResult> results = searchService.searchBuses(from, to, date, time, busNumber);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyStopResult>> getNearbyStops(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5.0") double radiusKm) {

        List<NearbyStopResult> stops = searchService.findNearbyStops(lat, lon, radiusKm);
        return ResponseEntity.ok(stops);
    }
}
