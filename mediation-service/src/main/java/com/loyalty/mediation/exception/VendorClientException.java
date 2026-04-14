package com.loyalty.mediation.exception;

import lombok.Getter;

/**
 * Thrown when the vendor API returns a non-2xx response or is unreachable.
 */
@Getter
public class VendorClientException extends RuntimeException {

    private final int vendorStatusCode;

    public VendorClientException(int vendorStatusCode, String message) {
        super(message);
        this.vendorStatusCode = vendorStatusCode;
    }
}
