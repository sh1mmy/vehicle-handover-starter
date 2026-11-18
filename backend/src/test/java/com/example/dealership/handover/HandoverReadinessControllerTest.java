package com.example.dealership.handover;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicledocument.VehicleDocumentType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HandoverReadinessController.class)
class HandoverReadinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HandoverReadinessService readinessService;

    @Test
    void getReadinessReturnsContractShape() throws Exception {
        when(readinessService.getReadiness(1L)).thenReturn(new HandoverReadiness(false,
                List.of(VehicleDocumentType.COC),
                List.of(new HandoverProblem(VehicleDocumentType.OC_POLICY, HandoverProblemReason.EXPIRED),
                        new HandoverProblem(null, HandoverProblemReason.NO_HANDOVER_DATE))));

        mockMvc.perform(get("/api/vehicles/1/handover-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.missingDocumentTypes.length()").value(1))
                .andExpect(jsonPath("$.missingDocumentTypes[0]").value("COC"))
                .andExpect(jsonPath("$.problems.length()").value(2))
                .andExpect(jsonPath("$.problems[0].documentType").value("OC_POLICY"))
                .andExpect(jsonPath("$.problems[0].reason").value("EXPIRED"))
                .andExpect(jsonPath("$.problems[1].documentType").value(nullValue()))
                .andExpect(jsonPath("$.problems[1].reason").value("NO_HANDOVER_DATE"));
    }

    @Test
    void getReadinessReturnsNotFoundProblemWhenVehicleDoesNotExist() throws Exception {
        when(readinessService.getReadiness(99L)).thenThrow(new NotFoundException("Vehicle with id 99 not found"));

        mockMvc.perform(get("/api/vehicles/99/handover-readiness"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99 not found"));
    }
}
