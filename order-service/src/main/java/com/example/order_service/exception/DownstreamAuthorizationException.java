package com.example.order_service.exception;

import org.springframework.http.HttpStatus;

public class DownstreamAuthorizationException extends RuntimeException {
    private final HttpStatus status;

    public DownstreamAuthorizationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
