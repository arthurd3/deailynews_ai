package com.arthur.newsbrief.summarization;

import com.arthur.newsbrief.shared.error.UpstreamUnavailableException;

/** Signals that a summary could not be generated. */
public class SummarizationException extends UpstreamUnavailableException {

    private static final String UPSTREAM = "ollama";

    public SummarizationException(String message, Throwable cause) {
        super(UPSTREAM, message, cause);
    }
}
