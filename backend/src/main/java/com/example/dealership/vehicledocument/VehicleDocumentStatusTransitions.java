package com.example.dealership.vehicledocument;

/**
 * Allowed document status changes: a pending document is either verified or rejected.
 * A verified or rejected document never changes again; a correction is a new document (INTENT.md S1, Z3).
 */
public final class VehicleDocumentStatusTransitions {

    private VehicleDocumentStatusTransitions() {
    }

    public static boolean isAllowed(VehicleDocumentStatus from, VehicleDocumentStatus to) {
        return from == VehicleDocumentStatus.PENDING
                && (to == VehicleDocumentStatus.VERIFIED || to == VehicleDocumentStatus.REJECTED);
    }
}
