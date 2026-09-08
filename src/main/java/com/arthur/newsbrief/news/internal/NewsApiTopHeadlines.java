package com.arthur.newsbrief.news.internal;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The {@code /top-headlines} response exactly as NewsAPI sends it.
 *
 * <p>Deliberately confined to this package: the public {@link com.arthur.newsbrief.news.Article}
 * is what leaves the module, so a change to NewsAPI's payload cannot ripple outward.
 *
 * @param status       {@code "ok"} or {@code "error"}
 * @param code         provider error code, present only on failures
 * @param message      provider error message, present only on failures
 * @param totalResults total matches upstream, absent on error payloads
 * @param articles     the returned articles, {@code null} on failures
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record NewsApiTopHeadlines(String status, String code, String message, Integer totalResults,
                           List<NewsApiTopHeadlines.NewsApiArticle> articles) {

    static final String STATUS_OK = "ok";

    boolean isOk() {
        return STATUS_OK.equalsIgnoreCase(status);
    }

    List<NewsApiArticle> articlesOrEmpty() {
        return articles == null ? List.of() : articles;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NewsApiArticle(NewsApiSource source, String title, String description, String url,
                          Instant publishedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NewsApiSource(String id, String name) {
    }
}
