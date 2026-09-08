package com.arthur.newsbrief.summarization;

/**
 * A single piece of source material to be summarized.
 *
 * @param title       the document's headline
 * @param description supporting text, may be {@code null}
 */
public record SourceDocument(String title, String description) {
}
