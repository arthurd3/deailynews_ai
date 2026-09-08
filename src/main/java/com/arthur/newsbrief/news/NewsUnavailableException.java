package com.arthur.newsbrief.news;

import com.arthur.newsbrief.shared.error.UpstreamUnavailableException;

/**
 * Signals that headlines could not be retrieved.
 *
 * <p>{@link #transientFailure()} separates the two very different causes behind that:
 * a timeout or a 503 is worth retrying, a rejected API key never is. Retrying a bad
 * credential just multiplies the failure and burns quota.
 */
public class NewsUnavailableException extends UpstreamUnavailableException {

    private static final String UPSTREAM = "newsapi";

    private final boolean transientFailure;

    public NewsUnavailableException(String message, boolean transientFailure, Throwable cause) {
        super(UPSTREAM, message, cause);
        this.transientFailure = transientFailure;
    }

    /** Whether retrying the same request could plausibly succeed. */
    public boolean transientFailure() {
        return transientFailure;
    }
}
