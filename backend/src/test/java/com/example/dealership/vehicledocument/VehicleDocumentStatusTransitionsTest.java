package com.example.dealership.vehicledocument;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VehicleDocumentStatusTransitionsTest {

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @CsvSource({
            "PENDING, VERIFIED, true",
            "PENDING, REJECTED, true",
            "PENDING, PENDING, false",
            "VERIFIED, VERIFIED, false",
            "VERIFIED, REJECTED, true",
            "VERIFIED, PENDING, false",
            "REJECTED, REJECTED, false",
            "REJECTED, VERIFIED, false",
            "REJECTED, PENDING, false"
    })
    void allowsVerifyingOrRejectingPendingAndRejectingVerifiedDocument(VehicleDocumentStatus from, VehicleDocumentStatus to,
            boolean allowed) {
        assertThat(VehicleDocumentStatusTransitions.isAllowed(from, to)).isEqualTo(allowed);
    }
}
