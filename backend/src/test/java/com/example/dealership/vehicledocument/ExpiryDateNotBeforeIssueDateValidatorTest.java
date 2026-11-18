package com.example.dealership.vehicledocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;

class ExpiryDateNotBeforeIssueDateValidatorTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2025, 6, 15);

    private final ExpiryDateNotBeforeIssueDateValidator validator = new ExpiryDateNotBeforeIssueDateValidator();

    @Test
    void acceptsExpiryDateAfterIssueDate() {
        assertThat(validator.isValid(request(ISSUE_DATE, ISSUE_DATE.plusDays(1)), null)).isTrue();
    }

    @Test
    void acceptsExpiryDateEqualToIssueDate() {
        assertThat(validator.isValid(request(ISSUE_DATE, ISSUE_DATE), null)).isTrue();
    }

    @Test
    void rejectsExpiryDateBeforeIssueDate() {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);

        assertThat(validator.isValid(request(ISSUE_DATE, ISSUE_DATE.minusDays(1)), context)).isFalse();
    }

    @Test
    void acceptsMissingDates() {
        assertThat(validator.isValid(request(ISSUE_DATE, null), null)).isTrue();
        assertThat(validator.isValid(request(null, ISSUE_DATE), null)).isTrue();
        assertThat(validator.isValid(null, null)).isTrue();
    }

    private static VehicleDocumentRequest request(LocalDate issueDate, LocalDate expiryDate) {
        return new VehicleDocumentRequest(VehicleDocumentType.OC_POLICY, "OC/1", issueDate, expiryDate);
    }
}
