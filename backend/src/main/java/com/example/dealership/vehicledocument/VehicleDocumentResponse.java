package com.example.dealership.vehicledocument;

import java.time.LocalDate;

public record VehicleDocumentResponse(
        Long id,
        Long vehicleId,
        VehicleDocumentType type,
        String number,
        LocalDate issueDate,
        LocalDate expiryDate,
        VehicleDocumentStatus status
) {
}
