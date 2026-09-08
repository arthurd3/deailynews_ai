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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Fetches headlines, has them summarized, and assembles the result.
 *
 * <p>Talks only to the two ports, so this class can be unit-tested with a pair of fakes
 * and no network at all.
 */
@Service
public class NewsBriefService {

    static final String CACHE = "briefs";

    private static final Logger log = LoggerFactory.getLogger(NewsBriefService.class);

    private final NewsProvider newsProvider;
    private final SummaryGenerator summaryGenerator;

    NewsBriefService(NewsProvider newsProvider, SummaryGenerator summaryGenerator) {
        this.newsProvider = newsProvider;
        this.summaryGenerator = summaryGenerator;
    }

    /**
     * Builds the brief for {@code query}, reusing a recent one when available.
     *
     * <p>The cache key is the query itself. The previous implementation keyed on the
     * method name, which collapsed every distinct request onto a single entry — so the
     * first caller's brief was served to everyone regardless of what they asked for.
     */
    @Cacheable(cacheNames = CACHE, key = "#query")
    public DailyBrief dailyBrief(HeadlinesQuery query) {
        log.info("Generating a brief for {}", query);

        Headlines headlines = newsProvider.topHeadlines(query);

        if (headlines.isEmpty()) {
            throw new NewsUnavailableException(
                    "No headlines are currently available for " + describe(query), true, null);
        }

        NewsSummary summary = summaryGenerator.summarize(asSourceDocuments(headlines.articles()));

        return new DailyBrief(
                summary.headline(),
                summary.overview(),
                summary.topics().stream()
                        .map(topic -> new DailyBrief.Topic(topic.title(), topic.summary(), topic.category()))
                        .toList(),
                new DailyBrief.Sources(
                        query.country(), query.category(), headlines.size(), headlines.retrievedAt()),
                Instant.now());
    }

    private static List<SourceDocument> asSourceDocuments(List<Article> articles) {
        return articles.stream()
                .map(article -> new SourceDocument(article.title(), article.description()))
                .toList();
    }

    private static String describe(HeadlinesQuery query) {
        return query.category() == null
                ? "country '%s'".formatted(query.country())
                : "country '%s' and category '%s'".formatted(query.country(), query.category());
    }
}
