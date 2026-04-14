package com.loyalty.vendormock.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class EnrollmentRequest {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String country;
    private String correlationId;
}
