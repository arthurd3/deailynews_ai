/**
 * Assembles a daily brief and publishes it over HTTP.
 *
 * <p>The only module that depends on both {@code news} and {@code summarization}: it
 * fetches headlines, hands them to the summarizer, and decides how the result is
 * represented. Presentation lives here rather than in the summarization module, because
 * "JSON or HTML" is a delivery question, not a modelling one.
 */
@ApplicationModule(displayName = "Daily Brief")
package com.arthur.newsbrief.brief;

import org.springframework.modulith.ApplicationModule;
