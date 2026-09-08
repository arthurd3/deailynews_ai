package com.arthur.newsbrief.brief.web;

import java.time.Instant;
import java.util.List;

import com.arthur.newsbrief.brief.DailyBrief;
import com.arthur.newsbrief.brief.internal.BriefProperties;
import com.arthur.newsbrief.brief.internal.NewsBriefService;
import com.arthur.newsbrief.news.HeadlinesQuery;
import com.arthur.newsbrief.news.NewsUnavailableException;
import com.arthur.newsbrief.summarization.SummarizationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** The browser-facing routes: rendering, htmx fragments, and errors as pages. */
@WebMvcTest(BriefPageController.class)
@EnableConfigurationProperties(BriefProperties.class)
class BriefPageControllerTest {

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
    void landingPageOffersTheControlsWithoutGeneratingAnything() {
        MvcTestResult result = mvc.get().uri("/").exchange();

        assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
        assertThat(result).bodyText().contains("Generate brief").contains("Category");

        verify(newsBriefService, never()).dailyBrief(any());
    }

    @Test
    void rendersTheFullPageForAnOrdinaryRequest() {
        given(newsBriefService.dailyBrief(any())).willReturn(BRIEF);

        assertThat(mvc.get().uri("/briefs/daily").exchange())
                .hasStatusOk()
                .bodyText()
                .contains("<!DOCTYPE html>")
                .contains("Markets steady")
                .contains("The central bank paused.");
    }

    @Test
    void returnsOnlyTheFragmentWhenHtmxDrivesTheRequest() {
        given(newsBriefService.dailyBrief(any())).willReturn(BRIEF);

        assertThat(mvc.get().uri("/briefs/daily").header("HX-Request", "true").exchange())
                .hasStatusOk()
                .bodyText()
                .contains("Markets steady")
                .doesNotContain("<!DOCTYPE html>");
    }

    @Test
    void shipsTheLoadingPlaceholderWithEveryRender() {
        given(newsBriefService.dailyBrief(any())).willReturn(BRIEF);

        // htmx swaps content only once the response lands, so the skeleton has to already be
        // on the page. Without it the tab simply freezes for the length of the inference.
        assertThat(mvc.get().uri("/briefs/daily").exchange())
                .bodyText()
                .contains("Summarizing headlines with a local model");
    }

    @Test
    void postGoesThroughTheRegeneratePathRatherThanTheCachedOne() {
        given(newsBriefService.regenerate(any())).willReturn(BRIEF);

        assertThat(mvc.post().uri("/briefs/daily")
                .param("country", "gb")
                .header("HX-Request", "true")
                .exchange()).hasStatusOk();

        verify(newsBriefService).regenerate(new HeadlinesQuery("gb", null));
        verify(newsBriefService, never()).dailyBrief(any());
    }

    @Test
    void appliesTheDefaultCountryWhenNoneIsGiven() {
        given(newsBriefService.dailyBrief(any())).willReturn(BRIEF);

        assertThat(mvc.get().uri("/briefs/daily").exchange()).hasStatusOk();

        verify(newsBriefService).dailyBrief(new HeadlinesQuery("us", null));
    }

    @Test
    void showsAnErrorPageWhenTheNewsProviderIsDown() {
        given(newsBriefService.dailyBrief(any()))
                .willThrow(new NewsUnavailableException("NewsAPI responded with 503", true, null));

        MvcTestResult result = mvc.get().uri("/briefs/daily").exchange();

        // The regression this guards: ApiExceptionHandler is a global @RestControllerAdvice,
        // so without the scoped web advice a browser would be handed raw problem-detail JSON.
        assertThat(result).hasStatus(503).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
        assertThat(result).bodyText()
                .contains("That did not work")
                .contains("newsapi")
                .doesNotContain("\"type\":");
    }

    @Test
    void showsAnErrorPageWhenTheModelIsDown() {
        given(newsBriefService.dailyBrief(any()))
                .willThrow(new SummarizationException("model unreachable", null));

        MvcTestResult result = mvc.get().uri("/briefs/daily").exchange();

        assertThat(result).hasStatus(503);
        assertThat(result).bodyText().contains("ollama").contains("ollama pull");
    }

    @Test
    void rejectsAnUnknownCategoryWithAPageRatherThanJson() {
        MvcTestResult result = mvc.get().uri("/briefs/daily?category=astrology").exchange();

        assertThat(result).hasStatus(400).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
        assertThat(result).bodyText().contains("Bad request");
    }

    @Test
    void hidesInternalDetailWhenSomethingUnexpectedFails() {
        given(newsBriefService.dailyBrief(any()))
                .willThrow(new IllegalStateException("jdbc://user:hunter2@db/internal"));

        MvcTestResult result = mvc.get().uri("/briefs/daily").exchange();

        assertThat(result).hasStatus(500);
        assertThat(result).bodyText().doesNotContain("hunter2");
    }
}
