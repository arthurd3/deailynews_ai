package com.arthur.newsbrief.brief.internal;

import com.arthur.newsbrief.news.HeadlinesQuery;
import com.arthur.newsbrief.news.NewsUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** The scheduled job that pays the generation cost before the first visitor does. */
class BriefWarmupTest {

    private final NewsBriefService service = mock(NewsBriefService.class);
    private final BriefWarmup warmup = new BriefWarmup(service, new BriefProperties("us"));

    @Test
    void regeneratesRatherThanReadingWhateverIsCached() {
        warmup.warmDefaultBrief();

        // The point is to replace a stale entry, not to return early because one exists.
        verify(service).regenerate(HeadlinesQuery.ofCountry("us"));
        verify(service, never()).dailyBrief(any());
    }

    @Test
    void usesTheConfiguredDefaultCountry() {
        new BriefWarmup(service, new BriefProperties("gb")).warmDefaultBrief();

        verify(service).regenerate(HeadlinesQuery.ofCountry("gb"));
    }

    @Test
    void survivesAnUpstreamOutage() {
        given(service.regenerate(any()))
                .willThrow(new NewsUnavailableException("NewsAPI responded with 503", true, null));

        // A throwing scheduled method would stop the whole schedule. Failing to warm the cache
        // just means the next request pays what it used to.
        assertThatCode(warmup::warmDefaultBrief).doesNotThrowAnyException();
        assertThat(true).isTrue();
    }
}
