package com.arthur.newsbrief.brief;

import java.time.Instant;
import java.util.List;

import com.arthur.newsbrief.brief.internal.BriefMarkdownRenderer;
import com.arthur.newsbrief.brief.internal.BriefProperties;
import com.arthur.newsbrief.brief.internal.NewsBriefService;
import com.arthur.newsbrief.news.HeadlinesQuery;
import com.arthur.newsbrief.news.NewsUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/** Verifies the HTTP contract: content negotiation, validation and error shape. */
@WebMvcTest(NewsBriefController.class)
@EnableConfigurationProperties(BriefProperties.class)
@Import(BriefMarkdownRenderer.class)
class NewsBriefControllerTest {

    private static final String DAILY = "/api/v1/briefs/daily";

    private static final DailyBrief BRIEF = new DailyBrief(
            "Markets steady",
            "A quiet day across most sectors.",
            List.of(new DailyBrief.Topic("Rates held", "The central bank paused.", "business")),
            new DailyBrief.Sources("us", null, 12, Instant.parse("2026-09-08T07:00:00Z")),
            Instant.parse("2026-09-08T07:00:05Z"));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private NewsBriefService newsBriefService;

    @Test
    void servesJsonWhenJsonIsRequested() {
        given(newsBriefService.dailyBrief(any())).willReturn(BRIEF);

        assertThat(mvc.get().uri(DAILY).accept(MediaType.APPLICATION_JSON))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .extractingPath("$.headline").isEqualTo("Markets steady");
    }

    @Test
    void servesMarkdownWhenMarkdownIsRequested() {
        given(newsBriefService.dailyBrief(any())).willReturn(BRIEF);

        assertThat(mvc.get().uri(DAILY).accept(MediaType.valueOf(BriefMarkdownRenderer.TEXT_MARKDOWN)))
                .hasStatusOk()
                .bodyText()
                // A third representation of the same resource, so the copy button in the UI
                // fetches it rather than reassembling the text in JavaScript.
                .contains("# Markets steady")
                .contains("## Rates held")
                .contains("The central bank paused.");
    }

    @Test
    void doesNotServeHtmlFromTheApiResource() {
        assertThat(mvc.get().uri(DAILY).accept(MediaType.TEXT_HTML))
                .as("the browser-facing pages live outside /api/v1")
                .hasStatus(406);
    }

    @Test
    void appliesTheDefaultCountryWhenTheRequestDoesNotNameOne() {
        given(newsBriefService.dailyBrief(any())).willReturn(BRIEF);

        assertThat(mvc.get().uri(DAILY).accept(MediaType.APPLICATION_JSON)).hasStatusOk();

        ArgumentCaptor<HeadlinesQuery> query = ArgumentCaptor.forClass(HeadlinesQuery.class);
        verify(newsBriefService).dailyBrief(query.capture());

        // Resolved here rather than deep in the adapter, so the response can report the
        // country it actually used and the cache is keyed on a fully specified request.
        assertThat(query.getValue()).isEqualTo(new HeadlinesQuery("us", null));
    }

    @Test
    void passesQueryParametersThroughToTheService() {
        given(newsBriefService.dailyBrief(any())).willReturn(BRIEF);

        assertThat(mvc.get().uri(DAILY + "?country=GB&category=technology")
                .accept(MediaType.APPLICATION_JSON)).hasStatusOk();

        ArgumentCaptor<HeadlinesQuery> query = ArgumentCaptor.forClass(HeadlinesQuery.class);
        verify(newsBriefService).dailyBrief(query.capture());

        assertThat(query.getValue()).isEqualTo(new HeadlinesQuery("gb", "technology"));
    }

    @Test
    void rejectsAnUnknownCategory() {
        assertThat(mvc.get().uri(DAILY + "?category=astrology").accept(MediaType.APPLICATION_JSON))
                .hasStatus(400);
    }

    @Test
    void rejectsAMalformedCountryCode() {
        assertThat(mvc.get().uri(DAILY + "?country=usa").accept(MediaType.APPLICATION_JSON))
                .hasStatus(400);
    }

    @Test
    void reportsAnUpstreamOutageAsAProblemDetail() {
        given(newsBriefService.dailyBrief(any()))
                .willThrow(new NewsUnavailableException("NewsAPI responded with 503", true, null));

        MvcTestResult result = mvc.get().uri(DAILY).accept(MediaType.APPLICATION_JSON).exchange();

        assertThat(result).hasStatus(503);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Upstream service unavailable");
        assertThat(result).bodyJson().extractingPath("$.upstream").isEqualTo("newsapi");
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(503);
    }

    @Test
    void reportsAnOpenCircuitAsAnUpstreamOutageRatherThanAnInternalError() {
        given(newsBriefService.dailyBrief(any())).willThrow(
                CallNotPermittedException.createCallNotPermittedException(
                        CircuitBreaker.ofDefaults("headlines")));

        MvcTestResult result = mvc.get().uri(DAILY).accept(MediaType.APPLICATION_JSON).exchange();

        assertThat(result).hasStatus(503);
        assertThat(result).bodyJson().extractingPath("$.upstream").isEqualTo("headlines");
    }

    @Test
    void hidesInternalDetailWhenSomethingUnexpectedFails() {
        given(newsBriefService.dailyBrief(any()))
                .willThrow(new IllegalStateException("jdbc://user:hunter2@db/internal"));

        assertThat(mvc.get().uri(DAILY).accept(MediaType.APPLICATION_JSON))
                .hasStatus(500)
                .bodyText()
                .as("an unexpected failure must not echo internals back to the caller")
                .doesNotContain("hunter2");
    }
}
