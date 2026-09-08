package com.arthur.newsbrief.news.internal;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative client for the NewsAPI REST endpoints.
 *
 * <p>Spring generates the implementation from this interface, which is why there is no
 * hand-written URL assembly, no {@code RestTemplate} instantiation and no place for the
 * API key to end up in a log line — it is attached as a header by
 * {@link NewsApiConfiguration}.
 *
 * <p>Query parameters arrive as a map because {@code category} is optional and the
 * caller decides whether to send it at all.
 */
@HttpExchange
interface NewsApiClient {

    @GetExchange("/top-headlines")
    NewsApiTopHeadlines topHeadlines(@RequestParam MultiValueMap<String, String> parameters);
}
