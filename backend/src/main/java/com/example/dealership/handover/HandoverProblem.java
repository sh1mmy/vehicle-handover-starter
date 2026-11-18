package com.example.dealership.handover;

import com.example.dealership.vehicledocument.VehicleDocumentType;

/** {@code documentType} is null for problems of the vehicle itself, such as a missing handover date. */
public record HandoverProblem(
        VehicleDocumentType documentType,
        HandoverProblemReason reason
) {
}
