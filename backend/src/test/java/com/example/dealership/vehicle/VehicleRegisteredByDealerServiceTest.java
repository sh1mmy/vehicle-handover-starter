package com.example.dealership.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleRegisteredByDealerServiceTest {

    private static final String VIN = "WVWZZZ1KZAW000001";

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void createSavesRegisteredByDealer() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.create(request(true));

        assertThat(response.registeredByDealer()).isTrue();
    }

    @Test
    void updateChangesRegisteredByDealer() {
        Vehicle vehicle = new Vehicle(VIN, "Volkswagen", "Golf", 2025, FuelType.PETROL, LocalDate.of(2026, 12, 15));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        VehicleResponse response = vehicleService.update(1L, request(true));

        assertThat(response.registeredByDealer()).isTrue();
        assertThat(vehicle.isRegisteredByDealer()).isTrue();
    }

    @Test
    void oldConstructorsDefaultRegisteredByDealerToFalse() {
        Vehicle vehicle = new Vehicle(VIN, "Volkswagen", "Golf", 2025, FuelType.PETROL, null);
        VehicleRequest request = new VehicleRequest(VIN, "Volkswagen", "Golf", 2025, FuelType.PETROL, null);
        VehicleResponse response = new VehicleResponse(1L, VIN, "Volkswagen", "Golf", 2025, FuelType.PETROL, null);

        assertThat(vehicle.isRegisteredByDealer()).isFalse();
        assertThat(request.registeredByDealer()).isFalse();
        assertThat(response.registeredByDealer()).isFalse();
    }

    private static VehicleRequest request(boolean registeredByDealer) {
        return new VehicleRequest(VIN, "Volkswagen", "Golf", 2025, FuelType.PETROL, LocalDate.of(2026, 12, 15),
                registeredByDealer);
    }
}
