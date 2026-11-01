package com.smartbus.repository;

import com.smartbus.entity.Bus;
import com.smartbus.entity.Route;
import com.smartbus.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByRouteAndCancelledFalse(Route route);
    List<Schedule> findByBusAndCancelledFalse(Bus bus);

    @Query("SELECT s FROM Schedule s WHERE s.route = :route AND s.cancelled = false AND s.departureTime >= :depTime ORDER BY s.departureTime ASC")
    List<Schedule> findAvailableSchedulesFromTime(@Param("route") Route route, @Param("depTime") LocalTime depTime);

    @Query("SELECT s FROM Schedule s WHERE s.cancelled = false AND s.departureTime >= :depTime ORDER BY s.departureTime ASC")
    List<Schedule> findAllAvailableFromTime(@Param("depTime") LocalTime depTime);
}
