package com.arthur.newsbrief.brief;

import com.arthur.newsbrief.brief.internal.BriefProperties;
import com.arthur.newsbrief.brief.internal.NewsBriefService;
import com.arthur.newsbrief.news.HeadlinesQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The daily brief, offered as one resource in two representations.
 *
 * <p>JSON and HTML are the same resource, chosen by content negotiation, rather than two
 * paths differing by a {@code /render} suffix. A browser asks for {@code text/html} and
 * gets a page; anything else gets JSON.
 *
 * <p>The class is deliberately not {@code @Validated}: Spring MVC validates constrained
 * handler parameters natively and reports failures as a 400 problem detail, whereas the
 * AOP-based alternative raises a {@code ConstraintViolationException} that surfaces as a 500.
 */
@Controller
@RequestMapping("/api/v1/briefs")
@Tag(name = "Briefs", description = "AI-generated summaries of current headlines")
class NewsBriefController {

    private static final String COUNTRY_PATTERN = "[a-zA-Z]{2}";
    private static final String CATEGORY_PATTERN =
            "business|entertainment|general|health|science|sports|technology";

    private final NewsBriefService newsBriefService;
    private final BriefProperties properties;

    NewsBriefController(NewsBriefService newsBriefService, BriefProperties properties) {
        this.newsBriefService = newsBriefService;
        this.properties = properties;
    }

    @GetMapping(value = "/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
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

    @GetMapping(value = "/daily", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Today's brief as a readable page")
    String dailyBriefPage(
            @RequestParam(required = false) @Pattern(regexp = COUNTRY_PATTERN) String country,
            @RequestParam(required = false) @Pattern(regexp = CATEGORY_PATTERN) String category,
            Model model) {

        model.addAttribute("brief", newsBriefService.dailyBrief(queryFor(country, category)));
        return "brief";
    }

    /** Applies the configured default so the query - and the cache key - is fully specified. */
    private HeadlinesQuery queryFor(String country, String category) {
        return new HeadlinesQuery(
                country == null || country.isBlank() ? properties.defaultCountry() : country,
                category);
    }
}
