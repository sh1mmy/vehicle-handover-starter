package com.example.dealership.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** registeredByDealer is optional in the request: bodies from before the field existed stay valid (INTENT.md §7, §8). */
@WebMvcTest(VehicleController.class)
class VehicleRegisteredByDealerControllerTest {

    private static final String BODY_WITHOUT_FLAG = """
            {
              "vin": "WVWZZZ1KZAW000001",
              "make": "Volkswagen",
              "model": "Golf",
              "productionYear": 2025,
              "fuelType": "PETROL",
              "plannedHandoverDate": "2026-12-15"
            }
            """;

    private static final String BODY_WITH_FLAG = """
            {
              "vin": "WVWZZZ1KZAW000001",
              "make": "Volkswagen",
              "model": "Golf",
              "productionYear": 2025,
              "fuelType": "PETROL",
              "plannedHandoverDate": "2026-12-15",
              "registeredByDealer": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;

    @Test
    void createDefaultsRegisteredByDealerToFalseWhenFieldIsMissing() throws Exception {
        ArgumentCaptor<VehicleRequest> captor = ArgumentCaptor.forClass(VehicleRequest.class);
        when(vehicleService.create(captor.capture())).thenReturn(response(false));

        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(BODY_WITHOUT_FLAG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registeredByDealer").value(false));

        assertThat(captor.getValue().registeredByDealer()).isFalse();
    }

    @Test
    void createDefaultsRegisteredByDealerToFalseWhenFieldIsNull() throws Exception {
        ArgumentCaptor<VehicleRequest> captor = ArgumentCaptor.forClass(VehicleRequest.class);
        when(vehicleService.create(captor.capture())).thenReturn(response(false));
        String body = BODY_WITH_FLAG.replace("\"registeredByDealer\": true", "\"registeredByDealer\": null");

        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        assertThat(captor.getValue().registeredByDealer()).isFalse();
    }

    @Test
    void createPassesRegisteredByDealerToService() throws Exception {
        ArgumentCaptor<VehicleRequest> captor = ArgumentCaptor.forClass(VehicleRequest.class);
        when(vehicleService.create(captor.capture())).thenReturn(response(true));

        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(BODY_WITH_FLAG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registeredByDealer").value(true));

        assertThat(captor.getValue().registeredByDealer()).isTrue();
    }

    @Test
    void updatePassesRegisteredByDealerToService() throws Exception {
        ArgumentCaptor<VehicleRequest> captor = ArgumentCaptor.forClass(VehicleRequest.class);
        when(vehicleService.update(eq(1L), captor.capture())).thenReturn(response(true));

        mockMvc.perform(put("/api/vehicles/1").contentType(MediaType.APPLICATION_JSON).content(BODY_WITH_FLAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredByDealer").value(true));

        assertThat(captor.getValue().registeredByDealer()).isTrue();
    }

    @Test
    void findByIdReturnsRegisteredByDealer() throws Exception {
        when(vehicleService.findById(1L)).thenReturn(response(true));

        mockMvc.perform(get("/api/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredByDealer").value(true));

        verify(vehicleService).findById(1L);
    }

    private static VehicleResponse response(boolean registeredByDealer) {
        return new VehicleResponse(1L, "WVWZZZ1KZAW000001", "Volkswagen", "Golf", 2025, FuelType.PETROL,
                LocalDate.of(2026, 12, 15), registeredByDealer);
    }
}
