package com.arthur.newsbrief.summarization;

import java.util.List;

/**
 * A structured editorial summary of the supplied source documents.
 *
 * <p>This record is also the schema handed to the language model: Spring AI derives a
 * JSON schema from it and parses the reply back into an instance. That is what lets the
 * HTML and JSON representations be rendered from the same typed value, instead of
 * slicing markdown fences off a free-text blob at the controller.
 *
 * <p>It carries no timestamp by design — the model must not be asked to invent one.
 *
 * @param headline a short banner line for the whole brief
 * @param overview two or three sentences covering the day as a whole
 * @param topics   the individual stories worth calling out
 */
public record NewsSummary(String headline, String overview, List<Topic> topics) {

    public NewsSummary {
        topics = topics == null ? List.of() : List.copyOf(topics);
    }

    /**
     * One story within the brief.
     *
     * @param title    the story in a few words
     * @param summary  what happened and why it matters
     * @param category broad subject area, e.g. {@code politics} or {@code technology}
     */
    public record Topic(String title, String summary, String category) {
    }
}
