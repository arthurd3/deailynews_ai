package com.arthur.newsbrief.brief.internal;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Defaults applied to incoming brief requests.
 *
 * <p>What "no country specified" means is a property of the published API, so it is
 * decided here rather than inside the news adapter. Resolving it before the query is
 * built also keeps the cache keyed on fully specified requests.
 *
 * @param defaultCountry country used when a request does not name one
 */
@ConfigurationProperties("newsbrief.brief")
@Validated
public record BriefProperties(

        @Pattern(regexp = "[a-zA-Z]{2}", message = "must be an ISO 3166-1 alpha-2 country code")
        @DefaultValue("us")
        String defaultCountry) {
}
