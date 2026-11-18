package com.example.dealership.handover;

import java.util.List;

import com.example.dealership.vehicledocument.VehicleDocumentType;

/** Response of {@code GET /api/vehicles/{vehicleId}/handover-readiness}, shaped as in the API contract. */
public record HandoverReadiness(
        boolean ready,
        List<VehicleDocumentType> missingDocumentTypes,
        List<HandoverProblem> problems
) {
}
