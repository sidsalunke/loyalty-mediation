package com.loyalty.mediation.controller;

import com.loyalty.mediation.model.EnrollmentRequest;
import com.loyalty.mediation.model.EnrollmentResponse;
import com.loyalty.mediation.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * POST /api/v1/enroll
     *
     * Accepts an enrolment request from the UI, forwards it to the vendor system,
     * and returns an immediate acceptance acknowledgement.
     * Membership number is delivered asynchronously via Solace.
     */
    @PostMapping("/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.enroll(request);
        return ResponseEntity.ok(response);
    }
}
