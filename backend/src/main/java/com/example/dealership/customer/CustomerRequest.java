package com.example.dealership.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 100)
        String email,

        @NotBlank
        @Pattern(
                regexp = "(?=.{9,20}$)\\+?[0-9]+([ -][0-9]+)*",
                message = "must be 9 to 20 characters: digits separated by single spaces or hyphens, optionally starting with +")
        String phone,

        @Pattern(regexp = "[0-9]{10}", message = "must be exactly 10 digits")
        String nip
) {
}
