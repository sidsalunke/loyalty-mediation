package com.loyalty.mediation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the event published by the vendor system to Solace
 * once a membership enrolment has been processed.
 */
@Data
@NoArgsConstructor
public class EnrollmentEvent {

    /** "success" or "failure" */
    private String status;

    /** 9-digit membership number assigned by the vendor (present on success) */
    private String membershipNumber;

    /** Matches the correlationId sent in the enrolment request */
    private String correlationId;

    /** Tier assigned — "BLUE" for new members */
    private String tier;

    /** Optional error detail (present on failure events) */
    private String errorMessage;
}
