package com.example.dealership.vehicledocument;

import jakarta.validation.constraints.NotNull;

public record VehicleDocumentStatusRequest(
        @NotNull
        VehicleDocumentStatus status
) {
}
