package com.example.dealership.vehicledocument;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record VehicleDocumentRequest(
        @NotNull
        VehicleDocumentType type,

        @NotBlank
        @Size(max = 50)
        String number,

        @NotNull
        @PastOrPresent
        LocalDate issueDate,

        LocalDate expiryDate
) {
}
