package com.arthur.newsbrief.summarization.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for the summarization step.
 *
 * @param maxDocuments how many source documents to include in a single prompt. The
 *                     original implementation appended every article it was given, which
 *                     grows the prompt without bound and eventually overruns the model's
 *                     context window.
 * @param provider     which model backend answers. The port exists precisely so this is a
 *                     configuration choice rather than a rewrite.
 */
@ConfigurationProperties("newsbrief.summarization")
@Validated
public record SummarizationProperties(

        @Min(1) @Max(50)
        @DefaultValue("10")
        int maxDocuments,

        @DefaultValue("ollama")
        Provider provider) {

    /** Model backends this application knows how to talk to. */
    public enum Provider {
        /** Local inference through Ollama. Nothing leaves the machine. */
        OLLAMA,
        /** A hosted Anthropic model. Needs ANTHROPIC_API_KEY. */
        ANTHROPIC
    }
}
