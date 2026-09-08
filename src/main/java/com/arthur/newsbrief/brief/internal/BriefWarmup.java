package com.arthur.newsbrief.brief.internal;

import com.arthur.newsbrief.news.HeadlinesQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Generates the default brief on a schedule so the first visitor does not pay for it.
 *
 * <p>Without this, whoever opens the page first in the morning waits out a NewsAPI round trip
 * plus tens of seconds of local inference. The job runs ahead of that and leaves the result in
 * the cache.
 *
 * <p>It calls {@code regenerate} rather than {@code dailyBrief} deliberately: the point is to
 * replace whatever is cached with something current, not to return early because a stale entry
 * still happens to be there.
 *
 * <p>Single instance assumed. Running several replicas would want a shared lock (ShedLock or
 * similar) so they do not all summarize the same headlines at once; that is deliberately not
 * pulled in for a one-process deployment.
 */
@Component
@ConditionalOnProperty(name = "newsbrief.warmup.enabled", havingValue = "true", matchIfMissing = true)
class BriefWarmup {

    private static final Logger log = LoggerFactory.getLogger(BriefWarmup.class);

    private final NewsBriefService newsBriefService;
    private final BriefProperties properties;

    BriefWarmup(NewsBriefService newsBriefService, BriefProperties properties) {
        this.newsBriefService = newsBriefService;
        this.properties = properties;
    }

    @Scheduled(cron = "${newsbrief.warmup.cron:0 0 6 * * *}", zone = "${newsbrief.warmup.zone:UTC}")
    void warmDefaultBrief() {
        HeadlinesQuery query = HeadlinesQuery.ofCountry(properties.defaultCountry());
        log.info("Warming the cache for {}", query);

        try {
            newsBriefService.regenerate(query);
            log.info("Cache warmed for {}", query);
        }
        catch (RuntimeException ex) {
            // A failed warm-up must not kill the scheduler; the next request just pays the
            // cost the way it did before this job existed.
            log.warn("Warm-up failed for {} — falling back to on-demand generation", query, ex);
        }
    }
}
