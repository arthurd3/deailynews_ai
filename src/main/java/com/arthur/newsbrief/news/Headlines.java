package com.arthur.newsbrief.news;

import java.time.Instant;
import java.util.List;

/**
 * An immutable snapshot of the headlines matching a query at a point in time.
 *
 * @param query       the query that produced this snapshot
 * @param articles    matching articles, most significant first
 * @param retrievedAt when the upstream provider was actually called
 */
public record Headlines(HeadlinesQuery query, List<Article> articles, Instant retrievedAt) {

    public Headlines {
        articles = List.copyOf(articles);
    }

    public boolean isEmpty() {
        return articles.isEmpty();
    }

    public int size() {
        return articles.size();
    }
}
