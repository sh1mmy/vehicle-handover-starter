package com.example.dealership.vehicledocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * The document rules end to end on the real database (INTENT.md R4, R6, R7), through the same HTTP API the
 * external tests use. @Transactional is fine here: the rules are checked by service queries, not by constraints at
 * commit, and the rollback removes the test data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VehicleDocumentRulesIntegrationTest {

    private static final String INVOICE_BODY = """
            {
              "type": "INVOICE",
              "number": "FV/2026/0050",
              "issueDate": "2026-11-01"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository documentRepository;

    private Long vehicleId;

    @BeforeEach
    void setUp() {
        vehicleId = vehicleRepository.save(
                new Vehicle("WF0XXXGCDX0000050", "Ford", "Puma", 2026, FuelType.PETROL, null)).getId();
    }

    @Test
    void createIgnoresStatusInRequestBodyAndStoresPending() throws Exception {
        String body = INVOICE_BODY.replace("\"type\": \"INVOICE\",", "\"type\": \"INVOICE\",\n  \"status\": \"VERIFIED\",");

        Long documentId = create(body);

        assertThat(body).contains("\"status\": \"VERIFIED\"");
        assertThat(documentRepository.findById(documentId).orElseThrow().getStatus())
                .isEqualTo(VehicleDocumentStatus.PENDING);
    }

    @Test
    void createReturnsConflictForSecondActiveDocumentOfSameType() throws Exception {
        create(INVOICE_BODY);

        mockMvc.perform(post(documentsUrl()).contentType(MediaType.APPLICATION_JSON).content(INVOICE_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("Vehicle with id " + vehicleId + " already has an active document of type INVOICE"));
    }

    @Test
    void changeStatusReturnsConflictWhenRejectedDocumentIsVerified() throws Exception {
        Long documentId = create(INVOICE_BODY);
        changeStatus(documentId, "REJECTED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        changeStatus(documentId, "VERIFIED")
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("Document with id " + documentId + " cannot change status from REJECTED to VERIFIED"));
    }

    @Test
    void createAcceptsNewDocumentOfTypeAfterRejectedOne() throws Exception {
        Long rejectedId = create(INVOICE_BODY);
        changeStatus(rejectedId, "REJECTED").andExpect(status().isOk());

        mockMvc.perform(post(documentsUrl()).contentType(MediaType.APPLICATION_JSON).content(INVOICE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    private Long create(String body) throws Exception {
        String response = mockMvc.perform(post(documentsUrl()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private ResultActions changeStatus(Long documentId, String newStatus) throws Exception {
        return mockMvc.perform(put(documentsUrl() + "/" + documentId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + newStatus + "\"}"));
    }

    private String documentsUrl() {
        return "/api/vehicles/" + vehicleId + "/documents";
    }
}
