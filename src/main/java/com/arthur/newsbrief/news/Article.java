package com.arthur.newsbrief.news;

import java.time.Instant;

/**
 * A single headline, normalized away from any particular provider's wire format.
 *
 * @param title       headline text; never blank for articles that reach a caller
 * @param description short standfirst, may be {@code null} when the provider omits one
 * @param url         canonical link to the full story, may be {@code null}
 * @param source      publisher name, may be {@code null}
 * @param publishedAt publication instant, may be {@code null}
 */
public record Article(String title, String description, String url, String source, Instant publishedAt) {
}
