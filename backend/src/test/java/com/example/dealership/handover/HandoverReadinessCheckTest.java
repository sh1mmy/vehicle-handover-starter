package com.example.dealership.handover;

import static com.example.dealership.handover.HandoverProblemReason.EXPIRED;
import static com.example.dealership.handover.HandoverProblemReason.NOT_VERIFIED;
import static com.example.dealership.handover.HandoverProblemReason.NO_HANDOVER_DATE;
import static com.example.dealership.vehicledocument.VehicleDocumentStatus.PENDING;
import static com.example.dealership.vehicledocument.VehicleDocumentStatus.REJECTED;
import static com.example.dealership.vehicledocument.VehicleDocumentStatus.VERIFIED;
import static com.example.dealership.vehicledocument.VehicleDocumentType.COC;
import static com.example.dealership.vehicledocument.VehicleDocumentType.INVOICE;
import static com.example.dealership.vehicledocument.VehicleDocumentType.OC_POLICY;
import static com.example.dealership.vehicledocument.VehicleDocumentType.REGISTRATION_POA;
import static com.example.dealership.vehicledocument.VehicleDocumentType.SALES_AGREEMENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.dealership.vehicledocument.VehicleDocument;
import com.example.dealership.vehicledocument.VehicleDocumentStatus;
import com.example.dealership.vehicledocument.VehicleDocumentType;

import org.junit.jupiter.api.Test;

class HandoverReadinessCheckTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);
    private static final LocalDate HANDOVER = LocalDate.of(2026, 12, 15);

    @Test
    void isReadyWhenAllRequiredDocumentsAreVerifiedAndValid() {
        HandoverReadiness readiness = evaluate(false, HANDOVER, allRequiredVerified());

        assertThat(readiness.ready()).isTrue();
        assertThat(readiness.missingDocumentTypes()).isEmpty();
        assertThat(readiness.problems()).isEmpty();
    }

    @Test
    void reportsAllRequiredTypesAsMissingWhenVehicleHasNoDocuments() {
        HandoverReadiness readiness = evaluate(false, HANDOVER, List.of());

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.missingDocumentTypes()).containsExactly(SALES_AGREEMENT, INVOICE, COC, OC_POLICY);
        assertThat(readiness.problems()).isEmpty();
    }

    @Test
    void requiresPowerOfAttorneyWhenDealerRegistersVehicle() {
        HandoverReadiness readiness = evaluate(true, HANDOVER, allRequiredVerified());

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.missingDocumentTypes()).containsExactly(REGISTRATION_POA);
    }

    @Test
    void isReadyWithVerifiedPowerOfAttorneyWhenDealerRegistersVehicle() {
        List<VehicleDocument> documents = allRequiredVerified();
        documents.add(document(REGISTRATION_POA, VERIFIED, HANDOVER.plusMonths(1)));

        assertThat(evaluate(true, HANDOVER, documents).ready()).isTrue();
    }

    @Test
    void ignoresPowerOfAttorneyWhenCustomerRegistersVehicle() {
        List<VehicleDocument> documents = allRequiredVerified();
        documents.add(document(REGISTRATION_POA, PENDING, HANDOVER.minusDays(10)));

        HandoverReadiness readiness = evaluate(false, HANDOVER, documents);

        assertThat(readiness.ready()).isTrue();
        assertThat(readiness.problems()).isEmpty();
    }

    @Test
    void treatsRejectedDocumentAsMissing() {
        List<VehicleDocument> documents = allRequiredVerifiedExcept(COC);
        documents.add(document(COC, REJECTED, null));

        HandoverReadiness readiness = evaluate(false, HANDOVER, documents);

        assertThat(readiness.missingDocumentTypes()).containsExactly(COC);
        assertThat(readiness.problems()).isEmpty();
    }

    @Test
    void usesActiveDocumentWhenRejectedDocumentOfSameTypeExists() {
        List<VehicleDocument> documents = allRequiredVerified();
        documents.add(0, document(INVOICE, REJECTED, null));

        assertThat(evaluate(false, HANDOVER, documents).ready()).isTrue();
    }

    @Test
    void reportsPendingDocumentAsNotVerified() {
        List<VehicleDocument> documents = allRequiredVerifiedExcept(INVOICE);
        documents.add(document(INVOICE, PENDING, null));

        HandoverReadiness readiness = evaluate(false, HANDOVER, documents);

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.missingDocumentTypes()).isEmpty();
        assertThat(readiness.problems()).containsExactly(new HandoverProblem(INVOICE, NOT_VERIFIED));
    }

    @Test
    void reportsBothNotVerifiedAndExpiredForPendingExpiredDocument() {
        List<VehicleDocument> documents = allRequiredVerifiedExcept(OC_POLICY);
        documents.add(document(OC_POLICY, PENDING, HANDOVER.minusDays(1)));

        assertThat(evaluate(false, HANDOVER, documents).problems()).containsExactly(
                new HandoverProblem(OC_POLICY, NOT_VERIFIED),
                new HandoverProblem(OC_POLICY, EXPIRED));
    }

    @Test
    void acceptsDocumentExpiringOnHandoverDate() {
        List<VehicleDocument> documents = allRequiredVerifiedExcept(OC_POLICY);
        documents.add(document(OC_POLICY, VERIFIED, HANDOVER));

        assertThat(evaluate(false, HANDOVER, documents).ready()).isTrue();
    }

    @Test
    void reportsDocumentExpiringDayBeforeHandoverAsExpired() {
        List<VehicleDocument> documents = allRequiredVerifiedExcept(OC_POLICY);
        documents.add(document(OC_POLICY, VERIFIED, HANDOVER.minusDays(1)));

        assertThat(evaluate(false, HANDOVER, documents).problems())
                .containsExactly(new HandoverProblem(OC_POLICY, EXPIRED));
    }

    @Test
    void neverReportsDocumentWithoutExpiryDateAsExpired() {
        List<VehicleDocument> documents = allRequiredVerifiedExcept(OC_POLICY);
        documents.add(document(OC_POLICY, VERIFIED, null));

        assertThat(evaluate(false, LocalDate.of(2099, 1, 1), documents).ready()).isTrue();
    }

    @Test
    void isNotReadyWithoutHandoverDateEvenWithAllDocumentsVerified() {
        HandoverReadiness readiness = evaluate(false, null, allRequiredVerified());

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.missingDocumentTypes()).isEmpty();
        assertThat(readiness.problems()).containsExactly(new HandoverProblem(null, NO_HANDOVER_DATE));
    }

    @Test
    void comparesExpiryWithTodayWhenThereIsNoHandoverDate() {
        List<VehicleDocument> documents = allRequiredVerifiedExcept(OC_POLICY);
        documents.add(document(OC_POLICY, VERIFIED, TODAY.minusDays(1)));

        assertThat(evaluate(false, null, documents).problems()).containsExactly(
                new HandoverProblem(OC_POLICY, EXPIRED),
                new HandoverProblem(null, NO_HANDOVER_DATE));
    }

    @Test
    void acceptsDocumentValidTodayWhenThereIsNoHandoverDate() {
        List<VehicleDocument> documents = allRequiredVerifiedExcept(OC_POLICY);
        documents.add(document(OC_POLICY, VERIFIED, TODAY));

        assertThat(evaluate(false, null, documents).problems())
                .containsExactly(new HandoverProblem(null, NO_HANDOVER_DATE));
    }

    @Test
    void comparesExpiryWithHandoverDateEvenWhenItIsInThePast() {
        LocalDate pastHandover = TODAY.minusDays(30);
        List<VehicleDocument> documents = allRequiredVerifiedExcept(OC_POLICY);
        documents.add(document(OC_POLICY, VERIFIED, pastHandover.plusDays(1)));

        assertThat(evaluate(false, pastHandover, documents).ready()).isTrue();
    }

    @Test
    void ordersMissingTypesAndProblemsByDocumentTypeThenReason() {
        // No handover date: expiry is compared with today, so the policy must expire before TODAY.
        List<VehicleDocument> documents = new ArrayList<>(List.of(
                document(OC_POLICY, PENDING, TODAY.minusDays(1)),
                document(INVOICE, PENDING, null)));

        HandoverReadiness readiness = evaluate(true, null, documents);

        assertThat(readiness.missingDocumentTypes()).containsExactly(SALES_AGREEMENT, COC, REGISTRATION_POA);
        assertThat(readiness.problems()).containsExactly(
                new HandoverProblem(INVOICE, NOT_VERIFIED),
                new HandoverProblem(OC_POLICY, NOT_VERIFIED),
                new HandoverProblem(OC_POLICY, EXPIRED),
                new HandoverProblem(null, NO_HANDOVER_DATE));
    }

    private static HandoverReadiness evaluate(boolean registeredByDealer, LocalDate handoverDate,
            List<VehicleDocument> documents) {
        return HandoverReadinessCheck.evaluate(registeredByDealer, handoverDate, documents, TODAY);
    }

    private static List<VehicleDocument> allRequiredVerified() {
        return allRequiredVerifiedExcept(null);
    }

    private static List<VehicleDocument> allRequiredVerifiedExcept(VehicleDocumentType skipped) {
        List<VehicleDocument> documents = new ArrayList<>();
        for (VehicleDocumentType type : List.of(SALES_AGREEMENT, INVOICE, COC, OC_POLICY)) {
            if (type != skipped) {
                documents.add(document(type, VERIFIED, type == OC_POLICY ? HANDOVER.plusYears(1) : null));
            }
        }
        return documents;
    }

    private static VehicleDocument document(VehicleDocumentType type, VehicleDocumentStatus status,
            LocalDate expiryDate) {
        VehicleDocument document = new VehicleDocument(1L, type, type + "/1", LocalDate.of(2026, 11, 1), expiryDate);
        if (status != PENDING) {
            document.changeStatus(status);
        }
        return document;
    }
}
