package com.example.dealership.vehicledocument;

/**
 * Allowed document status changes (INTENT.md R6, Z3):
 * <ul>
 *     <li>a pending document is verified or rejected;</li>
 *     <li>a verified document can still be rejected, e.g. an expired policy that is replaced by a new one;</li>
 *     <li>a rejected document never changes again; a correction is a new document (S1).</li>
 * </ul>
 */
public final class VehicleDocumentStatusTransitions {

    private VehicleDocumentStatusTransitions() {
    }

    public static boolean isAllowed(VehicleDocumentStatus from, VehicleDocumentStatus to) {
        return switch (from) {
            case PENDING -> to == VehicleDocumentStatus.VERIFIED || to == VehicleDocumentStatus.REJECTED;
            case VERIFIED -> to == VehicleDocumentStatus.REJECTED;
            case REJECTED -> false;
        };
    }
}
