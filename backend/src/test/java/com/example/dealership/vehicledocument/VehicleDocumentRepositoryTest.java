package com.example.dealership.vehicledocument;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Sort;

@DataJpaTest
class VehicleDocumentRepositoryTest {

    private static final String VIN = "WF0XXXGCDX0000010";
    private static final String OTHER_VIN = "WF0XXXGCDX0000011";

    @Autowired
    private VehicleDocumentRepository documentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findAllByVehicleIdReturnsOnlyChildrenOfVehicle() {
        Long vehicleId = vehicle(VIN);
        Long otherVehicleId = vehicle(OTHER_VIN);
        VehicleDocument first = documentRepository.save(document(vehicleId, VehicleDocumentType.INVOICE, "FV/1"));
        documentRepository.save(document(otherVehicleId, VehicleDocumentType.INVOICE, "FV/2"));
        VehicleDocument second = documentRepository.save(document(vehicleId, VehicleDocumentType.COC, "COC/1"));
        entityManager.flush();

        assertThat(documentRepository.findAllByVehicleId(vehicleId, Sort.by("id")))
                .extracting(VehicleDocument::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void findByIdAndVehicleIdIgnoresChildOfAnotherVehicle() {
        Long vehicleId = vehicle(VIN);
        Long otherVehicleId = vehicle(OTHER_VIN);
        VehicleDocument document = documentRepository.save(document(vehicleId, VehicleDocumentType.INVOICE, "FV/1"));
        entityManager.flush();

        assertThat(documentRepository.findByIdAndVehicleId(document.getId(), vehicleId)).isPresent();
        assertThat(documentRepository.findByIdAndVehicleId(document.getId(), otherVehicleId)).isEmpty();
    }

    @Test
    void saveStoresVehicleDocumentThatCanBeReadBack() {
        Long vehicleId = vehicle(VIN);
        VehicleDocument saved = documentRepository.save(new VehicleDocument(vehicleId, VehicleDocumentType.OC_POLICY,
                "OC/2026/0001", LocalDate.of(2026, 11, 18), LocalDate.of(2027, 11, 17)));
        entityManager.flush();
        entityManager.clear();

        VehicleDocument found = documentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found).isNotSameAs(saved);
        assertThat(found.getVehicleId()).isEqualTo(vehicleId);
        assertThat(found.getType()).isEqualTo(VehicleDocumentType.OC_POLICY);
        assertThat(found.getNumber()).isEqualTo("OC/2026/0001");
        assertThat(found.getIssueDate()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(found.getExpiryDate()).isEqualTo(LocalDate.of(2027, 11, 17));
        assertThat(found.getStatus()).isEqualTo(VehicleDocumentStatus.PENDING);
    }

    private Long vehicle(String vin) {
        return vehicleRepository.save(new Vehicle(vin, "Ford", "Focus", 2026, FuelType.ELECTRIC, null)).getId();
    }

    private static VehicleDocument document(Long vehicleId, VehicleDocumentType type, String number) {
        return new VehicleDocument(vehicleId, type, number, LocalDate.of(2026, 11, 1), null);
    }
}
