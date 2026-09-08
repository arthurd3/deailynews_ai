package com.arthur.newsbrief.shared.web;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Per-client request budget for the endpoints that can trigger model inference.
 *
 * @param enabled  whether the filter is active
 * @param capacity requests allowed per {@code period} per client
 * @param period   how long a full budget takes to refill
 * @param paths    path prefixes the limit applies to
 */
@ConfigurationProperties("newsbrief.rate-limit")
@Validated
public record RateLimitProperties(

        @DefaultValue("true") boolean enabled,

        @Min(1) @DefaultValue("30") int capacity,

        @NotNull @DefaultValue("1m") Duration period,

        @DefaultValue({ "/api/v1/briefs", "/briefs" }) String[] paths) {
}
