package com.loyalty.mediation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Synchronous acknowledgement returned by the vendor's enrolment API.
 * A 200 with this body means the request was accepted for processing.
 * Actual completion is confirmed via the Solace event.
 */
@Data
@NoArgsConstructor
public class VendorEnrollmentResponse {
    private String message;
}
