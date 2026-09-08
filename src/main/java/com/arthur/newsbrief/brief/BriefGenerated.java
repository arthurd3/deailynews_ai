package com.arthur.newsbrief.brief;

import java.time.Instant;

/**
 * Published whenever a brief is actually generated — not when one is served from the cache.
 *
 * <p>This is how the brief module tells the rest of the application that something happened
 * without knowing who cares. The archive listens; the brief module has no idea it exists.
 *
 * @param query       the query the brief answers, as an opaque label
 * @param brief       the generated brief
 * @param generatedAt when it was assembled
 */
public record BriefGenerated(String query, DailyBrief brief, Instant generatedAt) {
}
