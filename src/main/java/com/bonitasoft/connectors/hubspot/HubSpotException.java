package com.bonitasoft.connectors.hubspot;

/**
 * Typed exception for HubSpot connector operations.
 * Carries HTTP status code and retryable flag for retry policy decisions.
 */
public class HubSpotException extends Exception {

    private final int statusCode;
    private final boolean retryable;

    public HubSpotException(String message) {
        super(message);
        this.statusCode = -1;
        this.retryable = false;
    }

    public HubSpotException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = false;
    }

    public HubSpotException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public HubSpotException(String message, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
