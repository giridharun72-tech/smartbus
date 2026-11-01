package com.smartbus.repository;

import com.smartbus.entity.Route;
import com.smartbus.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    List<RouteStop> findByRouteOrderByStopOrderAsc(Route route);
}
