package com.example.dealership.handover;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.dealership.vehicledocument.VehicleDocument;
import com.example.dealership.vehicledocument.VehicleDocumentStatus;
import com.example.dealership.vehicledocument.VehicleDocumentType;

/**
 * Handover readiness rules (INTENT.md R10-R17), as plain code without Spring.
 *
 * <ul>
 *     <li>Required: sales agreement, invoice, COC, OC policy; the registration power of attorney only when the
 *     dealer registers the vehicle.</li>
 *     <li>A required type without an active (non-rejected) document is missing.</li>
 *     <li>An active pending document is NOT_VERIFIED.</li>
 *     <li>An active document expiring before the handover date is EXPIRED; expiring on that day is fine. Without a
 *     handover date, expiry is compared with today (Z4).</li>
 *     <li>No handover date adds NO_HANDOVER_DATE with documentType null, so the vehicle is never ready.</li>
 *     <li>Order (Z7): types in enum order, NOT_VERIFIED before EXPIRED, NO_HANDOVER_DATE last.</li>
 * </ul>
 */
public final class HandoverReadinessCheck {

    private static final List<VehicleDocumentType> ALWAYS_REQUIRED = List.of(
            VehicleDocumentType.SALES_AGREEMENT,
            VehicleDocumentType.INVOICE,
            VehicleDocumentType.COC,
            VehicleDocumentType.OC_POLICY);

    private HandoverReadinessCheck() {
    }

    public static HandoverReadiness evaluate(boolean registeredByDealer, LocalDate plannedHandoverDate,
            List<VehicleDocument> documents, LocalDate today) {
        LocalDate validOn = plannedHandoverDate != null ? plannedHandoverDate : today;
        List<VehicleDocumentType> missing = new ArrayList<>();
        List<HandoverProblem> problems = new ArrayList<>();

        for (VehicleDocumentType type : requiredTypes(registeredByDealer)) {
            Optional<VehicleDocument> active = activeDocument(documents, type);
            if (active.isEmpty()) {
                missing.add(type);
                continue;
            }
            VehicleDocument document = active.get();
            if (document.getStatus() == VehicleDocumentStatus.PENDING) {
                problems.add(new HandoverProblem(type, HandoverProblemReason.NOT_VERIFIED));
            }
            if (document.getExpiryDate() != null && document.getExpiryDate().isBefore(validOn)) {
                problems.add(new HandoverProblem(type, HandoverProblemReason.EXPIRED));
            }
        }
        if (plannedHandoverDate == null) {
            problems.add(new HandoverProblem(null, HandoverProblemReason.NO_HANDOVER_DATE));
        }

        return new HandoverReadiness(missing.isEmpty() && problems.isEmpty(), List.copyOf(missing),
                List.copyOf(problems));
    }

    private static List<VehicleDocumentType> requiredTypes(boolean registeredByDealer) {
        List<VehicleDocumentType> required = new ArrayList<>();
        for (VehicleDocumentType type : VehicleDocumentType.values()) {
            if (ALWAYS_REQUIRED.contains(type)
                    || (registeredByDealer && type == VehicleDocumentType.REGISTRATION_POA)) {
                required.add(type);
            }
        }
        return required;
    }

    private static Optional<VehicleDocument> activeDocument(List<VehicleDocument> documents, VehicleDocumentType type) {
        return documents.stream()
                .filter(document -> document.getType() == type)
                .filter(document -> document.getStatus() != VehicleDocumentStatus.REJECTED)
                .findFirst();
    }
}
