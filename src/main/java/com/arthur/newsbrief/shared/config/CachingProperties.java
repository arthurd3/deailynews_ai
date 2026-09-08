package com.arthur.newsbrief.shared.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Caffeine cache tuning, expressed as native Caffeine spec strings so TTLs can be
 * changed per environment without touching Java.
 *
 * @param defaultSpec applied to any cache without an explicit entry in {@code caches}
 * @param caches      cache name to Caffeine spec, e.g. {@code maximumSize=64,expireAfterWrite=5m}
 */
@ConfigurationProperties("newsbrief.caching")
public record CachingProperties(
        @DefaultValue("maximumSize=100,expireAfterWrite=10m") String defaultSpec,
        @DefaultValue Map<String, String> caches) {
}
