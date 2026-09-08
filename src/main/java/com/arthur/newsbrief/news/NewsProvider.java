package com.arthur.newsbrief.news;

/**
 * Port for reading current headlines.
 *
 * <p>Callers depend on this interface, never on the NewsAPI adapter behind it. Tests
 * that only need headlines can supply a two-line fake instead of standing up HTTP.
 */
public interface NewsProvider {

    /**
     * Fetches the headlines matching {@code query}.
     *
     * @throws NewsUnavailableException when the upstream provider cannot be reached or
     *                                  refuses the request
     */
    Headlines topHeadlines(HeadlinesQuery query);
}
