package com.smartbus.repository;

import com.smartbus.entity.RouteServiceChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RouteServiceChangeRepository extends JpaRepository<RouteServiceChange, Long> {
    Optional<RouteServiceChange> findByRouteIdAndEffectiveDateAndActiveTrue(Long routeId, LocalDate effectiveDate);

    Optional<RouteServiceChange> findByRouteIdAndEffectiveDate(Long routeId, LocalDate effectiveDate);

    boolean existsByRouteIdAndEffectiveDateAndActiveTrueAndReplacementBus_BusNumberIgnoreCase(
            Long routeId, LocalDate effectiveDate, String busNumber);
}
