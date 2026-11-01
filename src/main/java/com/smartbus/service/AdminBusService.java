package com.smartbus.service;

import com.smartbus.dto.AdminBusResponse;
import com.smartbus.dto.CreateBusRequest;
import com.smartbus.dto.LiveBusLocationDTO;
import com.smartbus.dto.PhoneGpsLocationRequest;
import com.smartbus.entity.Bus;
import com.smartbus.entity.BusStatus;
import com.smartbus.entity.BusLocation;
import com.smartbus.entity.Driver;
import com.smartbus.entity.LocationSource;
import com.smartbus.entity.Route;
import com.smartbus.repository.BusRepository;
import com.smartbus.repository.BusLocationRepository;
import com.smartbus.repository.DriverRepository;
import com.smartbus.repository.RouteRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AdminBusService {
    private final BusRepository busRepository;
    private final BusLocationRepository busLocationRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final GpsSimulationService gpsSimulationService;
    private final SimpMessagingTemplate messagingTemplate;

    public AdminBusService(BusRepository busRepository,
                           BusLocationRepository busLocationRepository,
                           RouteRepository routeRepository,
                           DriverRepository driverRepository,
                           GpsSimulationService gpsSimulationService,
                           SimpMessagingTemplate messagingTemplate) {
        this.busRepository = busRepository;
        this.busLocationRepository = busLocationRepository;
        this.routeRepository = routeRepository;
        this.driverRepository = driverRepository;
        this.gpsSimulationService = gpsSimulationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public AdminBusResponse addBus(CreateBusRequest request) {
        String busNumber = request.getBusNumber().trim();
        if (busRepository.findFirstByBusNumberIgnoreCase(busNumber).isPresent()) {
            throw new BusAlreadyExistsException(busNumber);
        }
        if (request.getCurrentLatitude() == 0.0 || request.getCurrentLongitude() == 0.0) {
            throw new IllegalArgumentException("A non-zero device latitude and longitude are required.");
        }

        String routeName = request.getRoute().trim();
        Route route = routeRepository.findFirstByNameIgnoreCase(routeName)
                .orElseGet(() -> createRoute(routeName));
        Driver driver = driverRepository.findFirstByFullNameIgnoreCase(request.getDriverName().trim())
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();

        Bus bus = new Bus();
        bus.setBusNumber(busNumber);
        bus.setRegistrationNumber("SB-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        bus.setBusType("STANDARD");
        bus.setCapacity(request.getTotalSeats());
        bus.setRoute(route);
        bus.setDriver(driver);
        bus.setAssignedDriverName(request.getDriverName().trim());
        bus.setGpsDeviceId("GPS-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        bus.setCurrentLatitude(request.getCurrentLatitude());
        bus.setCurrentLongitude(request.getCurrentLongitude());
        bus.setCurrentSpeed(0.0);
        bus.setNextStopName("Initial device location");
        bus.setDistanceToNextStopKm(0.0);
        bus.setEtaMinutes(0);
        bus.setStatus(request.getStatus() != null ? request.getStatus() : BusStatus.RUNNING);
        bus.setLastGpsUpdate(now);
        bus.setLocationSource(LocationSource.GPS);

        Bus savedBus = busRepository.saveAndFlush(bus);
        LiveBusLocationDTO fleetUpdate = gpsSimulationService.buildLocationDTO(savedBus);
        messagingTemplate.convertAndSend("/topic/fleet-updates", fleetUpdate);
        return toResponse(savedBus);
    }

    @Transactional
    public LiveBusLocationDTO updatePhoneLocation(Long busId, PhoneGpsLocationRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found."));
        LocalDateTime now = LocalDateTime.now();
        double speed = request.getSpeed() != null && Double.isFinite(request.getSpeed())
                ? Math.max(0.0, request.getSpeed()) : 0.0;

        bus.setCurrentLatitude(request.getLatitude());
        bus.setCurrentLongitude(request.getLongitude());
        bus.setCurrentSpeed(speed);
        bus.setLastGpsUpdate(now);
        bus.setLocationSource(LocationSource.PHONE_GPS);
        if (bus.getStatus() == BusStatus.GPS_SIGNAL_LOST
                || bus.getStatus() == BusStatus.MANUAL_LOCATION_MODE) {
            bus.setStatus(BusStatus.RUNNING);
        }
        Bus savedBus = busRepository.save(bus);
        busLocationRepository.save(new BusLocation(savedBus, request.getLatitude(), request.getLongitude(),
                speed, 0.0, now, LocationSource.PHONE_GPS, savedBus.getNextStopName(),
                savedBus.getDistanceToNextStopKm(), savedBus.getEtaMinutes()));

        LiveBusLocationDTO location = gpsSimulationService.buildLocationDTO(savedBus);
        var publicLocation = com.smartbus.dto.PublicBusLocationDTO.from(location);
        messagingTemplate.convertAndSend("/topic/bus-location", publicLocation);
        messagingTemplate.convertAndSend("/topic/bus-location/" + savedBus.getId(), publicLocation);
        return location;
    }

    private Route createRoute(String routeName) {
        String[] endpoints = routeName.split("\\s+-\\s+", 2);
        String source = endpoints[0].trim();
        String destination = endpoints.length > 1 ? endpoints[1].trim() : source;
        String routeNumber = "NEW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return routeRepository.save(new Route(routeNumber, routeName, source, destination, 0.0, 0));
    }

    private AdminBusResponse toResponse(Bus bus) {
        AdminBusResponse response = new AdminBusResponse();
        response.setId(bus.getId());
        response.setBusNumber(bus.getBusNumber());
        response.setRegistrationNumber(bus.getRegistrationNumber());
        response.setBusType(bus.getBusType());
        response.setCapacity(bus.getCapacity());
        response.setRouteName(bus.getRoute() != null ? bus.getRoute().getName() : null);
        response.setDriverName(bus.getDriver() != null ? bus.getDriver().getFullName() : bus.getAssignedDriverName());
        response.setGpsDeviceId(bus.getGpsDeviceId());
        response.setStatus(bus.getStatus().name());
        response.setLatitude(bus.getCurrentLatitude());
        response.setLongitude(bus.getCurrentLongitude());
        return response;
    }
}
