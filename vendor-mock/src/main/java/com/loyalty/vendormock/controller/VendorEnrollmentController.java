package com.loyalty.vendormock.controller;

import com.loyalty.vendormock.model.EnrollmentRequest;
import com.loyalty.vendormock.service.VendorMockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simulates the vendor's loyalty management system enrolment API.
 *
 * Error simulation header (for testing):
 *   X-Simulate-Error: 400   → returns 400 Bad Request
 *   X-Simulate-Error: 422   → returns 422 Unprocessable Entity
 *   X-Simulate-Error: 500   → returns 500 Internal Server Error
 *
 * Without the header: returns 200 and asynchronously publishes a Solace success event.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorEnrollmentController {

    private final VendorMockService vendorMockService;

    @PostMapping("/enroll")
    public ResponseEntity<Map<String, String>> enroll(
            @RequestBody EnrollmentRequest request,
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulateError) {

        log.info("Vendor mock received enrolment request [correlationId={}, country={}]",
                request.getCorrelationId(), request.getCountry());

        if (simulateError != null) {
            return switch (simulateError.trim()) {
                case "400" -> {
                    log.warn("Simulating 400 Bad Request [correlationId={}]", request.getCorrelationId());
                    yield ResponseEntity.badRequest()
                            .body(Map.of("message", "Invalid request: missing or malformed field."));
                }
                case "422" -> {
                    log.warn("Simulating 422 Unprocessable Entity [correlationId={}]", request.getCorrelationId());
                    yield ResponseEntity.unprocessableEntity()
                            .body(Map.of("message", "Member already exists with the provided details."));
                }
                case "500" -> {
                    log.error("Simulating 500 Internal Server Error [correlationId={}]", request.getCorrelationId());
                    yield ResponseEntity.internalServerError()
                            .body(Map.of("message", "Vendor loyalty system encountered an internal error."));
                }
                default -> processNormally(request);
            };
        }

        return processNormally(request);
    }

    private ResponseEntity<Map<String, String>> processNormally(EnrollmentRequest request) {
        // Respond synchronously with acceptance, then asynchronously publish the Solace event
        vendorMockService.processEnrolmentAsync(request);
        return ResponseEntity.ok(Map.of("message", "Request accepted"));
    }
}
