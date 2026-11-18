package com.example.dealership.handover;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;
import com.example.dealership.vehicledocument.VehicleDocument;
import com.example.dealership.vehicledocument.VehicleDocumentRepository;
import com.example.dealership.vehicledocument.VehicleDocumentStatus;
import com.example.dealership.vehicledocument.VehicleDocumentType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// @Transactional is fine here: the endpoint only reads, and the rollback removes the test data.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HandoverReadinessIntegrationTest {

    private static final LocalDate HANDOVER = LocalDate.of(2030, 1, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository documentRepository;

    @Test
    void getReadinessCombinesStoredVehicleAndDocuments() throws Exception {
        Long vehicleId = vehicleRepository.save(
                new Vehicle("WF0XXXGCDX0000040", "Ford", "Kuga", 2026, FuelType.HYBRID, HANDOVER, true)).getId();
        save(vehicleId, VehicleDocumentType.SALES_AGREEMENT, VehicleDocumentStatus.VERIFIED, null);
        save(vehicleId, VehicleDocumentType.INVOICE, VehicleDocumentStatus.PENDING, null);
        save(vehicleId, VehicleDocumentType.COC, VehicleDocumentStatus.REJECTED, null);
        save(vehicleId, VehicleDocumentType.OC_POLICY, VehicleDocumentStatus.VERIFIED, HANDOVER.minusDays(1));

        mockMvc.perform(get("/api/vehicles/" + vehicleId + "/handover-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.missingDocumentTypes.length()").value(2))
                .andExpect(jsonPath("$.missingDocumentTypes[0]").value("COC"))
                .andExpect(jsonPath("$.missingDocumentTypes[1]").value("REGISTRATION_POA"))
                .andExpect(jsonPath("$.problems.length()").value(2))
                .andExpect(jsonPath("$.problems[0].documentType").value("INVOICE"))
                .andExpect(jsonPath("$.problems[0].reason").value("NOT_VERIFIED"))
                .andExpect(jsonPath("$.problems[1].documentType").value("OC_POLICY"))
                .andExpect(jsonPath("$.problems[1].reason").value("EXPIRED"));
    }

    @Test
    void getReadinessReportsMissingHandoverDateForSeededVehicle() throws Exception {
        mockMvc.perform(get("/api/vehicles/3/handover-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.missingDocumentTypes.length()").value(4))
                .andExpect(jsonPath("$.problems.length()").value(1))
                .andExpect(jsonPath("$.problems[0].documentType").value(nullValue()))
                .andExpect(jsonPath("$.problems[0].reason").value("NO_HANDOVER_DATE"));
    }

    private void save(Long vehicleId, VehicleDocumentType type, VehicleDocumentStatus status, LocalDate expiryDate) {
        VehicleDocument document = new VehicleDocument(vehicleId, type, type + "/1", LocalDate.of(2026, 1, 10),
                expiryDate);
        if (status != VehicleDocumentStatus.PENDING) {
            document.changeStatus(status);
        }
        documentRepository.save(document);
    }
}
