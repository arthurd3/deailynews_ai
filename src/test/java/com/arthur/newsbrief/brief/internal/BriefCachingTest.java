package com.arthur.newsbrief.brief.internal;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.arthur.newsbrief.brief.DailyBrief;
import com.arthur.newsbrief.news.Article;
import com.arthur.newsbrief.news.Headlines;
import com.arthur.newsbrief.news.HeadlinesQuery;
import com.arthur.newsbrief.news.NewsProvider;
import com.arthur.newsbrief.summarization.NewsSummary;
import com.arthur.newsbrief.summarization.SummaryGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Regression cover for the cache key.
 *
 * <p>The previous implementation keyed this cache on {@code #root.method.name}, a
 * constant. Every distinct request therefore collided on one entry and the first
 * caller's brief was served to everybody. These tests fail if that ever comes back.
 */
@SpringBootTest(properties = "newsbrief.news-api.key=test-key")
@ActiveProfiles("test")
class BriefCachingTest {

    @Autowired
    private NewsBriefService newsBriefService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private NewsProvider newsProvider;

    @MockitoBean
    private SummaryGenerator summaryGenerator;

    private final AtomicInteger generatorCalls = new AtomicInteger();

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        generatorCalls.set(0);

        given(newsProvider.topHeadlines(any())).willAnswer(invocation -> new Headlines(
                invocation.getArgument(0),
                List.of(new Article("A story", "Details.", null, "Wire", null)),
                Instant.now()));

        given(summaryGenerator.summarize(any())).willAnswer(invocation -> {
            int call = generatorCalls.incrementAndGet();
            return new NewsSummary("Headline " + call, "Overview " + call, List.of());
        });
    }

    @Test
    void reusesTheBriefForAnIdenticalQuery() {
        HeadlinesQuery query = new HeadlinesQuery("us", "technology");

        DailyBrief first = newsBriefService.dailyBrief(query);
        DailyBrief second = newsBriefService.dailyBrief(query);

        assertThat(second).isEqualTo(first);
        assertThat(generatorCalls).hasValue(1);
    }

    @Test
    void generatesASeparateBriefForEveryDistinctQuery() {
        DailyBrief us = newsBriefService.dailyBrief(new HeadlinesQuery("us", null));
        DailyBrief gb = newsBriefService.dailyBrief(new HeadlinesQuery("gb", null));
        DailyBrief usTech = newsBriefService.dailyBrief(new HeadlinesQuery("us", "technology"));

        assertThat(List.of(us.headline(), gb.headline(), usTech.headline()))
                .as("each query must get its own brief, not the first caller's")
                .doesNotHaveDuplicates();
        assertThat(generatorCalls).hasValue(3);
    }

    @Test
    void treatsEquivalentQueriesAsTheSameKey() {
        newsBriefService.dailyBrief(new HeadlinesQuery("US", "Technology"));
        newsBriefService.dailyBrief(new HeadlinesQuery(" us ", "technology"));

        assertThat(generatorCalls)
                .as("normalized queries must not occupy two cache entries")
                .hasValue(1);
    }
}
