package com.loyalty.mediation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload sent from the mediation layer to the vendor's enrolment API.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorEnrollmentRequest {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String country;
    private String correlationId;
}
