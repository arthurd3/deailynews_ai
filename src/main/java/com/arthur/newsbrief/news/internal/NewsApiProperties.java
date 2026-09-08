package com.arthur.newsbrief.news.internal;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for the NewsAPI integration.
 *
 * <p>Validated at startup so a missing or malformed key fails the boot with a clear
 * message, rather than surfacing as a 401 on the first user request.
 *
 * @param key            NewsAPI key; supplied via the {@code NEWS_API_KEY} environment variable
 * @param baseUrl        API root, overridable so tests can point at a local stub
 * @param maxArticles    upper bound on articles requested per call
 * @param connectTimeout how long to wait for a connection
 * @param readTimeout    how long to wait for a response
 */
@ConfigurationProperties("newsbrief.news-api")
@Validated
public record NewsApiProperties(

        @NotBlank(message = "A NewsAPI key is required. Set NEWS_API_KEY in your environment or .env file.")
        String key,

        @NotNull
        @DefaultValue("https://newsapi.org/v2")
        URI baseUrl,

        @Min(1) @Max(100)
        @DefaultValue("20")
        int maxArticles,

        @NotNull @DefaultValue("3s")
        Duration connectTimeout,

        @NotNull @DefaultValue("10s")
        Duration readTimeout) {
}
