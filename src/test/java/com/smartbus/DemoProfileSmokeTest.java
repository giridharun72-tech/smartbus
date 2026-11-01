package com.smartbus;

import com.smartbus.repository.BusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("demo")
class DemoProfileSmokeTest {

    @Autowired
    private BusRepository busRepository;

    @Test
    void demoProfileSeedsSampleBuses() {
        assertTrue(busRepository.count() > 0);
    }
}
