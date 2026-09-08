package com.arthur.newsbrief.brief.internal;

import java.time.Instant;
import java.util.List;

import com.arthur.newsbrief.brief.DailyBrief;
import com.arthur.newsbrief.news.Article;
import com.arthur.newsbrief.news.Headlines;
import com.arthur.newsbrief.news.HeadlinesQuery;
import com.arthur.newsbrief.news.NewsProvider;
import com.arthur.newsbrief.news.NewsUnavailableException;
import com.arthur.newsbrief.summarization.NewsSummary;
import com.arthur.newsbrief.summarization.SourceDocument;
import com.arthur.newsbrief.summarization.SummaryGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the orchestration logic with two hand-written fakes.
 *
 * <p>No Spring context, no HTTP, no model — which is the whole point of the service
 * depending on ports rather than on the adapters behind them.
 */
class NewsBriefServiceTest {

    private static final HeadlinesQuery QUERY = HeadlinesQuery.ofCountry("us");

    @Test
    void assemblesABriefFromHeadlinesAndSummary() {
        RecordingSummaryGenerator summarizer = new RecordingSummaryGenerator(
                new NewsSummary("Markets steady", "A quiet day.",
                        List.of(new NewsSummary.Topic("Rates held", "The central bank paused.", "business"))));

        NewsBriefService service = new NewsBriefService(
                query -> new Headlines(query, List.of(
                        new Article("Rates held", "No change this month.", "https://example.test/1",
                                "Example Wire", Instant.parse("2026-09-08T06:00:00Z"))),
                        Instant.parse("2026-09-08T07:00:00Z")),
                summarizer);

        DailyBrief brief = service.dailyBrief(QUERY);

        assertThat(brief.headline()).isEqualTo("Markets steady");
        assertThat(brief.overview()).isEqualTo("A quiet day.");
        assertThat(brief.topics()).singleElement()
                .satisfies(topic -> {
                    assertThat(topic.title()).isEqualTo("Rates held");
                    assertThat(topic.category()).isEqualTo("business");
                });
        assertThat(brief.sources().articleCount()).isEqualTo(1);
        assertThat(brief.sources().country()).isEqualTo("us");
        assertThat(brief.sources().retrievedAt()).isEqualTo(Instant.parse("2026-09-08T07:00:00Z"));
    }

    @Test
    void passesTitleAndDescriptionOfEveryArticleToTheSummarizer() {
        RecordingSummaryGenerator summarizer = new RecordingSummaryGenerator(
                new NewsSummary("h", "o", List.of()));

        NewsBriefService service = new NewsBriefService(
                query -> new Headlines(query, List.of(
                        new Article("First", "One", null, null, null),
                        new Article("Second", null, null, null, null)),
                        Instant.now()),
                summarizer);

        service.dailyBrief(QUERY);

        assertThat(summarizer.received).containsExactly(
                new SourceDocument("First", "One"),
                new SourceDocument("Second", null));
    }

    @Test
    void refusesToSummarizeAnEmptyHeadlineList() {
        NewsBriefService service = new NewsBriefService(
                query -> new Headlines(query, List.of(), Instant.now()),
                documents -> {
                    throw new AssertionError("the model must not be called without material");
                });

        assertThatThrownBy(() -> service.dailyBrief(QUERY))
                .isInstanceOf(NewsUnavailableException.class)
                .hasMessageContaining("No headlines");
    }

    @Test
    void propagatesUpstreamFailures() {
        NewsBriefService service = new NewsBriefService(
                query -> {
                    throw new NewsUnavailableException("NewsAPI responded with 503", true, null);
                },
                documents -> {
                    throw new AssertionError("unreachable");
                });

        assertThatThrownBy(() -> service.dailyBrief(QUERY))
                .isInstanceOf(NewsUnavailableException.class);
    }

    /** Captures what the service handed to the model. */
    private static final class RecordingSummaryGenerator implements SummaryGenerator {

        private final NewsSummary response;
        private List<SourceDocument> received = List.of();

        private RecordingSummaryGenerator(NewsSummary response) {
            this.response = response;
        }

        @Override
        public NewsSummary summarize(List<SourceDocument> documents) {
            this.received = documents;
            return response;
        }
    }
}
