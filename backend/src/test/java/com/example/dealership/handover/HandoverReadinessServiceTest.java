package com.example.dealership.handover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;
import com.example.dealership.vehicledocument.VehicleDocument;
import com.example.dealership.vehicledocument.VehicleDocumentRepository;
import com.example.dealership.vehicledocument.VehicleDocumentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class HandoverReadinessServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDate TODAY = LocalDate.of(2025, 6, 15);

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleDocumentRepository documentRepository;

    private HandoverReadinessService readinessService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
        readinessService = new HandoverReadinessService(vehicleRepository, documentRepository, clock);
    }

    @Test
    void getReadinessComparesExpiryWithTodayFromClockWhenThereIsNoHandoverDate() {
        Vehicle vehicle = new Vehicle("WVWZZZ1KZAW000001", "Volkswagen", "Golf", 2025, FuelType.PETROL, null);
        VehicleDocument policy = new VehicleDocument(1L, VehicleDocumentType.OC_POLICY, "OC/1", TODAY.minusYears(1),
                TODAY.minusDays(1));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(documentRepository.findAllByVehicleId(1L, Sort.by("id"))).thenReturn(List.of(policy));

        HandoverReadiness readiness = readinessService.getReadiness(1L);

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.problems()).containsExactly(
                new HandoverProblem(VehicleDocumentType.OC_POLICY, HandoverProblemReason.NOT_VERIFIED),
                new HandoverProblem(VehicleDocumentType.OC_POLICY, HandoverProblemReason.EXPIRED),
                new HandoverProblem(null, HandoverProblemReason.NO_HANDOVER_DATE));
    }

    @Test
    void getReadinessThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readinessService.getReadiness(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
    }
}
