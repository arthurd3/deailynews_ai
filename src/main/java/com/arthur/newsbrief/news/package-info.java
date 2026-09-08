/**
 * Retrieval of current headlines from an external news provider.
 *
 * <p>Public API: {@link com.arthur.newsbrief.news.NewsProvider} and the records it
 * returns. Everything under {@code internal} — the NewsAPI wire format, credentials and
 * HTTP wiring — is invisible to other modules, so swapping the upstream provider stays a
 * local change.
 */
@ApplicationModule(displayName = "News Acquisition")
package com.arthur.newsbrief.news;

import org.springframework.modulith.ApplicationModule;
