package com.smartbus.controller;

import com.smartbus.dto.BusPublicResponse;
import com.smartbus.entity.Bus;
import com.smartbus.entity.Driver;
import com.smartbus.entity.User;
import com.smartbus.entity.Role;
import com.smartbus.repository.BusRepository;
import com.smartbus.service.SearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusApiControllerTest {

    @Test
    void busEndpointsReturnPublicFieldsWithoutDriverCredentialsOrContactDetails() {
        BusRepository repository = mock(BusRepository.class);
        SearchService searchService = mock(SearchService.class);
        Bus bus = new Bus();
        bus.setId(7L);
        bus.setBusNumber("21A");
        bus.setRegistrationNumber("PRIVATE-REGISTRATION");
        bus.setDriver(new Driver(
                new User("private-user", "private@example.test", "hashed-password", Role.ROLE_DRIVER),
                "DRV-1", "Private Driver", "555-0101", "PRIVATE-LICENSE"));
        when(repository.findAll()).thenReturn(List.of(bus));

        BusApiController controller = new BusApiController(searchService, repository);
        BusPublicResponse response = controller.getAllBuses().getBody().get(0);

        assertThat(response.busNumber()).isEqualTo("21A");
        assertThat(response.toString())
                .doesNotContain("PRIVATE-REGISTRATION", "private@example.test", "555-0101", "PRIVATE-LICENSE");
    }
}
