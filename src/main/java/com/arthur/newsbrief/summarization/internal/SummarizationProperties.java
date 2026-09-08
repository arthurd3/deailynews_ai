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
 */
@ConfigurationProperties("newsbrief.summarization")
@Validated
public record SummarizationProperties(

        @Min(1) @Max(50)
        @DefaultValue("10")
        int maxDocuments) {
}
