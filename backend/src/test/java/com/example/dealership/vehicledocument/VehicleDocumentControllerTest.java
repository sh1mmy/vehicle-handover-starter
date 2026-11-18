package com.example.dealership.vehicledocument;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.example.dealership.config.ClockConfig;
import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VehicleDocumentController.class)
@Import({ClockConfig.class, VehicleDocumentControllerTest.FixedClockConfig.class})
class VehicleDocumentControllerTest {

    // Fixed "today" in the past: a 400 for the next day proves the validator uses the app Clock, not the system clock.
    private static final LocalDate TODAY = LocalDate.of(2025, 6, 15);

    private static final String VALID_BODY = """
            {
              "type": "OC_POLICY",
              "number": "OC/2026/0001",
              "issueDate": "2025-06-15",
              "expiryDate": "2026-06-14"
            }
            """;

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            ZoneId zone = ZoneId.of("Europe/Warsaw");
            return Clock.fixed(TODAY.atTime(12, 0).atZone(zone).toInstant(), zone);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleDocumentService documentService;

    @Test
    void findAllReturnsVehicleDocuments() throws Exception {
        when(documentService.findAll(1L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/vehicles/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].vehicleId").value(1))
                .andExpect(jsonPath("$[0].type").value("OC_POLICY"))
                .andExpect(jsonPath("$[0].number").value("OC/2026/0001"))
                .andExpect(jsonPath("$[0].issueDate").value("2025-06-15"))
                .andExpect(jsonPath("$[0].expiryDate").value("2026-06-14"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void findAllReturnsNotFoundProblemWhenVehicleDoesNotExist() throws Exception {
        when(documentService.findAll(99L)).thenThrow(new NotFoundException("Vehicle with id 99 not found"));

        mockMvc.perform(get("/api/vehicles/99/documents"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99 not found"));
    }

    @Test
    void findByIdReturnsVehicleDocument() throws Exception {
        when(documentService.findById(1L, 1L)).thenReturn(response());

        mockMvc.perform(get("/api/vehicles/1/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("OC/2026/0001"));
    }

    @Test
    void findByIdReturnsNotFoundProblemWhenVehicleDocumentDoesNotExist() throws Exception {
        when(documentService.findById(1L, 99L)).thenThrow(new NotFoundException("Document with id 99 not found"));

        mockMvc.perform(get("/api/vehicles/1/documents/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Document with id 99 not found"));
    }

    @Test
    void findByIdReturnsNotFoundProblemWhenVehicleDoesNotExist() throws Exception {
        when(documentService.findById(99L, 1L)).thenThrow(new NotFoundException("Vehicle with id 99 not found"));

        mockMvc.perform(get("/api/vehicles/99/documents/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99 not found"));
    }

    @Test
    void createReturnsCreatedVehicleDocument() throws Exception {
        when(documentService.create(eq(1L), any(VehicleDocumentRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/vehicles/1/documents/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReturnsNotFoundProblemWhenVehicleDoesNotExist() throws Exception {
        when(documentService.create(eq(99L), any(VehicleDocumentRequest.class)))
                .thenThrow(new NotFoundException("Vehicle with id 99 not found"));

        mockMvc.perform(post("/api/vehicles/99/documents").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99 not found"));
    }

    @Test
    void createReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(invalidBody()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("issueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a date in the past or in the present"))
                .andExpect(jsonPath("$.errors[1].field").value("number"))
                .andExpect(jsonPath("$.errors[1].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[2].field").value("type"))
                .andExpect(jsonPath("$.errors[2].message").value("must not be null"));

        verifyNoInteractions(documentService);
    }

    @Test
    void createAcceptsIssueDateOfToday() throws Exception {
        when(documentService.create(eq(1L), any(VehicleDocumentRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void createReturnsBadRequestProblemWithFieldErrorWhenIssueDateIsInTheFuture() throws Exception {
        String body = VALID_BODY.replace("\"issueDate\": \"2025-06-15\"", "\"issueDate\": \"2025-06-16\"");

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("issueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a date in the past or in the present"));

        verifyNoInteractions(documentService);
    }

    @Test
    void createReturnsEnglishValidationMessagesWhenAcceptLanguageIsPolish() throws Exception {
        String body = VALID_BODY.replace("OC/2026/0001", " ");

        mockMvc.perform(post("/api/vehicles/1/documents")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "pl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("number"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));

        verifyNoInteractions(documentService);
    }

    @Test
    void createReturnsBadRequestProblemWithFieldErrorWhenVehicleDocumentTypeIsUnknown() throws Exception {
        String body = VALID_BODY.replace("OC_POLICY", "PASSPORT");

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("type"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("must be one of: SALES_AGREEMENT, INVOICE, COC, OC_POLICY, REGISTRATION_POA"));

        verifyNoInteractions(documentService);
    }

    @Test
    void createReturnsBadRequestProblemWithFieldErrorWhenValueHasWrongType() throws Exception {
        String body = VALID_BODY.replace("\"2026-06-14\"", "\"next year\"");

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("expiryDate"))
                .andExpect(jsonPath("$.errors[0].message").value("has an invalid value"));

        verifyNoInteractions(documentService);
    }

    @Test
    void createReturnsBadRequestProblemWhenBodyIsNotJson() throws Exception {
        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].message").value("request body must be valid JSON"));

        verifyNoInteractions(documentService);
    }

    private static String invalidBody() {
        return """
                {
                  "number": "",
                  "issueDate": "2025-06-16"
                }
                """;
    }

    private static VehicleDocumentResponse response() {
        return new VehicleDocumentResponse(1L, 1L, VehicleDocumentType.OC_POLICY, "OC/2026/0001", TODAY,
                LocalDate.of(2026, 6, 14), VehicleDocumentStatus.PENDING);
    }
}
