package com.arthur.newsbrief.brief;

import com.arthur.newsbrief.brief.internal.BriefMarkdownRenderer;
import com.arthur.newsbrief.brief.internal.BriefProperties;
import com.arthur.newsbrief.brief.internal.NewsBriefService;
import com.arthur.newsbrief.news.HeadlinesQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The machine-facing API: one resource, two representations chosen by content negotiation.
 *
 * <p>{@code application/json} for programs, {@code text/markdown} for anything that wants to
 * paste the brief somewhere. The human-facing pages live in {@code brief.web} instead — once
 * there is an app shell with navigation and partial updates, HTML stops being "just another
 * representation of this resource" and becomes a separate delivery surface.
 *
 * <p>The class is deliberately not {@code @Validated}: Spring MVC validates constrained
 * handler parameters natively and reports failures as a 400 problem detail, whereas the
 * AOP-based alternative raises a {@code ConstraintViolationException} that surfaces as a 500.
 */
@RestController
@RequestMapping("/api/v1/briefs")
@Tag(name = "Briefs", description = "AI-generated summaries of current headlines")
class NewsBriefController {

    private static final String COUNTRY_PATTERN = "[a-zA-Z]{2}";
    private static final String CATEGORY_PATTERN =
            "business|entertainment|general|health|science|sports|technology";

    private final NewsBriefService newsBriefService;
    private final BriefProperties properties;
    private final BriefMarkdownRenderer markdownRenderer;

    NewsBriefController(NewsBriefService newsBriefService, BriefProperties properties,
                        BriefMarkdownRenderer markdownRenderer) {
        this.newsBriefService = newsBriefService;
        this.properties = properties;
        this.markdownRenderer = markdownRenderer;
    }

    @GetMapping(value = "/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Today's brief as structured data")
    @ApiResponse(responseCode = "200", description = "A generated brief")
    @ApiResponse(responseCode = "400", description = "The country or category is not recognised")
    @ApiResponse(responseCode = "503", description = "NewsAPI or the summarization model is unavailable")
    DailyBrief dailyBrief(
            @Parameter(description = "ISO 3166-1 alpha-2 country code")
            @RequestParam(required = false) @Pattern(regexp = COUNTRY_PATTERN) String country,

            @Parameter(description = "NewsAPI topic filter")
            @RequestParam(required = false) @Pattern(regexp = CATEGORY_PATTERN) String category) {

        return newsBriefService.dailyBrief(queryFor(country, category));
    }

    @GetMapping(value = "/daily", produces = BriefMarkdownRenderer.TEXT_MARKDOWN)
    @Operation(summary = "Today's brief as markdown, ready to paste elsewhere")
    String dailyBriefAsMarkdown(
            @RequestParam(required = false) @Pattern(regexp = COUNTRY_PATTERN) String country,
            @RequestParam(required = false) @Pattern(regexp = CATEGORY_PATTERN) String category) {

        return markdownRenderer.render(newsBriefService.dailyBrief(queryFor(country, category)));
    }

    /** Applies the configured default so the query - and the cache key - is fully specified. */
    private HeadlinesQuery queryFor(String country, String category) {
        return new HeadlinesQuery(
                country == null || country.isBlank() ? properties.defaultCountry() : country,
                category);
    }
}
