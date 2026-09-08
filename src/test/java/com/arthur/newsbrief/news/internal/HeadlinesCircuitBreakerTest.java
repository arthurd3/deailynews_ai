package com.arthur.newsbrief.news.internal;

import com.arthur.newsbrief.news.Headlines;
import com.arthur.newsbrief.news.HeadlinesQuery;
import com.arthur.newsbrief.news.NewsProvider;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.arthur.newsbrief.PostgresBackedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the interaction between the cache and the circuit breaker.
 *
 * <p>Advice ordering is easy to get backwards and invisible when you do. If the breaker
 * sat outside the cache, an upstream outage would also suppress results the application
 * already holds — the opposite of what a cache is for during an outage.
 */
@EnabledIf(value = "com.arthur.newsbrief.PostgresBackedTest#databaseIsReachable",
        disabledReason = "Docker unavailable, or its published ports do not forward traffic here")
@SpringBootTest(properties = {
        "newsbrief.news-api.key=test-key",
        "resilience4j.circuitbreaker.instances.headlines.slidingWindowSize=2",
        "resilience4j.circuitbreaker.instances.headlines.minimumNumberOfCalls=2",
        "resilience4j.circuitbreaker.instances.headlines.failureRateThreshold=50",
        "resilience4j.circuitbreaker.instances.headlines.waitDurationInOpenState=60s"
})
@ActiveProfiles("test")
@EnableWireMock(@ConfigureWireMock(name = "newsapi", baseUrlProperties = "newsbrief.news-api.base-url"))
class HeadlinesCircuitBreakerTest extends PostgresBackedTest {

    private static final String TOP_HEADLINES = "/top-headlines";
    private static final HeadlinesQuery CACHED = HeadlinesQuery.ofCountry("ca");

    @InjectWireMock("newsapi")
    private WireMockServer newsApi;

    @Autowired
    private NewsProvider newsProvider;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @Test
    void keepsServingCachedHeadlinesAfterTheCircuitOpens() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES)).willReturn(okJson("""
                {"status":"ok","totalResults":1,"articles":[
                  {"source":{"name":"Wire"},"title":"Cached story","description":"Held."}]}""")));

        Headlines primed = newsProvider.topHeadlines(CACHED);
        assertThat(primed.articles()).singleElement()
                .satisfies(article -> assertThat(article.title()).isEqualTo("Cached story"));

        openTheCircuit();

        // Uncached traffic is rejected outright...
        assertThatThrownBy(() -> newsProvider.topHeadlines(HeadlinesQuery.ofCountry("cc")))
                .isInstanceOf(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);

        // ...while a query already in the cache is still answered.
        assertThat(newsProvider.topHeadlines(CACHED).articles())
                .as("an open circuit must not hide results the application already holds")
                .singleElement()
                .satisfies(article -> assertThat(article.title()).isEqualTo("Cached story"));
    }

    private void openTheCircuit() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES)).willReturn(aResponse().withStatus(401)));

        // 401 is terminal, so each of these is a single failed call against the breaker.
        for (String country : new String[] { "cb", "cd", "ce", "cf" }) {
            try {
                newsProvider.topHeadlines(HeadlinesQuery.ofCountry(country));
            }
            catch (RuntimeException expected) {
                // counted by the circuit breaker
            }
        }

        assertThat(circuitBreakers.circuitBreaker(NewsApiNewsProvider.CACHE).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    private static ResponseDefinitionBuilder okJson(String body) {
        return aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body);
    }
}
