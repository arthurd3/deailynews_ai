package com.arthur.newsbrief.news.internal;

import com.arthur.newsbrief.news.Headlines;
import com.arthur.newsbrief.news.HeadlinesQuery;
import com.arthur.newsbrief.news.NewsProvider;
import com.arthur.newsbrief.news.NewsUnavailableException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.arthur.newsbrief.PostgresBackedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives the NewsAPI adapter against a stubbed server.
 *
 * <p>Covers the paths that only appear when the upstream misbehaves — which is exactly
 * where the original implementation went wrong, by assuming a well-formed response and
 * dereferencing whatever came back.
 */
@EnabledIf(value = "com.arthur.newsbrief.PostgresBackedTest#databaseIsReachable",
        disabledReason = "Docker unavailable, or its published ports do not forward traffic here")
@SpringBootTest(properties = "newsbrief.news-api.key=test-key")
@ActiveProfiles("test")
@EnableWireMock(@ConfigureWireMock(name = "newsapi", baseUrlProperties = "newsbrief.news-api.base-url"))
class NewsApiNewsProviderTest extends PostgresBackedTest {

    private static final String TOP_HEADLINES = "/top-headlines";

    @InjectWireMock("newsapi")
    private WireMockServer newsApi;

    @Autowired
    private NewsProvider newsProvider;

    @Autowired
    private CacheManager cacheManager;

    private HeadlinesQuery uniqueQuery(String country) {
        // Each test uses its own country so the shared cache cannot leak results between them.
        return HeadlinesQuery.ofCountry(country);
    }

    @Test
    void mapsASuccessfulResponseOntoArticles() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES)).willReturn(okJson("""
                {
                  "status": "ok",
                  "totalResults": 2,
                  "articles": [
                    {
                      "source": {"id": "wire", "name": "Example Wire"},
                      "title": "Rates held steady",
                      "description": "The central bank paused.",
                      "url": "https://example.test/1",
                      "publishedAt": "2026-09-08T06:00:00Z"
                    },
                    {
                      "source": {"id": null, "name": "Second Source"},
                      "title": "Storm clears",
                      "description": null,
                      "url": "https://example.test/2",
                      "publishedAt": "2026-09-08T05:30:00Z"
                    }
                  ]
                }""")));

        Headlines headlines = newsProvider.topHeadlines(uniqueQuery("aa"));

        assertThat(headlines.size()).isEqualTo(2);
        assertThat(headlines.articles().getFirst().title()).isEqualTo("Rates held steady");
        assertThat(headlines.articles().getFirst().source()).isEqualTo("Example Wire");
        assertThat(headlines.articles().get(1).description()).isNull();
    }

    @Test
    void sendsTheApiKeyAsAHeaderAndNeverInTheQueryString() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES))
                .willReturn(okJson("{\"status\":\"ok\",\"totalResults\":0,\"articles\":[]}")));

        newsProvider.topHeadlines(uniqueQuery("ab"));

        newsApi.verify(getRequestedFor(urlPathEqualTo(TOP_HEADLINES))
                .withHeader("X-Api-Key", equalTo("test-key")));

        assertThat(newsApi.getAllServeEvents())
                .allSatisfy(event -> assertThat(event.getRequest().getUrl()).doesNotContain("test-key"));
    }

    @Test
    void omitsCategoryWhenTheQueryDoesNotNameOne() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES))
                .willReturn(okJson("{\"status\":\"ok\",\"totalResults\":0,\"articles\":[]}")));

        newsProvider.topHeadlines(new HeadlinesQuery("ac", null));

        assertThat(newsApi.getAllServeEvents())
                .allSatisfy(event -> assertThat(event.getRequest().getUrl()).doesNotContain("category"));
    }

    @Test
    void discardsArticlesWithdrawnByThePublisher() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES)).willReturn(okJson("""
                {
                  "status": "ok",
                  "totalResults": 3,
                  "articles": [
                    {"source": {"name": "A"}, "title": "[Removed]", "description": "[Removed]"},
                    {"source": {"name": "B"}, "title": "   ", "description": "blank title"},
                    {"source": {"name": "C"}, "title": "A real story", "description": "Genuine."}
                  ]
                }""")));

        Headlines headlines = newsProvider.topHeadlines(uniqueQuery("ad"));

        assertThat(headlines.articles()).singleElement()
                .satisfies(article -> assertThat(article.title()).isEqualTo("A real story"));
    }

    @Test
    void reportsAnErrorPayloadReturnedWithA200() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES)).willReturn(okJson("""
                {"status": "error", "code": "apiKeyInvalid", "message": "Your API key is invalid."}""")));

        assertThatThrownBy(() -> newsProvider.topHeadlines(uniqueQuery("ae")))
                .isInstanceOf(NewsUnavailableException.class)
                .hasMessageContaining("apiKeyInvalid")
                .satisfies(thrown -> assertThat(((NewsUnavailableException) thrown).transientFailure())
                        .as("a rejected key will not fix itself")
                        .isFalse());
    }

    @Test
    void doesNotRetryARejectedApiKey() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES)).willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> newsProvider.topHeadlines(uniqueQuery("af")))
                .isInstanceOf(NewsUnavailableException.class);

        assertThat(newsApi.getAllServeEvents())
                .as("401 is terminal, so exactly one call should have been made")
                .hasSize(1);
    }

    @Test
    void retriesAServerErrorAndSucceedsOnASubsequentAttempt() {
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES))
                .inScenario("recovering")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("recovered"));

        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES))
                .inScenario("recovering")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson("""
                        {"status":"ok","totalResults":1,"articles":[
                          {"source":{"name":"A"},"title":"Back online","description":"Recovered."}]}""")));

        Headlines headlines = newsProvider.topHeadlines(uniqueQuery("ag"));

        assertThat(headlines.articles()).singleElement()
                .satisfies(article -> assertThat(article.title()).isEqualTo("Back online"));
        assertThat(newsApi.getAllServeEvents()).hasSizeGreaterThan(1);
    }

    @Test
    void servesRepeatedQueriesFromTheCacheWithoutCallingUpstreamAgain() {
        cacheManager.getCache(NewsApiNewsProvider.CACHE).clear();
        newsApi.stubFor(get(urlPathEqualTo(TOP_HEADLINES))
                .willReturn(okJson("{\"status\":\"ok\",\"totalResults\":0,\"articles\":[]}")));

        HeadlinesQuery query = uniqueQuery("ah");
        newsProvider.topHeadlines(query);
        newsProvider.topHeadlines(query);

        assertThat(newsApi.getAllServeEvents())
                .as("the second call should have been answered from the cache")
                .hasSize(1);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder okJson(String body) {
        return aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body);
    }
}
