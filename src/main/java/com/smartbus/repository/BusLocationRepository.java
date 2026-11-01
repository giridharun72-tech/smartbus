package com.smartbus.repository;

import com.smartbus.entity.Bus;
import com.smartbus.entity.BusLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BusLocationRepository extends JpaRepository<BusLocation, Long> {
    Optional<BusLocation> findTopByBusOrderByTimestampDesc(Bus bus);
    List<BusLocation> findTop10ByBusOrderByTimestampDesc(Bus bus);
}
