package com.smartbus.repository;

import com.smartbus.entity.Bus;
import com.smartbus.entity.Incident;
import com.smartbus.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByOrderByReportedAtDesc();
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByBus(Bus bus);
    Optional<Incident> findByBusAndTypeAndStatus(Bus bus, com.smartbus.entity.IncidentType type, IncidentStatus status);
    Optional<Incident> findByIncidentReference(String incidentReference);
    Optional<Incident> findFirstByBusAndStatusInOrderByReportedAtDesc(Bus bus, List<IncidentStatus> statuses);
}
