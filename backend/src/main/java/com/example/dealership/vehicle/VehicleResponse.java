package com.example.dealership.vehicle;

import java.time.LocalDate;

public record VehicleResponse(
        Long id,
        String vin,
        String make,
        String model,
        Integer productionYear,
        FuelType fuelType,
        LocalDate plannedHandoverDate,
        boolean registeredByDealer
) {

    public VehicleResponse(Long id, String vin, String make, String model, Integer productionYear, FuelType fuelType,
            LocalDate plannedHandoverDate) {
        this(id, vin, make, model, productionYear, fuelType, plannedHandoverDate, false);
    }
}
