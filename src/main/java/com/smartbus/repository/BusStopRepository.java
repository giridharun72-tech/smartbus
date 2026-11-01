package com.smartbus.repository;

import com.smartbus.entity.BusStop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusStopRepository extends JpaRepository<BusStop, Long> {
    List<BusStop> findByNameContainingIgnoreCase(String name);
}
