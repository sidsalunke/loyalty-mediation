package com.loyalty.vendormock.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrollmentEvent {
    private String status;
    private String membershipNumber;
    private String correlationId;
    private String tier;
    private String errorMessage;
}
