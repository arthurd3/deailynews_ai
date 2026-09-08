package com.arthur.newsbrief.shared.error;

/**
 * Raised when a third-party system this service depends on cannot satisfy a request.
 *
 * <p>Modules subclass this rather than leaking their own exception types across module
 * boundaries, which lets a single handler translate every upstream failure into one
 * consistent {@code 503} response.
 */
public abstract class UpstreamUnavailableException extends RuntimeException {

    private final String upstream;

    protected UpstreamUnavailableException(String upstream, String message, Throwable cause) {
        super(message, cause);
        this.upstream = upstream;
    }

    /** Identifier of the failing dependency, surfaced as a problem-detail property. */
    public String upstream() {
        return upstream;
    }
}
