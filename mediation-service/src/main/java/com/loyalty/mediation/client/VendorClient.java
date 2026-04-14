package com.loyalty.mediation.client;

import com.loyalty.mediation.exception.VendorClientException;
import com.loyalty.mediation.model.VendorEnrollmentRequest;
import com.loyalty.mediation.model.VendorEnrollmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for the vendor's loyalty management enrolment API.
 */
@Slf4j
@Component
public class VendorClient {

    private final RestTemplate restTemplate;
    private final String vendorBaseUrl;

    public VendorClient(
            RestTemplate restTemplate,
            @Value("${vendor.base-url}") String vendorBaseUrl) {
        this.restTemplate = restTemplate;
        this.vendorBaseUrl = vendorBaseUrl;
    }

    /**
     * Submit a membership enrolment to the vendor system.
     *
     * @param request the enrolment payload
     * @return the vendor's acknowledgement response
     * @throws VendorClientException on 4xx or 5xx from the vendor
     */
    public VendorEnrollmentResponse enroll(VendorEnrollmentRequest request) {
        String url = vendorBaseUrl + "/api/v1/vendor/enroll";
        log.debug("Calling vendor enrolment API [url={}, correlationId={}]", url, request.getCorrelationId());

        try {
            ResponseEntity<VendorEnrollmentResponse> response =
                    restTemplate.postForEntity(url, request, VendorEnrollmentResponse.class);
            return response.getBody();

        } catch (HttpClientErrorException ex) {
            String body = ex.getResponseBodyAsString();
            log.warn("Vendor returned client error [status={}, correlationId={}, body={}]",
                    ex.getStatusCode(), request.getCorrelationId(), body);
            throw new VendorClientException(ex.getStatusCode().value(), extractMessage(body, ex.getMessage()));

        } catch (HttpServerErrorException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Vendor returned server error [status={}, correlationId={}, body={}]",
                    ex.getStatusCode(), request.getCorrelationId(), body);
            throw new VendorClientException(HttpStatus.BAD_GATEWAY.value(),
                    "Vendor service is temporarily unavailable. Please try again later.");

        } catch (Exception ex) {
            log.error("Failed to reach vendor enrolment API [correlationId={}]",
                    request.getCorrelationId(), ex);
            throw new VendorClientException(HttpStatus.BAD_GATEWAY.value(),
                    "Could not connect to the vendor service. Please try again later.");
        }
    }

    private String extractMessage(String responseBody, String fallback) {
        // Attempt to pull the "message" field from a simple JSON body
        if (responseBody != null && responseBody.contains("\"message\"")) {
            int start = responseBody.indexOf("\"message\"") + 11;
            int end = responseBody.indexOf("\"", start + 1);
            if (start > 10 && end > start) {
                return responseBody.substring(start + 1, end);
            }
        }
        return fallback;
    }
}
