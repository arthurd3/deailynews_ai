package com.arthur.newsbrief.news.internal;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import com.arthur.newsbrief.news.Article;
import com.arthur.newsbrief.news.Headlines;
import com.arthur.newsbrief.news.HeadlinesQuery;
import com.arthur.newsbrief.news.NewsProvider;
import com.arthur.newsbrief.news.NewsUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.resilience.retry.MethodRetryPredicate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * NewsAPI-backed implementation of the {@link NewsProvider} port.
 *
 * <p>Owns three concerns that the rest of the application should never see: mapping the
 * provider's wire format onto {@link Article}, deciding which failures are worth
 * retrying, and short-circuiting once NewsAPI is clearly down.
 */
@Component
class NewsApiNewsProvider implements NewsProvider {

    static final String CACHE = "headlines";

    private static final Logger log = LoggerFactory.getLogger(NewsApiNewsProvider.class);

    private final NewsApiClient client;
    private final NewsApiProperties properties;

    NewsApiNewsProvider(NewsApiClient client, NewsApiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    @Cacheable(cacheNames = CACHE, key = "#query")
    @CircuitBreaker(name = CACHE)
    @Retryable(predicate = RetryTransientFailures.class,
            maxRetries = 2, delay = 400, multiplier = 2.0, jitter = 200, maxDelay = 3000)
    public Headlines topHeadlines(HeadlinesQuery query) {
        NewsApiTopHeadlines response = call(query);

        if (!response.isOk()) {
            // A 200 with status="error" is NewsAPI's way of reporting a bad request.
            throw new NewsUnavailableException(
                    "NewsAPI rejected the request (%s): %s".formatted(response.code(), response.message()),
                    false, null);
        }

        List<Article> articles = response.articlesOrEmpty().stream()
                .filter(NewsApiNewsProvider::hasUsableContent)
                .map(NewsApiNewsProvider::toArticle)
                .toList();

        log.debug("Retrieved {} usable of {} articles for {}",
                articles.size(), response.articlesOrEmpty().size(), query);

        return new Headlines(query, articles, Instant.now());
    }

    private NewsApiTopHeadlines call(HeadlinesQuery query) {
        try {
            NewsApiTopHeadlines response = client.topHeadlines(parametersFor(query));
            if (response == null) {
                throw new NewsUnavailableException("NewsAPI returned an empty response body", true, null);
            }
            return response;
        }
        catch (HttpStatusCodeException ex) {
            throw new NewsUnavailableException(
                    "NewsAPI responded with %s".formatted(ex.getStatusCode()), isTransient(ex), ex);
        }
        catch (ResourceAccessException ex) {
            // Connect or read timeout, DNS failure, connection reset.
            throw new NewsUnavailableException("NewsAPI could not be reached", true, ex);
        }
        catch (RestClientException ex) {
            throw new NewsUnavailableException("NewsAPI response could not be processed", false, ex);
        }
    }

    private MultiValueMap<String, String> parametersFor(HeadlinesQuery query) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("country", query.country());
        parameters.add("pageSize", String.valueOf(properties.maxArticles()));

        if (query.category() != null) {
            parameters.add("category", query.category());
        }
        return parameters;
    }

    /**
     * Server errors and rate limits clear on their own; a rejected key or a malformed
     * request will fail identically no matter how often it is repeated.
     */
    private static boolean isTransient(HttpStatusCodeException ex) {
        return ex.getStatusCode().is5xxServerError()
                || ex.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS);
    }

    private static boolean hasUsableContent(NewsApiTopHeadlines.NewsApiArticle article) {
        // NewsAPI emits "[Removed]" placeholders for withdrawn stories; they carry no
        // information and would only dilute the model's prompt.
        return article.title() != null
                && !article.title().isBlank()
                && !article.title().startsWith("[Removed]");
    }

    private static Article toArticle(NewsApiTopHeadlines.NewsApiArticle article) {
        return new Article(
                article.title(),
                article.description(),
                article.url(),
                article.source() == null ? null : article.source().name(),
                article.publishedAt());
    }

    /** Retries only the failures that a second attempt could actually fix. */
    static class RetryTransientFailures implements MethodRetryPredicate {

        @Override
        public boolean shouldRetry(Method method, Throwable throwable) {
            return throwable instanceof NewsUnavailableException ex && ex.transientFailure();
        }
    }
}
