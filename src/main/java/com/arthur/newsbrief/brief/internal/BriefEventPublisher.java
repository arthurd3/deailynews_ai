package com.arthur.newsbrief.brief.internal;

import java.time.Instant;

import com.arthur.newsbrief.brief.BriefGenerated;
import com.arthur.newsbrief.news.HeadlinesQuery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Announces that a brief was generated.
 *
 * <p>A separate bean with its own short transaction on purpose. Spring Modulith writes the
 * event publication into its log as part of the publishing transaction, and generating a brief
 * takes tens of seconds of model inference — wrapping that in a database transaction would
 * hold a connection open for the whole time. Building happens outside; only the announcement
 * is transactional.
 */
@Component
class BriefEventPublisher {

    private final ApplicationEventPublisher events;

    BriefEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Transactional
    void briefGenerated(HeadlinesQuery query, com.arthur.newsbrief.brief.DailyBrief brief) {
        events.publishEvent(new BriefGenerated(query.toString(), brief, Instant.now()));
    }
}
