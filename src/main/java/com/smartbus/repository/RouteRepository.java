package com.smartbus.repository;

import com.smartbus.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByRouteNumber(String routeNumber);
    Optional<Route> findFirstByNameIgnoreCase(String name);
}
