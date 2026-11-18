package com.example.dealership.vehicledocument;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Cross-field rule reported on {@code expiryDate}, so the 400 has the usual {@code errors[{field}]} shape. */
@Documented
@Constraint(validatedBy = ExpiryDateNotBeforeIssueDateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpiryDateNotBeforeIssueDate {

    String message() default "must not be before issue date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
