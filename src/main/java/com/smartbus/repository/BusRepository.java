package com.smartbus.repository;

import com.smartbus.entity.Bus;
import com.smartbus.entity.BusStatus;
import com.smartbus.entity.Driver;
import com.smartbus.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, Long> {
    Optional<Bus> findByBusNumber(String busNumber);
    Optional<Bus> findFirstByBusNumberIgnoreCase(String busNumber);
    Optional<Bus> findByGpsDeviceId(String gpsDeviceId);
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
    List<Bus> findByStatus(BusStatus status);
    Optional<Bus> findByDriver(Driver driver);
    List<Bus> findByRoute(Route route);
    boolean existsByRegistrationNumberIgnoreCaseAndIdNot(String registrationNumber, Long id);
    boolean existsByBusNumberIgnoreCaseAndIdNot(String busNumber, Long id);
}
