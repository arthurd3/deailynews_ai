package com.arthur.newsbrief.summarization;

import java.util.List;

/**
 * Port for producing a {@link NewsSummary} from source material.
 *
 * <p>The implementation currently talks to a local Ollama model through Spring AI.
 * Because callers only see this interface, moving to a hosted provider is a change to
 * one class in {@code internal} and a line of configuration.
 */
public interface SummaryGenerator {

    /**
     * Summarizes {@code documents} into a single structured brief.
     *
     * @throws SummarizationException when the model is unreachable or returns something
     *                                that cannot be parsed as a summary
     */
    NewsSummary summarize(List<SourceDocument> documents);
}
