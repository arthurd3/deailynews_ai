package com.arthur.newsbrief.news;

import java.util.Locale;
import java.util.Set;

/**
 * The parameters that identify a headline request.
 *
 * <p>This record doubles as a cache key, which is why the compact constructor
 * normalizes its inputs: {@code "US"} and {@code "us "} must not occupy two cache
 * entries. Being a record, it gets the {@code equals}/{@code hashCode} that correct
 * caching depends on for free — the bug the previous {@code #root.method.name} cache
 * key papered over.
 *
 * <p>The country is required. Leaving it unresolved would let {@code (null, null)} and
 * {@code ("us", null)} occupy two cache entries holding identical results, and would
 * make a response claim it covered no country in particular when it in fact used a
 * default. Deciding what "unspecified" means is the API layer's job, not this record's.
 *
 * @param country  ISO 3166-1 alpha-2 country code, lowercased
 * @param category optional NewsAPI category, or {@code null} for all categories
 */
public record HeadlinesQuery(String country, String category) {

    /** Categories NewsAPI accepts on the top-headlines endpoint. */
    public static final Set<String> CATEGORIES = Set.of(
            "business", "entertainment", "general", "health", "science", "sports", "technology");

    public HeadlinesQuery {
        country = normalize(country);
        category = normalize(category);

        if (country == null) {
            throw new IllegalArgumentException("A headlines query must name a country");
        }
    }

    public static HeadlinesQuery ofCountry(String country) {
        return new HeadlinesQuery(country, null);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
