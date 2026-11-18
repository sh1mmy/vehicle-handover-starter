package com.example.dealership.vehicledocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Not @Transactional: the delete must really flush and commit for the foreign key to fire.
@SpringBootTest
@AutoConfigureMockMvc
class VehicleDeleteWithVehicleDocumentsIntegrationTest {

    private static final String VIN = "WF0XXXGCDX0000020";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository documentRepository;

    private Long vehicleId;
    private Long documentId;

    @AfterEach
    void cleanUp() {
        if (documentId != null) {
            documentRepository.deleteById(documentId);
        }
        if (vehicleId != null) {
            vehicleRepository.deleteById(vehicleId);
        }
    }

    @Test
    void deleteReturnsConflictProblemWhenVehicleHasVehicleDocuments() throws Exception {
        vehicleId = vehicleRepository.save(new Vehicle(VIN, "Ford", "Kuga", 2026, FuelType.HYBRID, null)).getId();
        documentId = documentRepository.save(new VehicleDocument(vehicleId, VehicleDocumentType.INVOICE, "FV/2026/0020",
                LocalDate.of(2026, 11, 1), null)).getId();

        mockMvc.perform(delete("/api/vehicles/" + vehicleId))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request conflicts with existing data"));

        assertThat(vehicleRepository.existsById(vehicleId)).isTrue();
    }
}
