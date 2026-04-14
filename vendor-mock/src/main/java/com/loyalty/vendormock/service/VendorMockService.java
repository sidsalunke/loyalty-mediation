package com.loyalty.vendormock.service;

import com.loyalty.vendormock.messaging.SolaceEventPublisher;
import com.loyalty.vendormock.model.EnrollmentEvent;
import com.loyalty.vendormock.model.EnrollmentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorMockService {

    private final SolaceEventPublisher eventPublisher;

    @Value("${vendor-mock.event-delay-ms:1000}")
    private long eventDelayMs;

    /**
     * Simulate the vendor processing the enrolment asynchronously.
     * After a configurable delay, publishes a success event to Solace.
     */
    @Async
    public void processEnrolmentAsync(EnrollmentRequest request) {
        try {
            log.info("Vendor mock processing enrolment [correlationId={}] — event in {}ms",
                    request.getCorrelationId(), eventDelayMs);
            Thread.sleep(eventDelayMs);

            String membershipNumber = generateMembershipNumber();

            EnrollmentEvent event = new EnrollmentEvent(
                    "success",
                    membershipNumber,
                    request.getCorrelationId(),
                    "BLUE",
                    null
            );

            eventPublisher.publish(event);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Vendor mock processing interrupted [correlationId={}]", request.getCorrelationId());
        }
    }

    /**
     * Simulate a vendor-side failure event (triggered via error simulation header).
     */
    @Async
    public void processEnrolmentFailureAsync(EnrollmentRequest request, String reason) {
        try {
            Thread.sleep(eventDelayMs);

            EnrollmentEvent event = new EnrollmentEvent(
                    "failure",
                    null,
                    request.getCorrelationId(),
                    null,
                    reason
            );

            eventPublisher.publish(event);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** Generates a random 9-digit membership number as per vendor spec. */
    private String generateMembershipNumber() {
        // Ensure it's always 9 digits (100,000,000 – 999,999,999)
        long number = ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
        return String.valueOf(number);
    }
}
