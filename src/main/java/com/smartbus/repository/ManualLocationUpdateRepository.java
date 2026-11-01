package com.smartbus.repository;

import com.smartbus.entity.Bus;
import com.smartbus.entity.ManualLocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ManualLocationUpdateRepository extends JpaRepository<ManualLocationUpdate, Long> {
    List<ManualLocationUpdate> findByBusOrderByTimestampDesc(Bus bus);
}
