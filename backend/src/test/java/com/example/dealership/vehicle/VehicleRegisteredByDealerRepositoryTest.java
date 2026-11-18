package com.example.dealership.vehicle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class VehicleRegisteredByDealerRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveStoresRegisteredByDealer() {
        Vehicle saved = vehicleRepository.save(
                new Vehicle("WF0XXXGCDX0000030", "Ford", "Focus", 2026, FuelType.ELECTRIC, null, true));
        entityManager.flush();
        entityManager.clear();

        assertThat(vehicleRepository.findById(saved.getId()).orElseThrow().isRegisteredByDealer()).isTrue();
    }

    @Test
    void seededVehiclesDefaultToCustomerRegistering() {
        assertThat(vehicleRepository.findAll()).filteredOn(vehicle -> vehicle.getId() <= 3)
                .hasSize(3)
                .allSatisfy(vehicle -> assertThat(vehicle.isRegisteredByDealer()).isFalse());
    }
}
