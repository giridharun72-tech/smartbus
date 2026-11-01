package com.smartbus.service;

import com.smartbus.entity.BusStatus;
import com.smartbus.entity.LocationSource;
import com.smartbus.repository.BusRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class DevGpsHeartbeatService {

    private final BusRepository busRepository;
    private final GpsSimulationService gpsSimulationService;

    public DevGpsHeartbeatService(BusRepository busRepository, GpsSimulationService gpsSimulationService) {
        this.busRepository = busRepository;
        this.gpsSimulationService = gpsSimulationService;
    }

    @Scheduled(fixedRate = 10000)
    public void sendDemoHeartbeats() {
        busRepository.findAll().stream()
                .filter(bus -> bus.getStatus() == BusStatus.RUNNING || bus.getStatus() == BusStatus.DELAYED)
                .filter(bus -> bus.getLocationSource() != LocationSource.PHONE_GPS)
                .forEach(bus -> gpsSimulationService.stepBusSimulation(bus.getId()));
    }
}