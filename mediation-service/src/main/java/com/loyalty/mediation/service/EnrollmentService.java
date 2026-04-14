package com.loyalty.mediation.service;

import com.loyalty.mediation.client.VendorClient;
import com.loyalty.mediation.logging.SplunkLogger;
import com.loyalty.mediation.model.EnrollmentRequest;
import com.loyalty.mediation.model.EnrollmentResponse;
import com.loyalty.mediation.model.VendorEnrollmentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final VendorClient vendorClient;
    private final SplunkLogger splunkLogger;

    /**
     * Orchestrates the enrolment flow:
     * 1. Generate a correlation ID for end-to-end tracing.
     * 2. Call the vendor enrolment API.
     * 3. Return an acceptance response to the caller.
     *    (Completion is confirmed asynchronously via the Solace event.)
     */
    public EnrollmentResponse enroll(EnrollmentRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Starting enrolment [correlationId={}, country={}]", correlationId, request.getCountry());

        VendorEnrollmentRequest vendorRequest = new VendorEnrollmentRequest(
                request.getFirstName(),
                request.getLastName(),
                request.getDateOfBirth(),
                request.getCountry(),
                correlationId
        );

        // Throws VendorClientException on 4xx/5xx — handled by GlobalExceptionHandler
        vendorClient.enroll(vendorRequest);

        splunkLogger.info(
                "ENROLMENT_ACCEPTED",
                correlationId,
                Map.of(
                        "country", request.getCountry(),
                        "status", "ACCEPTED"
                )
        );

        log.info("Enrolment accepted by vendor [correlationId={}]", correlationId);
        return new EnrollmentResponse("Enrollment request accepted", correlationId);
    }
}
