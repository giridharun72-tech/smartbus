package com.smartbus.service;

import com.smartbus.dto.LiveBusLocationDTO;
import com.smartbus.dto.PhoneGpsLocationRequest;
import com.smartbus.dto.PublicBusLocationDTO;
import com.smartbus.entity.Bus;
import com.smartbus.entity.BusStatus;
import com.smartbus.entity.LocationSource;
import com.smartbus.repository.BusLocationRepository;
import com.smartbus.repository.BusRepository;
import com.smartbus.repository.DriverRepository;
import com.smartbus.repository.RouteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneGpsTrackingTest {

    @Mock
    private BusRepository busRepository;
    @Mock
    private BusLocationRepository busLocationRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private GpsSimulationService gpsSimulationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void phoneLocationIsSavedAndBroadcastAsPhoneGps() {
        Bus bus = new Bus();
        bus.setId(7L);
        bus.setBusNumber("21A");
        bus.setStatus(BusStatus.RUNNING);
        when(busRepository.findById(7L)).thenReturn(Optional.of(bus));
        when(busRepository.save(any(Bus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LiveBusLocationDTO liveLocation = new LiveBusLocationDTO();
        liveLocation.setBusId(7L);
        liveLocation.setBusNumber("21A");
        liveLocation.setLatitude(12.345);
        liveLocation.setLongitude(67.890);
        liveLocation.setSource(LocationSource.PHONE_GPS.name());
        liveLocation.setDriverPhone("private");
        when(gpsSimulationService.buildLocationDTO(bus)).thenReturn(liveLocation);

        PhoneGpsLocationRequest request = new PhoneGpsLocationRequest();
        request.setLatitude(12.345);
        request.setLongitude(67.890);
        request.setSpeed(18.5);

        AdminBusService service = new AdminBusService(busRepository, busLocationRepository, routeRepository,
                driverRepository, gpsSimulationService, messagingTemplate);
        service.updatePhoneLocation(7L, request);

        assertEquals(12.345, bus.getCurrentLatitude());
        assertEquals(67.890, bus.getCurrentLongitude());
        assertEquals(LocationSource.PHONE_GPS, bus.getLocationSource());
        verify(busLocationRepository).save(argThat(location -> location.getSource() == LocationSource.PHONE_GPS));
        verify(messagingTemplate).convertAndSend(eq("/topic/bus-location"), any(PublicBusLocationDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/bus-location/7"), any(PublicBusLocationDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/bus-location"),
                eq(new PublicBusLocationDTO(7L, "21A", null, 12.345, 67.890, null, null, null,
                        null, null, LocationSource.PHONE_GPS.name(), null)));
    }

    @Test
    void developmentHeartbeatDoesNotReplacePhoneGps() {
        Bus phoneTrackedBus = new Bus();
        phoneTrackedBus.setId(7L);
        phoneTrackedBus.setStatus(BusStatus.RUNNING);
        phoneTrackedBus.setLocationSource(LocationSource.PHONE_GPS);

        Bus simulatedBus = new Bus();
        simulatedBus.setId(8L);
        simulatedBus.setStatus(BusStatus.RUNNING);
        simulatedBus.setLocationSource(LocationSource.GPS);
        when(busRepository.findAll()).thenReturn(List.of(phoneTrackedBus, simulatedBus));

        new DevGpsHeartbeatService(busRepository, gpsSimulationService).sendDemoHeartbeats();

        verify(gpsSimulationService, never()).stepBusSimulation(7L);
        verify(gpsSimulationService).stepBusSimulation(8L);
    }
}
