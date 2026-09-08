/**
 * Turning a set of source documents into a structured editorial summary.
 *
 * <p>This module knows nothing about NewsAPI, and nothing about HTTP responses. It
 * accepts {@link com.arthur.newsbrief.summarization.SourceDocument} rather than the news
 * module's {@code Article} on purpose: that keeps news acquisition and summarization
 * independent siblings instead of a chain, so either can be replaced without touching
 * the other.
 */
@ApplicationModule(displayName = "AI Summarization")
package com.arthur.newsbrief.summarization;

import org.springframework.modulith.ApplicationModule;
