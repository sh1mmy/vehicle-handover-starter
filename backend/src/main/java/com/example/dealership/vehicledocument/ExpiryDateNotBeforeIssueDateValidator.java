package com.example.dealership.vehicledocument;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExpiryDateNotBeforeIssueDateValidator
        implements ConstraintValidator<ExpiryDateNotBeforeIssueDate, VehicleDocumentRequest> {

    @Override
    public boolean isValid(VehicleDocumentRequest request, ConstraintValidatorContext context) {
        if (request == null || request.issueDate() == null || request.expiryDate() == null) {
            return true;
        }
        if (!request.expiryDate().isBefore(request.issueDate())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("expiryDate")
                .addConstraintViolation();
        return false;
    }
}
